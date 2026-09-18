package com.knowledge.agent.chat.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.agent.chat.bo.AgentConversationBO;
import com.knowledge.agent.chat.bo.AgentHistoryMessageBO;
import com.knowledge.agent.chat.bo.AgentRunBO;
import com.knowledge.agent.chat.bo.ConversationSummaryBO;
import com.knowledge.agent.chat.dao.mapper.AgentConversationMapper;
import com.knowledge.agent.chat.dao.mapper.AgentMessageMapper;
import com.knowledge.agent.chat.dao.model.AgentConversationDO;
import com.knowledge.agent.chat.dao.model.AgentMessageDO;
import com.knowledge.agent.chat.dto.CreateConversationDTO;
import com.knowledge.agent.chat.dto.SendAgentMessageDTO;
import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import com.knowledge.agent.mention.dao.model.AgentRunDO;
import com.knowledge.agent.mention.enums.AgentRunStatus;
import com.knowledge.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentConversationService {
    @Autowired private AgentConversationMapper conversationMapper;
    @Autowired private AgentMessageMapper messageMapper;
    @Autowired private AgentRunMapper runMapper;
    @Autowired private Clock clock;

    @Transactional
    public AgentConversationBO create(String userId, CreateConversationDTO request) {
        LocalDateTime now = LocalDateTime.now(clock);
        AgentConversationDO conversation = new AgentConversationDO();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setTitle(request.title().trim());
        conversation.setVersion(0);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);
        return new AgentConversationBO(conversation.getId(), conversation.getTitle(), 0, now);
    }

    @Transactional
    public AgentRunBO start(String userId, String conversationId, SendAgentMessageDTO request) {
        AgentConversationDO conversation = conversationMapper.selectForUpdate(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) throw BusinessException.notFound("会话不存在");
        AgentMessageDO existing = messageMapper.findByRequest(conversationId, request.clientRequestId());
        if (existing != null) {
            AgentRunDO existingRun = runMapper.selectOne(Wrappers.<AgentRunDO>lambdaQuery()
                    .eq(AgentRunDO::getConversationId, conversationId)
                    .eq(AgentRunDO::getClientRequestId, request.clientRequestId()));
            if (existingRun != null) return new AgentRunBO(existingRun.getId(), true);
        }
        long active = runMapper.selectCount(Wrappers.<AgentRunDO>lambdaQuery()
                .eq(AgentRunDO::getConversationId, conversationId)
                .eq(AgentRunDO::getStatus, AgentRunStatus.RUNNING));
        if (active > 0) throw BusinessException.conflict("该会话已有进行中的小智回答");
        LocalDateTime now = LocalDateTime.now(clock);
        String runId = UUID.randomUUID().toString();
        AgentMessageDO message = new AgentMessageDO();
        message.setId(UUID.randomUUID().toString()); message.setConversationId(conversationId); message.setRunId(runId);
        message.setRole("USER"); message.setClientRequestId(request.clientRequestId()); message.setContent(request.question().trim()); message.setCreatedAt(now);
        messageMapper.insert(message);
        AgentRunDO run = new AgentRunDO();
        run.setId(runId); run.setRunType("CHAT"); run.setStatus(AgentRunStatus.RUNNING); run.setConversationId(conversationId);
        run.setClientRequestId(request.clientRequestId()); run.setRequesterId(userId); run.setAttemptCount(1); run.setCreatedAt(now); run.setUpdatedAt(now);
        runMapper.insert(run);
        return new AgentRunBO(runId, false);
    }

    @Transactional
    public void complete(String runId, String answer) {
        AgentRunDO run = runMapper.selectById(runId);
        if (run == null || run.getStatus() != AgentRunStatus.RUNNING) return;
        LocalDateTime now = LocalDateTime.now(clock);
        AgentMessageDO message = new AgentMessageDO();
        message.setId(UUID.randomUUID().toString()); message.setConversationId(run.getConversationId()); message.setRunId(runId);
        message.setRole("ASSISTANT"); message.setContent(answer); message.setCreatedAt(now); messageMapper.insert(message);
        run.setStatus(AgentRunStatus.SUCCEEDED); run.setUpdatedAt(now); runMapper.updateById(run);
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
    public List<AgentHistoryMessageBO> messages(String userId, String conversationId) {
        AgentConversationDO conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) throw BusinessException.notFound("会话不存在");
        return messageMapper.selectList(Wrappers.<AgentMessageDO>lambdaQuery()
                .eq(AgentMessageDO::getConversationId, conversationId)
                .orderByAsc(AgentMessageDO::getCreatedAt)
                .orderByAsc(AgentMessageDO::getId)).stream()
                .map(message -> new AgentHistoryMessageBO(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt()))
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
        if (run == null || run.getStatus() != AgentRunStatus.RUNNING) return;
        run.setStatus(AgentRunStatus.FAILED); run.setLastError(error.substring(0, Math.min(500, error.length())));
        run.setUpdatedAt(LocalDateTime.now(clock)); runMapper.updateById(run);
    }
}
