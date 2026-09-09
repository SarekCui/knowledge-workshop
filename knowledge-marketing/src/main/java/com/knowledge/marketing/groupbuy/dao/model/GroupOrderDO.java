package com.knowledge.marketing.groupbuy.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.marketing.groupbuy.enums.GroupStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("mk_group_order")
public class GroupOrderDO {
    @TableId
    private String id;
    private String activityId;
    private String ownerUserId;
    private GroupStatus status;
    private Integer targetCount;
    private Integer confirmedCount;
    private LocalDateTime expiresAt;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
