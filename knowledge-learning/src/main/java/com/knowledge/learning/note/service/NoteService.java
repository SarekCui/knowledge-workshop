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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private final NoteMapper noteMapper;
    private final CourseQueryService courseQueryService;
    private final EntitlementService entitlementService;
    private final Clock clock;

    public NoteService(NoteMapper noteMapper, CourseQueryService courseQueryService,
                       EntitlementService entitlementService, Clock clock) {
        this.noteMapper = noteMapper;
        this.courseQueryService = courseQueryService;
        this.entitlementService = entitlementService;
        this.clock = clock;
    }

    @Transactional
    public NoteBO create(String userId, CreateNoteDTO request) {
        String chapterId = normalizeNullable(request.chapterId());
        ChapterBO chapter = courseQueryService.requirePublishedCourseAndOptionalChapter(request.courseId(), chapterId);
        entitlementService.requireActive(userId, request.courseId());
        validatePosition(request.videoPositionMs(), chapter);
        NoteDO note = new NoteDO();
        note.setId(UUID.randomUUID().toString());
        note.setUserId(userId);
        note.setCourseId(request.courseId());
        note.setChapterId(chapterId);
        note.setClientRequestId(request.clientRequestId().trim());
        note.setTitle(normalizeTitle(request.title()));
        note.setContent(request.content().trim());
        note.setVideoPositionMs(request.videoPositionMs());
        note.setVersion(0);
        note.setDeleted(0);
        LocalDateTime now = LocalDateTime.now(clock);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        try {
            noteMapper.insert(note);
            return NoteConverter.toBO(note);
        } catch (DuplicateKeyException duplicate) {
            NoteDO existing = noteMapper.findByRequest(userId, request.clientRequestId().trim());
            if (existing != null && sameCreate(existing, note)) {
                return NoteConverter.toBO(existing);
            }
            throw BusinessException.conflict("相同 clientRequestId 已用于其他笔记内容");
        }
    }

    public NoteBO get(String userId, String noteId) {
        return NoteConverter.toBO(requireOwned(userId, noteId));
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
        return noteMapper.selectList(query).stream().map(NoteConverter::toBO).toList();
    }

    @Transactional
    public NoteBO update(String userId, String noteId, UpdateNoteDTO request) {
        requireOwned(userId, noteId);
        validateNonNegativePosition(request.videoPositionMs());
        if (noteMapper.updateContent(noteId, userId, normalizeTitle(request.title()), request.content().trim(),
                request.videoPositionMs(), request.version(), LocalDateTime.now(clock)) != 1) {
            throw BusinessException.conflict("Note 已被其他设备修改，请刷新后重试");
        }
        return get(userId, noteId);
    }

    @Transactional
    public NoteBO rename(String userId, String noteId, RenameNoteDTO request) {
        requireOwned(userId, noteId);
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
    }

    public boolean hasActiveNoteForChapter(String chapterId) {
        return noteMapper.countActiveByChapter(chapterId) > 0;
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
                && existing.getCourseId().equals(requested.getCourseId())
                && java.util.Objects.equals(existing.getChapterId(), requested.getChapterId())
                && existing.getTitle().equals(requested.getTitle())
                && existing.getContent().equals(requested.getContent())
                && java.util.Objects.equals(existing.getVideoPositionMs(), requested.getVideoPositionMs());
    }
}
