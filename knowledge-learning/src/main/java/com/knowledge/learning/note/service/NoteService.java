package com.knowledge.learning.note.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.note.bo.NoteBO;
import com.knowledge.learning.note.converter.NoteConverter;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.dto.CreateNoteDTO;
import com.knowledge.learning.note.dto.RenameNoteDTO;
import com.knowledge.learning.note.dto.UpdateNoteDTO;
import com.knowledge.learning.note.dto.ChangeNoteStatusDTO;
import com.knowledge.learning.note.enums.NoteStatus;
import jakarta.annotation.Resource;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    @Resource
    private NoteMapper noteMapper;
    @Resource
    private CourseQueryService courseQueryService;
    @Resource
    private EntitlementService entitlementService;
    @Resource
    private NoteTagService noteTagService;
    @Resource
    private NoteImageService noteImageService;
    @Resource
    private Clock clock;

    @Transactional
    public NoteBO create(String userId, CreateNoteDTO request) {
        List<String> tags = noteTagService.normalize(request.tags());
        String chapterId = normalizeNullable(request.chapterId());
        String courseId = normalizeNullable(request.courseId());
        if (courseId == null && chapterId != null) {
            throw BusinessException.badRequest("无关联课程的 Note 不能设置章节");
        }
        ChapterBO chapter = courseId == null ? null
                : courseQueryService.requirePublishedCourseAndOptionalChapter(courseId, chapterId);
        if (request.videoPositionMs() != null && courseId != null) {
            entitlementService.requireActive(userId, courseId);
        }
        validatePosition(request.videoPositionMs(), chapter);
        NoteDO note = new NoteDO();
        note.setId(UUID.randomUUID().toString());
        note.setUserId(userId);
        note.setCourseId(courseId);
        note.setChapterId(chapterId);
        note.setClientRequestId(request.idempotencyKey().trim());
        note.setTitle(normalizeTitle(request.title()));
        note.setContent(request.content().trim());
        note.setVideoPositionMs(request.videoPositionMs());
        note.setVersion(0);
        note.setDeleted(0);
        note.setStatus(NoteStatus.DRAFT);
        note.setLikeCount(0L);
        note.setFavoriteCount(0L);
        note.setCommentCount(0L);
        LocalDateTime now = LocalDateTime.now(clock);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        try {
            noteMapper.insert(note);
            noteTagService.replace(note.getId(), tags);
            noteImageService.syncReferences(userId, note.getId(), note.getContent());
            return NoteConverter.toBO(note, tags);
        } catch (DuplicateKeyException duplicate) {
            NoteDO existing = noteMapper.findByRequest(userId, request.idempotencyKey().trim());
            List<String> existingTags = existing == null ? List.of() : noteTagService.find(existing.getId());
            if (existing != null && sameCreate(existing, note) && existingTags.equals(tags)) {
                return NoteConverter.toBO(existing, existingTags);
            }
            throw BusinessException.conflict("相同 idempotencyKey 已用于其他笔记内容");
        }
    }

    public NoteBO get(String userId, String noteId) {
        NoteDO note = requireOwned(userId, noteId);
        return NoteConverter.toBO(note, noteTagService.find(noteId));
    }

    public List<NoteBO> list(String userId, String courseId, String chapterId, String keyword, int limit) {
        var query = Wrappers.<NoteDO>lambdaQuery()
                .eq(NoteDO::getUserId, userId)
                .eq(NoteDO::getDeleted, 0)
                .eq(courseId != null && !courseId.isBlank(), NoteDO::getCourseId, courseId)
                .eq(chapterId != null && !chapterId.isBlank(), NoteDO::getChapterId, chapterId)
                .like(keyword != null && !keyword.isBlank(), NoteDO::getTitle, keyword == null ? null : keyword.trim())
                .orderByDesc(NoteDO::getUpdatedAt)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 100));
        return toBOs(noteMapper.selectList(query));
    }

    @Transactional
    public NoteBO update(String userId, String noteId, UpdateNoteDTO request) {
        NoteDO note = requireEditable(userId, noteId);
        List<String> tags = noteTagService.normalize(request.tags());
        ChapterBO chapter = note.getCourseId() == null ? null
                : courseQueryService.requirePublishedCourseAndOptionalChapter(note.getCourseId(), note.getChapterId());
        validatePosition(request.videoPositionMs(), chapter);
        if (request.videoPositionMs() != null) entitlementService.requireActive(userId, note.getCourseId());
        if (noteMapper.updateContent(noteId, userId, normalizeTitle(request.title()), request.content().trim(),
                request.videoPositionMs(), request.version(), LocalDateTime.now(clock)) != 1) {
            throw BusinessException.conflict("Note 已被其他设备修改，请刷新后重试");
        }
        noteTagService.replace(noteId, tags);
        noteImageService.syncReferences(userId, noteId, request.content());
        return get(userId, noteId);
    }

    @Transactional
    public NoteBO rename(String userId, String noteId, RenameNoteDTO request) {
        requireEditable(userId, noteId);
        if (noteMapper.rename(noteId, userId, normalizeTitle(request.title()), request.version(),
                LocalDateTime.now(clock)) != 1) {
            throw BusinessException.conflict("Note 已被其他设备修改，请刷新后重试");
        }
        return get(userId, noteId);
    }

    @Transactional
    public void delete(String userId, String noteId, int version) {
        requireOwned(userId, noteId);
        if (noteMapper.softDelete(noteId, userId, version, LocalDateTime.now(clock)) != 1) {
            throw BusinessException.conflict("Note 已被其他设备修改，请刷新后重试");
        }
        noteImageService.releaseAll(noteId);
    }

    public boolean hasActiveNoteForChapter(String chapterId) {
        return noteMapper.countActiveByChapter(chapterId) > 0;
    }

    @Transactional
    public NoteBO changeStatus(String userId, String noteId, ChangeNoteStatusDTO request) {
        NoteDO note = requireOwned(userId, noteId);
        if (request.status() == NoteStatus.PUBLIC && note.getContent().isBlank()) {
            throw BusinessException.badRequest("发布 Note 必须填写正文");
        }
        if (note.getVersion() != request.version()) {
            throw BusinessException.conflict("Note 已被其他设备修改，请刷新后重试");
        }
        if (note.getStatus() == request.status()) return NoteConverter.toBO(note, noteTagService.find(noteId));
        LocalDateTime now = LocalDateTime.now(clock);
        if (noteMapper.changeStatus(noteId, userId, request.status(), request.version(),
                request.status() == NoteStatus.PUBLIC ? now : null, now) != 1) {
            throw BusinessException.conflict("Note 状态已变化，请刷新后重试");
        }
        return get(userId, noteId);
    }

    private NoteDO requireEditable(String userId, String noteId) {
        NoteDO note = requireOwned(userId, noteId);
        if (note.getStatus() == NoteStatus.PUBLIC) {
            throw BusinessException.conflict("请先将公开 Note 转为草稿或私人，再进行编辑");
        }
        return note;
    }

    private NoteDO requireOwned(String userId, String noteId) {
        NoteDO note = noteMapper.selectById(noteId);
        if (note == null || note.getDeleted() != 0) {
            throw BusinessException.notFound("Note 不存在");
        }
        if (!userId.equals(note.getUserId())) {
            throw BusinessException.forbidden("无权访问该 Note");
        }
        return note;
    }

    private String normalizeTitle(String title) {
        String normalized = title.trim();
        if (normalized.isEmpty()) {
            throw BusinessException.badRequest("Note 标题不能为空");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validatePosition(Long position, ChapterBO chapter) {
        validateNonNegativePosition(position);
        if (position != null && chapter == null) {
            throw BusinessException.badRequest("课程级 Note 不能设置视频位置");
        }
        if (position != null && position > chapter.videoDurationMs()) {
            throw BusinessException.badRequest("视频位置不能超过章节视频时长");
        }
    }

    private void validateNonNegativePosition(Long position) {
        if (position != null && position < 0) {
            throw BusinessException.badRequest("视频位置不能小于 0");
        }
    }

    private boolean sameCreate(NoteDO existing, NoteDO requested) {
        return existing.getDeleted() == 0
                && java.util.Objects.equals(existing.getCourseId(), requested.getCourseId())
                && java.util.Objects.equals(existing.getChapterId(), requested.getChapterId())
                && existing.getTitle().equals(requested.getTitle())
                && existing.getContent().equals(requested.getContent())
                && java.util.Objects.equals(existing.getVideoPositionMs(), requested.getVideoPositionMs());
    }

    private List<NoteBO> toBOs(List<NoteDO> notes) {
        if (notes.isEmpty()) return List.of();
        var tags = noteTagService.findByNoteIds(notes.stream().map(NoteDO::getId).toList());
        return notes.stream()
                .map(note -> NoteConverter.toBO(note, tags.getOrDefault(note.getId(), List.of())))
                .toList();
    }
}
