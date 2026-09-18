package com.knowledge.learning.note.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.learning.note.enums.NoteCommentAuthorType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("note_comment")
public class NoteCommentDO {
    @TableId
    private String id;
    private String noteId;
    private String userId;
    private NoteCommentAuthorType authorType;
    private String parentCommentId;
    private String sourceCommentId;
    private String clientRequestId;
    private String content;
    private Long likeCount;
    private Integer version;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
