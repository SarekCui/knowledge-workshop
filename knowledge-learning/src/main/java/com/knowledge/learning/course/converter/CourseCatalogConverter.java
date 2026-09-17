package com.knowledge.learning.course.converter;

import com.knowledge.learning.course.bo.CourseCatalogBO;
import com.knowledge.learning.course.bo.CourseCatalogDetailBO;
import com.knowledge.learning.course.bo.CourseCategoryBO;
import com.knowledge.learning.course.bo.CourseChapterSummaryBO;
import com.knowledge.learning.course.dao.model.ChapterDO;
import com.knowledge.learning.course.dao.model.CourseCatalogDO;
import com.knowledge.learning.course.dao.model.CourseCategoryDO;
import com.knowledge.learning.course.vo.CourseCatalogDetailVO;
import com.knowledge.learning.course.vo.CourseCatalogVO;
import com.knowledge.learning.course.vo.CourseCategoryVO;
import com.knowledge.learning.course.vo.CourseChapterSummaryVO;

public final class CourseCatalogConverter {
    private CourseCatalogConverter() {
    }

    public static CourseCategoryBO toBO(CourseCategoryDO source) {
        return new CourseCategoryBO(source.getId(), source.getName());
    }

    public static CourseCatalogBO toBO(CourseCatalogDO source) {
        return new CourseCatalogBO(source.getId(), source.getTitle(), source.getSummary(), source.getCoverUrl(),
                source.getPriceCents(), source.getCategoryId(), source.getCategoryName(), source.getChapterCount(),
                source.getTotalDurationMs(), Boolean.TRUE.equals(source.getEntitled()), source.getUpdatedAt());
    }

    public static CourseChapterSummaryBO toSummaryBO(ChapterDO source) {
        return new CourseChapterSummaryBO(source.getId(), source.getTitle(), source.getSortOrder(),
                source.getVideoDurationMs());
    }

    public static CourseCategoryVO toVO(CourseCategoryBO source) {
        return new CourseCategoryVO(source.id(), source.name());
    }

    public static CourseCatalogVO toVO(CourseCatalogBO source) {
        return new CourseCatalogVO(source.id(), source.title(), source.summary(), source.coverUrl(),
                source.priceCents(), source.categoryId(), source.categoryName(), source.chapterCount(),
                source.totalDurationMs(), source.entitled(), source.updatedAt());
    }

    public static CourseChapterSummaryVO toVO(CourseChapterSummaryBO source) {
        return new CourseChapterSummaryVO(source.id(), source.title(), source.sortOrder(), source.videoDurationMs());
    }

    public static CourseCatalogDetailVO toVO(CourseCatalogDetailBO source) {
        return new CourseCatalogDetailVO(toVO(source.course()),
                source.chapters().stream().map(CourseCatalogConverter::toVO).toList());
    }
}
