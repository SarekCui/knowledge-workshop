package com.knowledge.agent.mention.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.mention.config.MessagingConfiguration;
import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import com.knowledge.agent.mention.dao.model.AgentRunDO;
import com.knowledge.agent.mention.enums.RunStage;
import com.knowledge.agent.mention.mq.ExecuteRunCommand;
import com.knowledge.agent.mention.service.LearningCommentService;
import com.knowledge.agent.mention.service.RetryService;
import com.knowledge.agent.model.ChatPrompt;
import com.knowledge.agent.model.GenerationResult;
import com.knowledge.agent.model.ChatModelClient;
import com.knowledge.api.learning.dto.AgentCommentContextDTO;
import com.rabbitmq.client.Channel;
import feign.FeignException;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.TaskScheduler;

/**
 * Executes a durable COMMENT_REPLY command. RabbitMQ distributes notifications; the conditional
 * MySQL update remains authoritative for ownership and every later update carries its fencing token.
 */
@Component
@ConditionalOnProperty(name = "knowledge.agent.model.enabled", havingValue = "true")
public class MentionReplyWorker {

    private static final Logger log = LoggerFactory.getLogger(MentionReplyWorker.class);
    private static final String COMMENT_REPLY_RUN = "COMMENT_REPLY";

    @Resource private AgentRunMapper runMapper;
    @Resource private LearningCommentService learningCommentService;
    @Resource private RetryService retryService;
    @Resource private ChatModelClient modelService;
    @Resource private Clock clock;
    @Resource private ObjectMapper objectMapper;
    @Resource private TaskScheduler agentLeaseScheduler;

    @Value("${knowledge.agent.execution.instance-id:${HOSTNAME:local-agent}}")
    private String instanceId;
    @Value("${knowledge.agent.execution.lease-seconds:90}")
    private long leaseSeconds;
    @Value("${knowledge.agent.execution.lease-renew-interval-seconds:20}")
    private long leaseRenewIntervalSeconds;

    @RabbitListener(queues = MessagingConfiguration.AGENT_RUN_EXECUTION_QUEUE,
            containerFactory = "agentExecutionRabbitListenerContainerFactory")
    public void consume(String payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            ExecuteRunCommand command = objectMapper.readValue(payload, ExecuteRunCommand.class);
            if (command.commandId() == null || command.createdAt() == null || command.runId() == null
                    || !COMMENT_REPLY_RUN.equals(command.runType())) {
                channel.basicReject(deliveryTag, false);
                return;
            }
            execute(command.runId());
            channel.basicAck(deliveryTag, false);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("agent execution command failed before acknowledgement", exception);
            channel.basicReject(deliveryTag, false);
        }
    }

    void execute(String runId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (runMapper.claim(runId, instanceId, now.plusSeconds(leaseSeconds), now) != 1) {
            return;
        }
        AgentRunDO run = runMapper.selectById(runId);
        if (run == null || run.getExecutionVersion() == null) {
            return;
        }
        long executionVersion = run.getExecutionVersion();
        AtomicBoolean leaseLost = new AtomicBoolean(false);
        ScheduledFuture<?> leaseRenewal = startLeaseRenewal(runId, executionVersion, leaseLost);
        try {
            if (run.getRunStage() != RunStage.PUBLISH || run.getAnswer() == null) {
                var context = learningCommentService.fetchContext(run.getNoteId(), run.getSourceCommentId());
                if (leaseLost.get()) {
                    return;
                }
                if (run.getRunStage() == RunStage.CONTEXT
                        && runMapper.startGeneration(runId, executionVersion, LocalDateTime.now(clock)) != 1) {
                    return;
                }
                GenerationResult answer = generate(run, context);
                if (answer.text() == null || answer.text().isBlank()) {
                    throw new IllegalStateException("模型返回空回答");
                }
                // A lost lease means another worker may recover this run. Never persist a result
                // from an owner that can no longer prove it still holds the fencing token.
                if (leaseLost.get()) {
                    return;
                }
                if (runMapper.persistGeneratedAnswer(runId, executionVersion, answer.text().trim(),
                        LocalDateTime.now(clock)) != 1) {
                    return;
                }
                run.setAnswer(answer.text().trim());
            }
            if (leaseLost.get()) {
                return;
            }
            learningCommentService.publishReply(run.getNoteId(), run.getSourceCommentId(), run.getAnswer());
            if (leaseLost.get()) {
                return;
            }
            if (runMapper.markPublished(runId, executionVersion, LocalDateTime.now(clock)) == 1) {
                log.info("comment reply published for run={}, executionVersion={}", runId, executionVersion);
            }
        } catch (Exception exception) {
            if (leaseLost.get()) {
                log.warn("agent lease was lost; leaving run for recovery: run={}, executionVersion={}",
                        runId, executionVersion, exception);
                return;
            }
            if (isPermanent(exception)) {
                runMapper.markDead(runId, executionVersion, errorCode(exception), errorSummary(exception),
                        LocalDateTime.now(clock));
            } else {
                int attempt = run.getAttemptCount() == null ? 1 : run.getAttemptCount();
                retryService.schedule(runId, executionVersion, run.getRunType(), attempt, errorCode(exception),
                        errorSummary(exception));
            }
            log.warn("comment reply run did not complete: run={}, executionVersion={}", runId, executionVersion,
                    exception);
        } finally {
            if (leaseRenewal != null) {
                leaseRenewal.cancel(false);
            }
        }
    }

    private ScheduledFuture<?> startLeaseRenewal(String runId, long executionVersion, AtomicBoolean leaseLost) {
        long intervalSeconds = Math.max(1, Math.min(leaseRenewIntervalSeconds, Math.max(1, leaseSeconds / 2)));
        return agentLeaseScheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            try {
                if (runMapper.renewLease(runId, executionVersion, instanceId, now.plusSeconds(leaseSeconds), now) != 1) {
                    leaseLost.set(true);
                }
            } catch (RuntimeException exception) {
                leaseLost.set(true);
                log.warn("agent lease renewal failed: run={}, executionVersion={}", runId, executionVersion, exception);
            }
        }, Duration.ofSeconds(intervalSeconds));
    }

    private GenerationResult generate(AgentRunDO run, AgentCommentContextDTO context) {
        if (context == null) {
            throw new IllegalStateException("empty comment context response");
        }
        String system = "你是知识工坊学习助手小智，在公开笔记的评论区回答学习者的提问。"
                + "回答简洁、准确、友好，紧扣笔记内容。不要暴露内部术语或工具调用。";
        String user = "笔记标题：" + context.noteTitle() + "\n笔记正文：" + context.noteContent()
                + "\n\n学习者评论：" + context.commentContent()
                + "\n\n请直接给出对这条评论的回复（150 字以内）。";
        return modelService.generate(new ChatPrompt(system, user, run.getUserId()));
    }

    private boolean isPermanent(Exception exception) {
        return exception instanceof FeignException response && response.status() >= 400 && response.status() < 500;
    }

    private String errorCode(Exception exception) {
        return isPermanent(exception) ? "CONTEXT_NOT_AVAILABLE" : "DEPENDENCY_FAILURE";
    }

    private String errorSummary(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 500));
    }
}
