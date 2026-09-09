package com.knowledge.points.season.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("job_execution")
public class JobExecutionDO {
    @TableId
    private String id;
    private String jobName;
    private String businessKey;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
