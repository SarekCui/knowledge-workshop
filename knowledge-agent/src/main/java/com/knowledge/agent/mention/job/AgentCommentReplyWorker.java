package com.knowledge.agent.mention.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import com.knowledge.agent.mention.dao.model.AgentRunDO;
import com.knowledge.agent.mention.enums.AgentRunStatus;
import com.knowledge.agent.model.AgentModelRequest;
import com.knowledge.agent.model.AgentModelResponse;
import com.knowledge.agent.model.AgentModelService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Picks up PENDING COMMENT_REPLY runs, asks the model for a grounded reply,
 * and publishes it back to the learning comment stream via the internal API.
 */
@Component
@ConditionalOnBean(AgentModelService.class)
public class AgentCommentReplyWorker {

    private static final Logger log = LoggerFactory.getLogger(AgentCommentReplyWorker.class);
    private static final String COMMENT_REPLY_RUN = "COMMENT_REPLY";

    @Autowired private AgentRunMapper runMapper;
    @Autowired private AgentServiceTokenSigner tokenSigner;
    @Autowired private AgentModelService modelService;
    @Autowired private Clock clock;
    @Autowired private ObjectMapper objectMapper;

    @Value("${knowledge.agent.learning-base-url:http://127.0.0.1:8084}")
    private String learningBaseUrl;

    @Scheduled(fixedDelayString = "${knowledge.agent.comment-reply-poll-delay:3000}")
    public void processPendingRuns() {
        List<AgentRunDO> pending = runMapper.selectList(Wrappers.<AgentRunDO>lambdaQuery()
                .eq(AgentRunDO::getRunType, COMMENT_REPLY_RUN)
                .eq(AgentRunDO::getStatus, AgentRunStatus.PENDING)
                .orderByAsc(AgentRunDO::getCreatedAt)
                .last("LIMIT 5"));
        for (AgentRunDO run : pending) {
            try {
                execute(run);
            } catch (Exception e) {
                log.error("comment reply run failed: {}", run.getId(), e);
                markFailed(run, e.getMessage());
            }
        }
    }

    private void execute(AgentRunDO run) {
        run.setStatus(AgentRunStatus.RUNNING);
        run.setAttemptCount(run.getAttemptCount() == null ? 1 : run.getAttemptCount() + 1);
        run.setUpdatedAt(LocalDateTime.now(clock));
        runMapper.updateById(run);

        String token = tokenSigner.signAgentToken();
        RestClient http = RestClient.builder().baseUrl(learningBaseUrl).build();

        // 1. fetch note + comment context
        JsonNode ctx = http.get()
                .uri(uri -> uri.path("/internal/agent/comments/context")
                        .queryParam("noteId", run.getNoteId())
                        .queryParam("commentId", run.getSourceCommentId()).build())
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(JsonNode.class);
        JsonNode data = ctx == null ? null : ctx.get("data");
        if (data == null) throw new IllegalStateException("empty context response");
        String noteTitle = data.path("noteTitle").asText("");
        String noteContent = data.path("noteContent").asText("");
        String commentContent = data.path("commentContent").asText("");

        // 2. ask model
        String system = "你是知识工坊学习助手小智，在公开笔记的评论区回答学习者的提问。"
                + "回答简洁、准确、友好，紧扣笔记内容。不要暴露内部术语或工具调用。";
        String user = "笔记标题：" + noteTitle + "\n笔记正文：" + noteContent
                + "\n\n学习者评论：" + commentContent
                + "\n\n请直接给出对这条评论的回复（150 字以内）。";
        AgentModelResponse answer = modelService.generate(new AgentModelRequest(system, user, run.getRequesterId()));

        // 3. publish reply
        String clientRequestId = UUID.randomUUID().toString();
        http.post()
                .uri("/internal/agent/comments/ai-replies")
                .headers(h -> h.setBearerAuth(token))
                .body(java.util.Map.of(
                        "clientRequestId", clientRequestId,
                        "noteId", run.getNoteId(),
                        "sourceCommentId", run.getSourceCommentId(),
                        "content", answer.text()))
                .retrieve()
                .toBodilessEntity();

        run.setStatus(AgentRunStatus.SUCCEEDED);
        run.setUpdatedAt(LocalDateTime.now(clock));
        runMapper.updateById(run);
        log.info("comment reply published for run={}", run.getId());
    }

    private void markFailed(AgentRunDO run, String error) {
        run.setStatus(AgentRunStatus.FAILED);
        run.setLastError(error == null ? "unknown" : error.substring(0, Math.min(500, error.length())));
        run.setUpdatedAt(LocalDateTime.now(clock));
        runMapper.updateById(run);
    }
}
