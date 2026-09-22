package com.knowledge.agent.conversation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.agent.conversation.bo.ConversationBO;
import com.knowledge.agent.conversation.bo.HistoryMessageBO;
import com.knowledge.agent.conversation.bo.RunBO;
import com.knowledge.agent.conversation.bo.ConversationSummaryBO;
import com.knowledge.agent.conversation.dao.mapper.AgentConversationMapper;
import com.knowledge.agent.conversation.dao.mapper.AgentMessageMapper;
import com.knowledge.agent.conversation.dao.model.AgentConversationDO;
import com.knowledge.agent.conversation.dao.model.AgentMessageDO;
import com.knowledge.agent.conversation.dto.CreateConversationDTO;
import com.knowledge.agent.conversation.dto.SendMessageDTO;
import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import com.knowledge.agent.mention.dao.model.AgentRunDO;
import com.knowledge.agent.mention.enums.RunStatus;
import com.knowledge.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {
    @Resource private AgentConversationMapper conversationMapper;
    @Resource private AgentMessageMapper messageMapper;
    @Resource private AgentRunMapper runMapper;
    @Resource private Clock clock;

    @Transactional
    public ConversationBO create(String userId, CreateConversationDTO request) {
        LocalDateTime now = LocalDateTime.now(clock);
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setTitle(request.title().trim());
        conversation.setVersion(0);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);
        return new ConversationBO(conversation.getId(), conversation.getTitle(), 0, now);
    }

    @Transactional
    public RunBO start(String userId, String conversationId, SendMessageDTO request) {
        AgentConversationDO conversation = conversationMapper.selectForUpdate(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) throw BusinessException.notFound("会话不存在");
        AgentMessageDO existing = messageMapper.findByConversationAndIdempotencyKey(conversationId, request.idempotencyKey());
        if (existing != null) {
            AgentRunDO existingRun = runMapper.selectOne(Wrappers.<AgentRunDO>lambdaQuery()
                    .eq(AgentRunDO::getConversationId, conversationId)
                    .eq(AgentRunDO::getIdempotencyKey, request.idempotencyKey()));
            if (existingRun != null) return new RunBO(existingRun.getId(), true);
        }
        long active = runMapper.selectCount(Wrappers.<AgentRunDO>lambdaQuery()
                .eq(AgentRunDO::getConversationId, conversationId)
                .eq(AgentRunDO::getStatus, RunStatus.RUNNING));
        if (active > 0) throw BusinessException.conflict("该会话已有进行中的小智回答");
        LocalDateTime now = LocalDateTime.now(clock);
        String runId = UUID.randomUUID().toString();
        AgentMessageDO message = new AgentMessageDO();
        message.setId(UUID.randomUUID().toString()); message.setConversationId(conversationId); message.setRunId(runId);
        message.setRole("USER"); message.setIdempotencyKey(request.idempotencyKey()); message.setContent(request.question().trim()); message.setCreatedAt(now);
        messageMapper.insert(message);
        AgentRunDO run = new AgentRunDO();
        run.setId(runId); run.setRunType("CHAT"); run.setStatus(RunStatus.RUNNING); run.setConversationId(conversationId);
        run.setIdempotencyKey(request.idempotencyKey()); run.setUserId(userId); run.setAttemptCount(1); run.setCreatedAt(now); run.setUpdatedAt(now);
        runMapper.insert(run);
        return new RunBO(runId, false);
    }

    @Transactional
    public void complete(String runId, String answer) {
        AgentRunDO run = runMapper.selectById(runId);
        if (run == null || run.getStatus() != RunStatus.RUNNING) return;
        LocalDateTime now = LocalDateTime.now(clock);
        AgentMessageDO message = new AgentMessageDO();
        message.setId(UUID.randomUUID().toString()); message.setConversationId(run.getConversationId()); message.setRunId(runId);
        message.setRole("ASSISTANT"); message.setContent(answer); message.setCreatedAt(now); messageMapper.insert(message);
        run.setStatus(RunStatus.SUCCEEDED); run.setUpdatedAt(now); runMapper.updateById(run);
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryBO> list(String userId) {
        return conversationMapper.selectList(Wrappers.<AgentConversationDO>lambdaQuery()
                .eq(AgentConversationDO::getUserId, userId)
                .orderByDesc(AgentConversationDO::getUpdatedAt)
                .last("LIMIT 50")).stream()
                .map(conversation -> new ConversationSummaryBO(conversation.getId(), conversation.getTitle(), conversation.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryMessageBO> messages(String userId, String conversationId) {
        AgentConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) throw BusinessException.notFound("会话不存在");
        return messageMapper.selectList(Wrappers.<AgentMessageDO>lambdaQuery()
                .eq(AgentMessageDO::getConversationId, conversationId)
                .orderByAsc(AgentMessageDO::getCreatedAt)
                .orderByAsc(AgentMessageDO::getId)).stream()
                .map(message -> new HistoryMessageBO(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void delete(String userId, String conversationId) {
        AgentConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) throw BusinessException.notFound("会话不存在");
        messageMapper.delete(Wrappers.<AgentMessageDO>lambdaQuery().eq(AgentMessageDO::getConversationId, conversationId));
        runMapper.delete(Wrappers.<AgentRunDO>lambdaQuery().eq(AgentRunDO::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }

    @Transactional
    public void fail(String runId, String error) {
        AgentRunDO run = runMapper.selectById(runId);
        if (run == null || run.getStatus() != RunStatus.RUNNING) return;
        run.setStatus(RunStatus.FAILED); run.setLastError(error.substring(0, Math.min(500, error.length())));
        run.setUpdatedAt(LocalDateTime.now(clock)); runMapper.updateById(run);
    }
}
