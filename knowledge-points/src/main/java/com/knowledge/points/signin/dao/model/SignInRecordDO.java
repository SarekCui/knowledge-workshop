package com.knowledge.points.signin.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("pt_signin_record")
public class SignInRecordDO {
    @TableId
    private String id;
    private String userId;
    private LocalDate signDate;
    private String eventId;
    private Integer continuousDays;
    private Integer rewardPoints;
    private LocalDateTime createdAt;
}
