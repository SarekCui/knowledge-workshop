package com.knowledge.learning.note.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("lr_note")
public class NoteDO {
    @TableId
    private String id;
    private String userId;
    private String courseId;
    private String chapterId;
    private String clientRequestId;
    private String title;
    private String content;
    private Long videoPositionMs;
    private Integer version;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
