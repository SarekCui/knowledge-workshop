package com.knowledge.agent.mention.service;

import com.knowledge.agent.mention.dao.mapper.AgentEventInboxMapper;
import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import com.knowledge.agent.mention.dao.model.AgentEventInboxDO;
import com.knowledge.agent.mention.dao.model.AgentRunDO;
import com.knowledge.agent.mention.enums.AgentRunStatus;
import com.knowledge.api.learning.dto.AgentMentionedEventDTO;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentMentionRunService {

    private static final String CONSUMER = "agent-comment-mention-v1";
    private static final String COMMENT_REPLY_RUN = "COMMENT_REPLY";

    @Autowired
    private AgentEventInboxMapper inboxMapper;
    @Autowired
    private AgentRunMapper runMapper;
    @Autowired
    private Clock clock;

    /**
     * Records a pending, asynchronous comment-reply run. This transaction deliberately contains no model call.
     */
    @Transactional
    public void enqueue(AgentMentionedEventDTO event) {
        LocalDateTime now = LocalDateTime.now(clock);
        AgentEventInboxDO inbox = new AgentEventInboxDO();
        inbox.setId(UUID.randomUUID().toString());
        inbox.setEventId(event.eventId());
        inbox.setConsumer(CONSUMER);
        inbox.setCreatedAt(now);
        if (inboxMapper.insertIgnore(inbox) == 0) {
            return;
        }

        AgentRunDO run = new AgentRunDO();
        run.setId(UUID.randomUUID().toString());
        run.setRunType(COMMENT_REPLY_RUN);
        run.setStatus(AgentRunStatus.PENDING);
        run.setEventId(event.eventId());
        run.setSourceCommentId(event.commentId());
        run.setNoteId(event.noteId());
        run.setRequesterId(event.requesterId());
        run.setAttemptCount(0);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        runMapper.insertIgnore(run);
    }
}
