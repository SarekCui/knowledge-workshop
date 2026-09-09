package com.knowledge.marketing.groupbuy.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.marketing.groupbuy.enums.ParticipantStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("group_participant")
public class GroupParticipantDO {
    @TableId
    private String id;
    private String activityId;
    private String groupId;
    private String userId;
    private ParticipantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
