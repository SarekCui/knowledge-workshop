package com.knowledge.points.ranking.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("pt_point_account")
public class PointAccountDO {
    @TableId
    private String userId;
    private Long totalPoints;
    private Integer version;
    private LocalDateTime updatedAt;
}
