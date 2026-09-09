package com.knowledge.points.ranking.dao.model;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("season_account")
public class SeasonAccountDO {
    private String season;
    private String userId;
    private Long points;
    private Integer version;
    private LocalDateTime updatedAt;
}
