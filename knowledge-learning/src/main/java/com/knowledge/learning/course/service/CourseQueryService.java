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
import com.knowledge.learning.course.enums.ChapterStatus;
import com.knowledge.learning.course.enums.CourseStatus;
import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class CourseQueryService {

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ChapterMapper chapterMapper;

    public List<CourseBO> listPublished() {
        return courseMapper.selectList(Wrappers.<CourseDO>lambdaQuery()
                        .eq(CourseDO::getStatus, CourseStatus.PUBLISHED)
                        .orderByDesc(CourseDO::getUpdatedAt)
                        .last("LIMIT 100"))
                .stream().map(CourseConverter::toBO).toList();
    }

    public CourseBO getPublished(String courseId) {
        CourseDO course = requireCourse(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw BusinessException.notFound("课程不存在或尚未发布");
        }
        return CourseConverter.toBO(course);
    }

    public CourseBO getForManagement(String courseId) {
        return CourseConverter.toBO(requireCourse(courseId));
    }

    public List<ChapterBO> listPublishedChapters(String courseId) {
        getPublished(courseId);
        return chapterMapper.selectList(Wrappers.<ChapterDO>lambdaQuery()
                        .eq(ChapterDO::getCourseId, courseId)
                        .eq(ChapterDO::getStatus, ChapterStatus.PUBLISHED)
                        .orderByAsc(ChapterDO::getSortOrder, ChapterDO::getId)
                        .last("LIMIT 500"))
                .stream().map(CourseConverter::toBO).toList();
    }

    public ChapterBO requirePublishedVideo(String videoId) {
        ChapterDO chapter = chapterMapper.findLatestByVideoId(videoId);
        if (chapter == null || chapter.getStatus() != ChapterStatus.PUBLISHED) {
            throw BusinessException.notFound("视频不存在或尚未发布");
        }
        getPublished(chapter.getCourseId());
        return CourseConverter.toBO(chapter);
    }

    public ChapterBO requirePublishedCourseAndOptionalChapter(String courseId, String chapterId) {
        getPublished(courseId);
        if (chapterId == null) {
            return null;
        }
        ChapterDO chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || chapter.getStatus() != ChapterStatus.PUBLISHED
                || !courseId.equals(chapter.getCourseId())) {
            throw BusinessException.badRequest("章节不存在、未发布或不属于指定课程");
        }
        return CourseConverter.toBO(chapter);
    }

    private CourseDO requireCourse(String courseId) {
        CourseDO course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        return course;
    }
}
