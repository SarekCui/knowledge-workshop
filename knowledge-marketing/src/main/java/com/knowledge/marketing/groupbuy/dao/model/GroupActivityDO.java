package com.knowledge.marketing.groupbuy.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.marketing.groupbuy.enums.ActivityStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("group_activity")
public class GroupActivityDO {
    @TableId
    private String id;
    private String courseId;
    private ActivityStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer targetCount;
    private Integer maxJoinPerUser;
    private Long priceCents;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
