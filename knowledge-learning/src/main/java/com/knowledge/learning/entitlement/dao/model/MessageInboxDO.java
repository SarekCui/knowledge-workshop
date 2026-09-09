package com.knowledge.learning.entitlement.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("message_inbox")
public class MessageInboxDO {
    @TableId
    private String id;
    private String consumerName;
    private String eventId;
    private LocalDateTime processedAt;
}
