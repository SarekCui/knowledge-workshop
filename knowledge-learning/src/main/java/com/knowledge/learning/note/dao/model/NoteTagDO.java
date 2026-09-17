package com.knowledge.learning.note.dao.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteTagDO {
    private String noteId;
    private String normalizedName;
    private String displayName;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
