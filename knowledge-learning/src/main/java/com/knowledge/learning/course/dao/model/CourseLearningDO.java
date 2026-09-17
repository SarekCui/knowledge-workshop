package com.knowledge.learning.course.dao.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 同库课程学习查询投影，不对应可写业务表。 */
@Getter
@Setter
public class CourseLearningDO {
    private String courseId;
    private String title;
    private String coverUrl;
    private Long totalVideos;
    private Long completedVideos;
    private String chapterId;
    private String videoId;
    private Integer videoVersion;
    private Long resumePositionMs;
    private LocalDateTime lastLearnedAt;
}
