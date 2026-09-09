package com.knowledge.learning.course.converter;

import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.bo.CourseBO;
import com.knowledge.learning.course.dao.model.ChapterDO;
import com.knowledge.learning.course.dao.model.CourseDO;
import com.knowledge.learning.course.vo.ChapterVO;
import com.knowledge.learning.course.vo.CourseVO;

public final class CourseConverter {

    private CourseConverter() {
    }

    public static CourseBO toBO(CourseDO source) {
        return new CourseBO(source.getId(), source.getTitle(), source.getSummary(), source.getCoverUrl(),
                source.getPriceCents(), source.getStatus(), source.getVersion(), source.getUpdatedAt());
    }

    public static ChapterBO toBO(ChapterDO source) {
        return new ChapterBO(source.getId(), source.getCourseId(), source.getTitle(), source.getSortOrder(),
                source.getVideoId(), source.getVideoUrl(), source.getVideoDurationMs(), source.getVideoVersion(),
                source.getStatus(), source.getVersion());
    }

    public static CourseVO toVO(CourseBO source) {
        return new CourseVO(source.id(), source.title(), source.summary(), source.coverUrl(), source.priceCents(),
                source.status().name(), source.version(), source.updatedAt());
    }

    public static ChapterVO toVO(ChapterBO source) {
        return new ChapterVO(source.id(), source.courseId(), source.title(), source.sortOrder(), source.videoId(),
                source.videoUrl(), source.videoDurationMs(), source.videoVersion(), source.status().name(),
                source.version());
    }
}
