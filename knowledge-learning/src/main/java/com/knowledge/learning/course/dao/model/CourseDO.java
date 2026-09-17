package com.knowledge.learning.course.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.learning.course.enums.CourseStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("course")
public class CourseDO {
    @TableId
    private String id;
    private String categoryId;
    private String title;
    private String summary;
    private String coverUrl;
    private Long priceCents;
    private CourseStatus status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
