package com.knowledge.points.season.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("season_snapshot")
public class SeasonSnapshotDO {
    @TableId
    private String id;
    private String season;
    private String userId;
    private Long score;
    private Integer rankNo;
    private LocalDateTime snapshotAt;
}
