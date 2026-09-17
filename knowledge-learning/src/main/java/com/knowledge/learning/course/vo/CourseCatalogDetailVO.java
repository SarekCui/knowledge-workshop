package com.knowledge.learning.course.vo;

import java.util.List;

public record CourseCatalogDetailVO(CourseCatalogVO course, List<CourseChapterSummaryVO> chapters) {
    public CourseCatalogDetailVO {
        chapters = List.copyOf(chapters);
    }
}
