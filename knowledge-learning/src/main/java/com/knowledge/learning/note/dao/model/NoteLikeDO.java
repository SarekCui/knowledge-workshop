package com.knowledge.learning.note.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("note_like")
public class NoteLikeDO {
    @TableId
    private String id;
    private String noteId;
    private String userId;
    private LocalDateTime createdAt;
}
