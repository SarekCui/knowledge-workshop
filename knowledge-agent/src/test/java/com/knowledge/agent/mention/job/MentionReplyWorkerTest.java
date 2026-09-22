package com.knowledge.agent.mention.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import com.knowledge.agent.mention.dao.model.AgentRunDO;
import com.knowledge.agent.mention.enums.RunStage;
import com.knowledge.agent.mention.service.LearningCommentService;
import com.knowledge.agent.mention.service.RetryService;
import com.knowledge.agent.model.ChatModelClient;
import com.knowledge.agent.model.GenerationResult;
import com.knowledge.api.learning.dto.AgentCommentContextDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.scheduling.TaskScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import feign.FeignException;

class MentionReplyWorkerTest {

    @Test
    void generatedAnswerIsPersistedBeforePublishAndRetryDoesNotRegenerateIt() {
        AgentRunMapper mapper = mock(AgentRunMapper.class);
        LearningCommentService client = mock(LearningCommentService.class);
        ChatModelClient model = mock(ChatModelClient.class);
        RetryService retryService = mock(RetryService.class);
        AgentRunDO run = run(RunStage.CONTEXT, null);
        when(mapper.claim(eq(run.getId()), anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(mapper.selectById(run.getId())).thenReturn(run);
        when(client.fetchContext(run.getNoteId(), run.getSourceCommentId()))
                .thenReturn(new AgentCommentContextDTO("标题", "正文", "问题"));
        when(mapper.startGeneration(eq(run.getId()), eq(7L), any(LocalDateTime.class))).thenReturn(1);
        when(model.generate(any())).thenReturn(new GenerationResult("模型回答", 10, 20));
        org.mockito.Mockito.doAnswer(invocation -> {
            run.setRunStage(RunStage.PUBLISH);
            return 1;
        }).when(mapper).persistGeneratedAnswer(eq(run.getId()), eq(7L), eq("模型回答"), any(LocalDateTime.class));
        org.mockito.Mockito.doThrow(new ResourceAccessException("learning unavailable"))
                .doNothing().when(client).publishReply(run.getNoteId(), run.getSourceCommentId(), "模型回答");
        when(mapper.markPublished(eq(run.getId()), eq(7L), any(LocalDateTime.class))).thenReturn(1);

        MentionReplyWorker worker = worker(mapper, client, model, retryService);
        worker.execute(run.getId());
        worker.execute(run.getId());

        verify(model, times(1)).generate(any());
        verify(mapper, times(1)).persistGeneratedAnswer(eq(run.getId()), eq(7L), eq("模型回答"),
                any(LocalDateTime.class));
        verify(client, times(2)).publishReply(run.getNoteId(), run.getSourceCommentId(), "模型回答");
        verify(retryService, times(1)).schedule(eq(run.getId()), eq(7L), eq("COMMENT_REPLY"), eq(2),
                eq("DEPENDENCY_FAILURE"), anyString());
        verify(mapper).markPublished(eq(run.getId()), eq(7L), any(LocalDateTime.class));
    }

    @Test
    void publishFailureReschedulesPersistedAnswerWithoutCallingModelAgain() {
        AgentRunMapper mapper = mock(AgentRunMapper.class);
        LearningCommentService client = mock(LearningCommentService.class);
        ChatModelClient model = mock(ChatModelClient.class);
        RetryService retryService = mock(RetryService.class);
        AgentRunDO run = run(RunStage.PUBLISH, "已持久化的回答");
        when(mapper.claim(eq(run.getId()), anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(mapper.selectById(run.getId())).thenReturn(run);
        org.mockito.Mockito.doThrow(new ResourceAccessException("learning unavailable"))
                .when(client).publishReply(run.getNoteId(), run.getSourceCommentId(), run.getAnswer());

        worker(mapper, client, model, retryService).execute(run.getId());

        verify(model, never()).generate(any());
        verify(retryService).schedule(eq(run.getId()), eq(7L), eq("COMMENT_REPLY"), eq(2),
                eq("DEPENDENCY_FAILURE"), anyString());
        verify(mapper, never()).persistGeneratedAnswer(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(),
                any(LocalDateTime.class));
    }

    @Test
    void missingPublicContextMarksRunDeadWithoutRetryingOrCallingModel() {
        AgentRunMapper mapper = mock(AgentRunMapper.class);
        LearningCommentService client = mock(LearningCommentService.class);
        ChatModelClient model = mock(ChatModelClient.class);
        RetryService retryService = mock(RetryService.class);
        AgentRunDO run = run(RunStage.CONTEXT, null);
        when(mapper.claim(eq(run.getId()), anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(mapper.selectById(run.getId())).thenReturn(run);
        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        org.mockito.Mockito.doThrow(notFound).when(client)
                .fetchContext(run.getNoteId(), run.getSourceCommentId());

        worker(mapper, client, model, retryService).execute(run.getId());

        verify(model, never()).generate(any());
        verify(mapper).markDead(eq(run.getId()), eq(7L), eq("CONTEXT_NOT_AVAILABLE"), anyString(),
                any(LocalDateTime.class));
        verify(retryService, never()).schedule(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), anyString(), anyString());
    }

    @Test
    void lostLeaseAfterModelReturnsDoesNotPersistOrPublishTheAnswer() {
        AgentRunMapper mapper = mock(AgentRunMapper.class);
        LearningCommentService client = mock(LearningCommentService.class);
        ChatModelClient model = mock(ChatModelClient.class);
        RetryService retryService = mock(RetryService.class);
        AgentRunDO run = run(RunStage.CONTEXT, null);
        when(mapper.claim(eq(run.getId()), anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(mapper.selectById(run.getId())).thenReturn(run);
        when(client.fetchContext(run.getNoteId(), run.getSourceCommentId()))
                .thenReturn(new AgentCommentContextDTO("标题", "正文", "问题"));
        when(mapper.startGeneration(eq(run.getId()), eq(7L), any(LocalDateTime.class))).thenReturn(1);
        when(mapper.renewLease(eq(run.getId()), eq(7L), anyString(), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(0);

        MentionReplyWorker worker = worker(mapper, client, model, retryService);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        AtomicReference<Runnable> renewalTask = new AtomicReference<>();
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), any(java.time.Duration.class))).thenAnswer(invocation -> {
            renewalTask.set(invocation.getArgument(0));
            return mock(ScheduledFuture.class);
        });
        ReflectionTestUtils.setField(worker, "agentLeaseScheduler", scheduler);
        when(model.generate(any())).thenAnswer(invocation -> {
            renewalTask.get().run();
            return new GenerationResult("不应写入", 10, 20);
        });

        worker.execute(run.getId());

        verify(mapper, never()).persistGeneratedAnswer(anyString(), org.mockito.ArgumentMatchers.anyLong(),
                anyString(), any(LocalDateTime.class));
        verify(client, never()).publishReply(anyString(), anyString(), anyString());
        verify(retryService, never()).schedule(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), anyString(), anyString());
    }

    private MentionReplyWorker worker(AgentRunMapper mapper, LearningCommentService client,
            ChatModelClient model, RetryService retryService) {
        MentionReplyWorker worker = new MentionReplyWorker();
        ReflectionTestUtils.setField(worker, "runMapper", mapper);
        ReflectionTestUtils.setField(worker, "learningCommentService", client);
        ReflectionTestUtils.setField(worker, "retryService", retryService);
        ReflectionTestUtils.setField(worker, "modelService", model);
        ReflectionTestUtils.setField(worker, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(worker, "clock", Clock.fixed(Instant.parse("2026-09-18T00:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(worker, "agentLeaseScheduler", mock(TaskScheduler.class));
        ReflectionTestUtils.setField(worker, "instanceId", "test-worker");
        ReflectionTestUtils.setField(worker, "leaseSeconds", 90L);
        ReflectionTestUtils.setField(worker, "leaseRenewIntervalSeconds", 20L);
        return worker;
    }

    private AgentRunDO run(RunStage stage, String answer) {
        AgentRunDO run = new AgentRunDO();
        run.setId("run-1");
        run.setRunType("COMMENT_REPLY");
        run.setNoteId("note-1");
        run.setSourceCommentId("comment-1");
        run.setUserId("user-1");
        run.setAttemptCount(2);
        run.setExecutionVersion(7L);
        run.setRunStage(stage);
        run.setAnswer(answer);
        return run;
    }
}
