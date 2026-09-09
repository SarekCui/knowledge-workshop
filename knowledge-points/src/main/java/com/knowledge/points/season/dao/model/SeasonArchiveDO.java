package com.knowledge.points.season.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("pt_season_archive")
public class SeasonArchiveDO {
    @TableId
    private String id;
    private String season;
    private String tableName;
    private String status;
    private LocalDateTime archivedAt;
}
