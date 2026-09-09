package com.knowledge.learning.progress.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.learning.progress.enums.ProgressStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("video_progress")
public class VideoProgressDO {
    @TableId
    private String id;
    private String userId;
    private String courseId;
    private String chapterId;
    private String videoId;
    private Integer videoVersion;
    private Long resumePositionMs;
    private Long maxPositionMs;
    private Long durationMs;
    private Long watchedSeconds;
    private Integer completionRate;
    private ProgressStatus status;
    private Long lastSessionEpoch;
    private Long lastSequence;
    private String lastEventId;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
