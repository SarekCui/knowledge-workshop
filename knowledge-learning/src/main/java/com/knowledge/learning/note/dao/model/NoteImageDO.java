package com.knowledge.learning.note.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.learning.note.enums.NoteImageStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("note_image")
public class NoteImageDO {
    @TableId
    private String id;
    private String userId;
    private String noteId;
    private String objectKey;
    private String contentType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private NoteImageStatus status;
    private LocalDateTime expiresAt;
    private String cleanupToken;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
