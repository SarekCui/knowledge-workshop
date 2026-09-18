package com.knowledge.learning.note.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.bo.NoteCommentBO;
import com.knowledge.learning.note.converter.NoteEngagementConverter;
import com.knowledge.learning.note.dao.mapper.NoteCommentLikeMapper;
import com.knowledge.learning.note.dao.mapper.NoteCommentMapper;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteCommentDO;
import com.knowledge.learning.note.dao.model.NoteCommentLikeDO;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.enums.NoteStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteCommentLikeService {

    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private NoteCommentMapper commentMapper;
    @Autowired
    private NoteCommentLikeMapper commentLikeMapper;
    @Autowired
    private Clock clock;

    @Transactional
    public NoteCommentBO like(String userId, String commentId) {
        NoteCommentDO comment = requirePublicCommentForUpdate(commentId);
        if (commentLikeMapper.countByUser(commentId, userId) == 0) {
            NoteCommentLikeDO like = new NoteCommentLikeDO();
            like.setId(UUID.randomUUID().toString());
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now(clock));
            commentLikeMapper.insert(like);
            commentMapper.adjustLikeCount(commentId, 1);
            comment.setLikeCount(count(comment.getLikeCount()) + 1);
        }
        return NoteEngagementConverter.toBO(comment, userId, true);
    }

    @Transactional
    public NoteCommentBO unlike(String userId, String commentId) {
        NoteCommentDO comment = requirePublicCommentForUpdate(commentId);
        if (commentLikeMapper.deleteByUser(commentId, userId) == 1) {
            commentMapper.adjustLikeCount(commentId, -1);
            comment.setLikeCount(Math.max(0, count(comment.getLikeCount()) - 1));
        }
        return NoteEngagementConverter.toBO(comment, userId, false);
    }

    private NoteCommentDO requirePublicCommentForUpdate(String commentId) {
        NoteCommentDO snapshot = commentMapper.selectById(commentId);
        if (snapshot == null || snapshot.getDeleted() != 0) {
            throw BusinessException.notFound("评论不存在");
        }
        NoteDO note = noteMapper.selectActiveForUpdate(snapshot.getNoteId());
        if (note == null || note.getStatus() != NoteStatus.PUBLIC) {
            throw BusinessException.notFound("公开 Note 不存在");
        }
        NoteCommentDO comment = commentMapper.selectForUpdate(commentId);
        if (comment == null || comment.getDeleted() != 0) {
            throw BusinessException.notFound("评论不存在");
        }
        return comment;
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
