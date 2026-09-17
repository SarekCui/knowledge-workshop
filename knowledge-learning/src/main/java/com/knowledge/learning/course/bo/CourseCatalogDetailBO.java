package com.knowledge.learning.course.bo;

import java.util.List;

public record CourseCatalogDetailBO(CourseCatalogBO course, List<CourseChapterSummaryBO> chapters) {
    public CourseCatalogDetailBO {
        chapters = List.copyOf(chapters);
    }
}
