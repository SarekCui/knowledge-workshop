package com.knowledge.learning.course.dao.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseCatalogDO {
    private String id;
    private String title;
    private String summary;
    private String coverUrl;
    private Long priceCents;
    private String categoryId;
    private String categoryName;
    private Long chapterCount;
    private Long totalDurationMs;
    private Boolean entitled;
    private LocalDateTime updatedAt;
}
