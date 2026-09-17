package com.knowledge.points.season.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import com.knowledge.points.season.enums.SeasonStatus;

@Getter
@Setter
@TableName("season")
public class SeasonDO {
    @TableId
    private String season;
    private String name;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
    private SeasonStatus status;
    private LocalDateTime settledAt;
    private Integer snapshotCount;
    private String lastError;
}
