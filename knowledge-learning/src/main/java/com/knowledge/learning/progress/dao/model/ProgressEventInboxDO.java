package com.knowledge.learning.progress.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("progress_event_inbox")
public class ProgressEventInboxDO {
    @TableId
    private String id;
    private String consumerName;
    private String eventId;
    private LocalDateTime processedAt;
}
