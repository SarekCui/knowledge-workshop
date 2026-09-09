package com.knowledge.learning.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.bo.CourseBO;
import com.knowledge.learning.course.converter.CourseConverter;
import com.knowledge.learning.course.dao.mapper.ChapterMapper;
import com.knowledge.learning.course.dao.mapper.CourseMapper;
import com.knowledge.learning.course.dao.model.ChapterDO;
import com.knowledge.learning.course.dao.model.CourseDO;
import com.knowledge.learning.course.dto.ChangeCourseStatusDTO;
import com.knowledge.learning.course.dto.CreateChapterDTO;
import com.knowledge.learning.course.dto.CreateCourseDTO;
import com.knowledge.learning.course.dto.UpdateChapterDTO;
import com.knowledge.learning.course.dto.UpdateCourseDTO;
import com.knowledge.learning.course.enums.ChapterStatus;
import com.knowledge.learning.course.enums.CourseStatus;
import com.knowledge.learning.note.service.NoteService;
import com.knowledge.learning.progress.service.ProgressQueryService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseManagementService {

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final CourseQueryService queryService;
    private final NoteService noteService;
    private final ProgressQueryService progressQueryService;
    private final Clock clock;

    public CourseManagementService(CourseMapper courseMapper, ChapterMapper chapterMapper,
                                   CourseQueryService queryService, NoteService noteService,
                                   ProgressQueryService progressQueryService, Clock clock) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.queryService = queryService;
        this.noteService = noteService;
        this.progressQueryService = progressQueryService;
        this.clock = clock;
    }

    @Transactional
    public CourseBO createCourse(CreateCourseDTO request) {
        LocalDateTime now = LocalDateTime.now(clock);
        CourseDO course = new CourseDO();
        course.setId(UUID.randomUUID().toString());
        course.setTitle(request.title().trim());
        course.setSummary(request.summary().trim());
        course.setCoverUrl(normalizeNullable(request.coverUrl()));
        course.setPriceCents(request.priceCents());
        course.setStatus(CourseStatus.DRAFT);
        course.setVersion(0);
        course.setCreatedAt(now);
        course.setUpdatedAt(now);
        courseMapper.insert(course);
        return CourseConverter.toBO(course);
    }

    @Transactional
    public CourseBO updateCourse(String courseId, UpdateCourseDTO request) {
        queryService.getForManagement(courseId);
        int changed = courseMapper.updateDetails(courseId, request.title().trim(), request.summary().trim(),
                normalizeNullable(request.coverUrl()), request.priceCents(), request.version(), LocalDateTime.now(clock));
        if (changed != 1) {
            throw BusinessException.conflict("课程已被其他请求修改，请刷新后重试");
        }
        return queryService.getForManagement(courseId);
    }

    @Transactional
    public CourseBO changeStatus(String courseId, ChangeCourseStatusDTO request) {
        queryService.getForManagement(courseId);
        if (request.status() == CourseStatus.PUBLISHED && chapterCount(courseId) == 0) {
            throw BusinessException.conflict("至少发布一个章节后才能发布课程");
        }
        if (courseMapper.updateStatus(courseId, request.status().name(), request.version(),
                LocalDateTime.now(clock)) != 1) {
            throw BusinessException.conflict("课程已被其他请求修改，请刷新后重试");
        }
        return queryService.getForManagement(courseId);
    }

    @Transactional
    public ChapterBO createChapter(String courseId, CreateChapterDTO request) {
        queryService.getForManagement(courseId);
        LocalDateTime now = LocalDateTime.now(clock);
        ChapterDO chapter = new ChapterDO();
        chapter.setId(UUID.randomUUID().toString());
        chapter.setCourseId(courseId);
        chapter.setTitle(request.title().trim());
        chapter.setSortOrder(request.sortOrder());
        chapter.setVideoId(request.videoId().trim());
        chapter.setVideoUrl(request.videoUrl().trim());
        chapter.setVideoDurationMs(request.videoDurationMs());
        chapter.setVideoVersion(request.videoVersion());
        chapter.setStatus(request.status());
        chapter.setVersion(0);
        chapter.setCreatedAt(now);
        chapter.setUpdatedAt(now);
        chapterMapper.insert(chapter);
        return CourseConverter.toBO(chapter);
    }

    @Transactional
    public ChapterBO updateChapter(String chapterId, UpdateChapterDTO request) {
        requireChapter(chapterId);
        if (chapterMapper.updateDetails(chapterId, request.title().trim(), request.sortOrder(),
                request.videoUrl().trim(), request.videoDurationMs(), request.status().name(),
                request.version(), LocalDateTime.now(clock)) != 1) {
            throw BusinessException.conflict("章节已被其他请求修改，请刷新后重试");
        }
        return CourseConverter.toBO(requireChapter(chapterId));
    }

    @Transactional
    public void deleteChapter(String chapterId, int version) {
        requireChapter(chapterId);
        if (noteService.hasActiveNoteForChapter(chapterId) || progressQueryService.hasProgressForChapter(chapterId)) {
            throw BusinessException.conflict("章节已有笔记或学习进度，不能删除");
        }
        if (chapterMapper.deleteWithVersion(chapterId, version) != 1) {
            throw BusinessException.conflict("章节已被其他请求修改，请刷新后重试");
        }
    }

    private ChapterDO requireChapter(String chapterId) {
        ChapterDO chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw BusinessException.notFound("章节不存在");
        }
        return chapter;
    }

    private long chapterCount(String courseId) {
        return chapterMapper.selectCount(Wrappers.<ChapterDO>lambdaQuery()
                .eq(ChapterDO::getCourseId, courseId)
                .eq(ChapterDO::getStatus, ChapterStatus.PUBLISHED));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
