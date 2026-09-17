package com.knowledge.learning.course.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.learning.course.enums.CourseCategoryStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("course_category")
public class CourseCategoryDO {
    @TableId
    private String id;
    private String name;
    private Integer sortOrder;
    private CourseCategoryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
