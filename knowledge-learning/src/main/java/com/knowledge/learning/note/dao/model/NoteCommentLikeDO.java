package com.knowledge.learning.note.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("note_comment_like")
public class NoteCommentLikeDO {
    @TableId
    private String id;
    private String commentId;
    private String userId;
    private LocalDateTime createdAt;
}
