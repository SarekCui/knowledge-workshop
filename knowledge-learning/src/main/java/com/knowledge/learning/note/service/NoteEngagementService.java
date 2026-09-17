package com.knowledge.learning.note.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.bo.NoteEngagementBO;
import com.knowledge.learning.note.dao.mapper.NoteFavoriteMapper;
import com.knowledge.learning.note.dao.mapper.NoteLikeMapper;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.dao.model.NoteFavoriteDO;
import com.knowledge.learning.note.dao.model.NoteLikeDO;
import com.knowledge.learning.note.enums.NoteStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteEngagementService {

    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private NoteLikeMapper likeMapper;
    @Autowired
    private NoteFavoriteMapper favoriteMapper;
    @Autowired
    private Clock clock;

    public NoteEngagementBO get(String userId, String noteId) {
        NoteDO note = requirePublic(noteMapper.selectById(noteId));
        return toBO(userId, note);
    }

    @Transactional
    public NoteEngagementBO like(String userId, String noteId) {
        requirePublic(noteMapper.selectActiveForUpdate(noteId));
        if (likeMapper.countByUser(noteId, userId) == 0) {
            NoteLikeDO like = new NoteLikeDO();
            like.setId(UUID.randomUUID().toString());
            like.setNoteId(noteId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now(clock));
            likeMapper.insert(like);
            noteMapper.adjustLikeCount(noteId, 1);
        }
        return get(userId, noteId);
    }

    @Transactional
    public NoteEngagementBO unlike(String userId, String noteId) {
        requirePublic(noteMapper.selectActiveForUpdate(noteId));
        if (likeMapper.deleteByUser(noteId, userId) == 1) {
            noteMapper.adjustLikeCount(noteId, -1);
        }
        return get(userId, noteId);
    }

    @Transactional
    public NoteEngagementBO favorite(String userId, String noteId) {
        requirePublic(noteMapper.selectActiveForUpdate(noteId));
        if (favoriteMapper.countByUser(noteId, userId) == 0) {
            NoteFavoriteDO favorite = new NoteFavoriteDO();
            favorite.setId(UUID.randomUUID().toString());
            favorite.setNoteId(noteId);
            favorite.setUserId(userId);
            favorite.setCreatedAt(LocalDateTime.now(clock));
            favoriteMapper.insert(favorite);
            noteMapper.adjustFavoriteCount(noteId, 1);
        }
        return get(userId, noteId);
    }

    @Transactional
    public NoteEngagementBO unfavorite(String userId, String noteId) {
        requirePublic(noteMapper.selectActiveForUpdate(noteId));
        if (favoriteMapper.deleteByUser(noteId, userId) == 1) {
            noteMapper.adjustFavoriteCount(noteId, -1);
        }
        return get(userId, noteId);
    }

    private NoteDO requirePublic(NoteDO note) {
        if (note == null || note.getDeleted() != 0 || note.getStatus() != NoteStatus.PUBLIC) {
            throw BusinessException.notFound("公开 Note 不存在");
        }
        return note;
    }

    private NoteEngagementBO toBO(String userId, NoteDO note) {
        return new NoteEngagementBO(count(note.getLikeCount()), count(note.getFavoriteCount()),
                count(note.getCommentCount()), userId != null && likeMapper.countByUser(note.getId(), userId) > 0,
                userId != null && favoriteMapper.countByUser(note.getId(), userId) > 0);
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
