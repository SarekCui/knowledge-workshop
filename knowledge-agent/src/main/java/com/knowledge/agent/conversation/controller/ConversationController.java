package com.knowledge.agent.conversation.controller;

import com.knowledge.agent.conversation.bo.ConversationBO;
import com.knowledge.agent.conversation.bo.HistoryMessageBO;
import com.knowledge.agent.conversation.bo.RunBO;
import com.knowledge.agent.conversation.bo.ConversationSummaryBO;
import com.knowledge.agent.conversation.dto.CreateConversationDTO;
import com.knowledge.agent.conversation.dto.SendMessageDTO;
import com.knowledge.agent.conversation.service.ConversationService;
import com.knowledge.agent.model.ChatPrompt;
import com.knowledge.agent.model.StreamingChatModelClient;
import com.knowledge.api.common.Result;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/agent/conversations")
public class ConversationController {
    @Resource private ConversationService conversationService;
    @Resource private StreamingChatModelClient modelService;
    @Resource private TaskExecutor agentRunExecutor;


    @PostMapping
    public Result<ConversationBO> create(@Valid @RequestBody CreateConversationDTO request,
            HttpServletRequest servletRequest) {
        return Result.ok(conversationService.create(UserContext.getUserId(), request), requestId(servletRequest));
    }

    @GetMapping
    public Result<List<ConversationSummaryBO>> list() {
        return Result.ok(conversationService.list(UserContext.getUserId()), null);
    }

    @GetMapping("/{conversationId}/messages")
    public Result<List<HistoryMessageBO>> messages(@PathVariable String conversationId) {
        return Result.ok(conversationService.messages(UserContext.getUserId(), conversationId), null);
    }

    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@PathVariable String conversationId) {
        conversationService.delete(UserContext.getUserId(), conversationId);
        return Result.ok(null, null);
    }

    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String conversationId, @Valid @RequestBody SendMessageDTO request) {
        if (!modelService.isAvailable()) {
            throw BusinessException.serviceUnavailable("小智模型尚未配置");
        }
        String userId = UserContext.getUserId();
        RunBO run = conversationService.start(userId, conversationId, request);
        if (run.replay()) throw BusinessException.conflict("相同请求已创建运行，请刷新会话记录");
        SseEmitter emitter = new SseEmitter(35_000L);
        agentRunExecutor.execute(() -> generate(emitter, modelService, run.runId(), userId, request));
        return emitter;
    }

    private void generate(SseEmitter emitter, StreamingChatModelClient model, String runId, String userId,
            SendMessageDTO request) {
        try {
            emitter.send(SseEmitter.event().name("run.started").data(Map.of("runId", runId)));
            String system = "你是知识工坊的学习助手小智。只回答问题本身；不知道时明确说明，不编造来源。";
            String prompt = request.question().trim() + (request.pageContext() == null || request.pageContext().isBlank()
                    ? "" : "\n页面上下文提示（仅供理解提问，不代表权限）：" + request.pageContext().trim());
            StringBuilder answer = new StringBuilder();
            AtomicBoolean completed = new AtomicBoolean();
            model.stream(new ChatPrompt(system, prompt, userId), delta -> {
                answer.append(delta);
                send(emitter, "answer.delta", Map.of("text", delta));
            }, response -> {
                if (completed.compareAndSet(false, true)) {
                    conversationService.complete(runId, answer.toString());
                    Map<String, Integer> completion = new LinkedHashMap<>();
                    completion.put("inputTokens", response.inputTokens());
                    completion.put("outputTokens", response.outputTokens());
                    send(emitter, "answer.completed", completion);
                    emitter.complete();
                }
            }, error -> fail(emitter, runId, error, completed));
        } catch (Exception exception) {
            fail(emitter, runId, exception, new AtomicBoolean());
        }
    }

    private void fail(SseEmitter emitter, String runId, Throwable error, AtomicBoolean completed) {
        if (!completed.compareAndSet(false, true)) return;
        conversationService.fail(runId, error.getMessage() == null ? "模型调用失败" : error.getMessage());
        try { send(emitter, "answer.failed", Map.of("code", "MODEL_UNAVAILABLE", "retryable", true)); emitter.complete(); }
        catch (Exception ignored) { emitter.completeWithError(error); }
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try { emitter.send(SseEmitter.event().name(event).data(data)); }
        catch (Exception exception) { throw new IllegalStateException("SSE 连接已断开", exception); }
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
