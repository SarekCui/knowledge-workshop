package com.knowledge.points.ranking.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import com.knowledge.points.ranking.enums.PointTaskStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("pt_point_task")
public class PointTaskDO {
    @TableId
    private String id;
    private String eventId;
    private String payload;
    private PointTaskStatus status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
