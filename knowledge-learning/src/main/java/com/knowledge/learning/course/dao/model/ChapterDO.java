package com.knowledge.learning.course.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.learning.course.enums.ChapterStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("chapter")
public class ChapterDO {
    @TableId
    private String id;
    private String courseId;
    private String title;
    private Integer sortOrder;
    private String videoId;
    private String videoUrl;
    private Long videoDurationMs;
    private Integer videoVersion;
    private ChapterStatus status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
