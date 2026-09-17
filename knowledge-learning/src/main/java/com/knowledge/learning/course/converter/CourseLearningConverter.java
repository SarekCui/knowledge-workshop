package com.knowledge.learning.course.converter;

import com.knowledge.learning.course.bo.CourseLearningBO;
import com.knowledge.learning.course.dao.model.CourseLearningDO;
import com.knowledge.learning.course.rule.CourseCompletionRule;
import com.knowledge.learning.course.vo.CourseLearningVO;
import java.time.ZoneOffset;

public final class CourseLearningConverter {
    private CourseLearningConverter() {
    }

    public static CourseLearningBO toBO(CourseLearningDO item) {
        return new CourseLearningBO(item.getCourseId(), item.getTitle(), item.getCoverUrl(), item.getTotalVideos(),
                item.getCompletedVideos(), CourseCompletionRule.percentage(item.getCompletedVideos(), item.getTotalVideos()),
                item.getChapterId(), item.getVideoId(), item.getVideoVersion(), item.getResumePositionMs(),
                item.getLastLearnedAt() == null ? null : item.getLastLearnedAt().toInstant(ZoneOffset.UTC));
    }

    public static CourseLearningVO toVO(CourseLearningBO item) {
        return new CourseLearningVO(item.courseId(), item.title(), item.coverUrl(), item.totalVideos(),
                item.completedVideos(), item.completionRate(), item.chapterId(), item.videoId(), item.videoVersion(),
                item.resumePositionMs(), item.lastLearnedAt());
    }
}
