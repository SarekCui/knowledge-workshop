package com.knowledge.learning.progress.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("lr_watched_segment")
public class WatchedSegmentDO {
    @TableId
    private String id;
    private String userId;
    private String videoId;
    private Integer videoVersion;
    private Integer segmentIndex;
    private LocalDateTime createdAt;
}
