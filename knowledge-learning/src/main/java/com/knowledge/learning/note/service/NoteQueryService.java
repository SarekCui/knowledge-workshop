package com.knowledge.learning.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.model.PageBO;
import com.knowledge.learning.note.bo.NoteBO;
import com.knowledge.learning.note.converter.NoteConverter;
import com.knowledge.learning.note.dao.mapper.NoteFavoriteMapper;
import com.knowledge.learning.note.dao.mapper.NoteLikeMapper;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteFavoriteDO;
import com.knowledge.learning.note.dao.model.NoteLikeDO;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.enums.NoteStatus;
import com.knowledge.learning.note.enums.NoteSort;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class NoteQueryService {
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private NoteTagService noteTagService;
    @Resource
    private NoteLikeMapper noteLikeMapper;
    @Resource
    private NoteFavoriteMapper noteFavoriteMapper;

    public NoteBO getPublic(String noteId) {
        return getPublicForUser(null, noteId);
    }

    public NoteBO getPublicForUser(String userId, String noteId) {
        NoteDO note = noteMapper.selectById(noteId);
        if (note == null || note.getDeleted() != 0 || note.getStatus() != NoteStatus.PUBLIC) {
            throw BusinessException.notFound("公开 Note 不存在");
        }
        return enrichViewer(userId, List.of(NoteConverter.toBO(note, noteTagService.find(noteId)))).get(0);
    }

    public PageBO<NoteBO> publicPage(String courseId, String keyword, NoteSort sort, int pageNo, int pageSize) {
        return publicPage(courseId, keyword, null, sort, pageNo, pageSize);
    }

    public PageBO<NoteBO> publicPage(String courseId, String keyword, String tag, NoteSort sort,
            int pageNo, int pageSize) {
        return publicPageForUser(null, courseId, keyword, tag, sort, pageNo, pageSize);
    }

    public PageBO<NoteBO> publicPageForUser(String userId, String courseId, String keyword, String tag,
            NoteSort sort, int pageNo, int pageSize) {
        validatePage(pageNo, pageSize);
        var query = base(courseId, keyword, tag).eq(NoteDO::getStatus, NoteStatus.PUBLIC);
        if (sort == NoteSort.HOT) {
            query.orderByDesc(NoteDO::getFavoriteCount, NoteDO::getLikeCount, NoteDO::getCommentCount,
                    NoteDO::getPublishedAt, NoteDO::getId);
        } else {
            query.orderByDesc(NoteDO::getPublishedAt, NoteDO::getId);
        }
        return page(query, pageNo, pageSize, userId);
    }

    public PageBO<NoteBO> mine(String userId, String courseId, String keyword, int pageNo, int pageSize) {
        validatePage(pageNo, pageSize);
        return page(base(courseId, keyword, null).eq(NoteDO::getUserId, userId)
                .orderByDesc(NoteDO::getUpdatedAt, NoteDO::getId), pageNo, pageSize, userId);
    }

    public PageBO<NoteBO> liked(String userId, int pageNo, int pageSize) {
        validatePage(pageNo, pageSize);
        long offset = (long) (pageNo - 1) * pageSize;
        return page(noteMapper.selectLiked(userId, offset, pageSize), pageNo, pageSize,
                noteMapper.countLiked(userId), userId);
    }

    public PageBO<NoteBO> favorited(String userId, int pageNo, int pageSize) {
        validatePage(pageNo, pageSize);
        long offset = (long) (pageNo - 1) * pageSize;
        return page(noteMapper.selectFavorited(userId, offset, pageSize), pageNo, pageSize,
                noteMapper.countFavorited(userId), userId);
    }

    private LambdaQueryWrapper<NoteDO> base(String courseId, String keyword, String tag) {
        var query = Wrappers.<NoteDO>lambdaQuery().eq(NoteDO::getDeleted, 0)
                .eq(courseId != null && !courseId.isBlank(), NoteDO::getCourseId,
                        courseId == null ? null : courseId.trim());
        if (keyword != null && !keyword.isBlank()) {
            String term = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String normalizedTerm = noteTagService.normalizeSearchTerm(keyword);
            query.and(search -> search.like(NoteDO::getTitle, term)
                    .or().like(NoteDO::getContent, term)
                    .or().apply("EXISTS (SELECT 1 FROM note_tag nt WHERE nt.note_id = note.id "
                            + "AND LOCATE({0}, nt.normalized_name) > 0)", normalizedTerm));
        }
        String normalizedTag = noteTagService.normalizeFilter(tag);
        if (normalizedTag != null) {
            query.apply("EXISTS (SELECT 1 FROM note_tag nt WHERE nt.note_id = note.id "
                    + "AND nt.normalized_name = {0})", normalizedTag);
        }
        return query;
    }

    private PageBO<NoteBO> page(LambdaQueryWrapper<NoteDO> query, int pageNo, int pageSize, String userId) {
        validatePage(pageNo, pageSize);
        var result = noteMapper.selectPage(new Page<NoteDO>(pageNo, pageSize), query);
        return new PageBO<>(enrichViewer(userId, toBOs(result.getRecords())),
                pageNo, pageSize, result.getTotal());
    }

    private PageBO<NoteBO> page(List<NoteDO> records, int pageNo, int pageSize, long total, String userId) {
        return new PageBO<>(enrichViewer(userId, toBOs(records)), pageNo, pageSize, total);
    }

    private void validatePage(int pageNo, int pageSize) {
        if (pageNo < 1 || pageNo > 1000 || pageSize < 1 || pageSize > 50) {
            throw BusinessException.badRequest("页码须为1—1000，每页条数须为1—50");
        }
    }

    private List<NoteBO> toBOs(List<NoteDO> notes) {
        if (notes.isEmpty()) return List.of();
        Map<String, List<String>> tags = noteTagService.findByNoteIds(
                notes.stream().map(NoteDO::getId).toList());
        return notes.stream()
                .map(note -> NoteConverter.toBO(note, tags.getOrDefault(note.getId(), List.of())))
                .toList();
    }

    private List<NoteBO> enrichViewer(String userId, List<NoteBO> notes) {
        if (userId == null || userId.isBlank() || notes.isEmpty()) return notes;
        List<String> noteIds = notes.stream().map(NoteBO::id).toList();
        Set<String> likedIds = noteLikeMapper.selectList(Wrappers.<NoteLikeDO>lambdaQuery()
                        .select(NoteLikeDO::getNoteId).eq(NoteLikeDO::getUserId, userId)
                        .in(NoteLikeDO::getNoteId, noteIds)).stream()
                .map(NoteLikeDO::getNoteId).collect(Collectors.toSet());
        Set<String> favoritedIds = noteFavoriteMapper.selectList(Wrappers.<NoteFavoriteDO>lambdaQuery()
                        .select(NoteFavoriteDO::getNoteId).eq(NoteFavoriteDO::getUserId, userId)
                        .in(NoteFavoriteDO::getNoteId, noteIds)).stream()
                .map(NoteFavoriteDO::getNoteId).collect(Collectors.toSet());
        return notes.stream().map(note -> new NoteBO(note.id(), note.courseId(), note.chapterId(), note.title(),
                note.content(), note.videoPositionMs(), note.version(), note.createdAt(), note.updatedAt(),
                note.authorId(), note.status(), note.publishedAt(), note.likeCount(), note.favoriteCount(),
                note.commentCount(), note.tags(), likedIds.contains(note.id()), favoritedIds.contains(note.id())))
                .toList();
    }
}
