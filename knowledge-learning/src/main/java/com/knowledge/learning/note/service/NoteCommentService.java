package com.knowledge.learning.note.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.model.PageBO;
import com.knowledge.learning.note.bo.NoteCommentBO;
import com.knowledge.learning.note.converter.NoteEngagementConverter;
import com.knowledge.learning.note.dao.mapper.NoteCommentMapper;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteCommentDO;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.dto.CreateNoteCommentDTO;
import com.knowledge.learning.note.enums.NoteStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteCommentService {

    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private NoteCommentMapper commentMapper;
    @Autowired
    private Clock clock;

    public PageBO<NoteCommentBO> page(String userId, String noteId, int pageNo, int pageSize) {
        requirePublic(noteMapper.selectById(noteId));
        validatePage(pageNo, pageSize);
        var result = commentMapper.selectPage(new Page<NoteCommentDO>(pageNo, pageSize),
                Wrappers.<NoteCommentDO>lambdaQuery()
                        .eq(NoteCommentDO::getNoteId, noteId)
                        .eq(NoteCommentDO::getDeleted, 0)
                        .orderByAsc(NoteCommentDO::getCreatedAt, NoteCommentDO::getId));
        return new PageBO<>(result.getRecords().stream()
                .map(comment -> NoteEngagementConverter.toBO(comment, userId)).toList(),
                pageNo, pageSize, result.getTotal());
    }

    @Transactional
    public NoteCommentBO create(String userId, String noteId, CreateNoteCommentDTO request) {
        requirePublic(noteMapper.selectActiveForUpdate(noteId));
        String clientRequestId = request.clientRequestId().trim();
        String parentId = normalizeNullable(request.parentCommentId());
        String content = request.content().trim();
        NoteCommentDO existing = commentMapper.findByRequest(userId, clientRequestId);
        if (existing != null) {
            if (existing.getDeleted() == 0 && existing.getNoteId().equals(noteId)
                    && Objects.equals(existing.getParentCommentId(), parentId)
                    && existing.getContent().equals(content)) {
                return NoteEngagementConverter.toBO(existing, userId);
            }
            throw BusinessException.conflict("相同 clientRequestId 已用于其他评论内容");
        }
        validateParent(noteId, parentId);
        NoteCommentDO comment = new NoteCommentDO();
        comment.setId(UUID.randomUUID().toString());
        comment.setNoteId(noteId);
        comment.setUserId(userId);
        comment.setParentCommentId(parentId);
        comment.setClientRequestId(clientRequestId);
        comment.setContent(content);
        comment.setVersion(0);
        comment.setDeleted(0);
        LocalDateTime now = LocalDateTime.now(clock);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentMapper.insert(comment);
        noteMapper.adjustCommentCount(noteId, 1);
        return NoteEngagementConverter.toBO(comment, userId);
    }

    @Transactional
    public void delete(String userId, String commentId, int version) {
        NoteCommentDO snapshot = commentMapper.selectById(commentId);
        if (snapshot == null || snapshot.getDeleted() != 0) {
            throw BusinessException.notFound("评论不存在");
        }
        NoteDO note = noteMapper.selectActiveForUpdate(snapshot.getNoteId());
        if (note == null) {
            throw BusinessException.notFound("评论所属 Note 不存在");
        }
        NoteCommentDO comment = commentMapper.selectForUpdate(commentId);
        if (comment == null || comment.getDeleted() != 0) {
            throw BusinessException.notFound("评论不存在");
        }
        if (!userId.equals(comment.getUserId())) {
            throw BusinessException.forbidden("无权删除该评论");
        }
        if (commentMapper.countActiveReplies(commentId) > 0) {
            throw BusinessException.conflict("该评论已有回复，暂不能删除");
        }
        if (commentMapper.softDelete(commentId, userId, version, LocalDateTime.now(clock)) != 1) {
            throw BusinessException.conflict("评论已被修改，请刷新后重试");
        }
        noteMapper.adjustCommentCount(note.getId(), -1);
    }

    private void validateParent(String noteId, String parentId) {
        if (parentId == null) return;
        NoteCommentDO parent = commentMapper.selectById(parentId);
        if (parent == null || parent.getDeleted() != 0 || !noteId.equals(parent.getNoteId())) {
            throw BusinessException.badRequest("回复的评论不存在或不属于当前 Note");
        }
        if (parent.getParentCommentId() != null) {
            throw BusinessException.badRequest("当前仅支持一级评论回复");
        }
    }

    private NoteDO requirePublic(NoteDO note) {
        if (note == null || note.getDeleted() != 0 || note.getStatus() != NoteStatus.PUBLIC) {
            throw BusinessException.notFound("公开 Note 不存在");
        }
        return note;
    }

    private void validatePage(int pageNo, int pageSize) {
        if (pageNo < 1 || pageNo > 1000 || pageSize < 1 || pageSize > 50) {
            throw BusinessException.badRequest("页码须为1—1000，每页条数须为1—50");
        }
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
