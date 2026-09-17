package com.knowledge.learning.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.model.PageBO;
import com.knowledge.learning.course.bo.CourseCatalogBO;
import com.knowledge.learning.course.bo.CourseCatalogDetailBO;
import com.knowledge.learning.course.bo.CourseCategoryBO;
import com.knowledge.learning.course.converter.CourseCatalogConverter;
import com.knowledge.learning.course.dao.mapper.ChapterMapper;
import com.knowledge.learning.course.dao.mapper.CourseCatalogMapper;
import com.knowledge.learning.course.dao.mapper.CourseCategoryMapper;
import com.knowledge.learning.course.dao.model.ChapterDO;
import com.knowledge.learning.course.dao.model.CourseCatalogDO;
import com.knowledge.learning.course.dao.model.CourseCategoryDO;
import com.knowledge.learning.course.enums.ChapterStatus;
import com.knowledge.learning.course.enums.CourseCategoryStatus;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseCatalogService {
    @Autowired
    private CourseCatalogMapper courseCatalogMapper;
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Autowired
    private ChapterMapper chapterMapper;

    public List<CourseCategoryBO> categories() {
        return courseCategoryMapper.selectList(Wrappers.<CourseCategoryDO>lambdaQuery()
                        .eq(CourseCategoryDO::getStatus, CourseCategoryStatus.ACTIVE)
                        .orderByAsc(CourseCategoryDO::getSortOrder, CourseCategoryDO::getId)
                        .last("LIMIT 100"))
                .stream().map(CourseCatalogConverter::toBO).toList();
    }

    public PageBO<CourseCatalogBO> page(String userId, String categoryId, String keyword,
            int pageNo, int pageSize) {
        String normalizedCategory = normalize(categoryId);
        if (normalizedCategory != null) requireActiveCategory(normalizedCategory);
        Page<CourseCatalogDO> page = new Page<>(pageNo, pageSize);
        courseCatalogMapper.selectPublishedPage(page, userId, normalizedCategory, normalize(keyword));
        List<CourseCatalogBO> items = page.getRecords().stream()
                .map(CourseCatalogConverter::toBO)
                .toList();
        return new PageBO<>(items, pageNo, pageSize, page.getTotal());
    }

    public CourseCatalogDetailBO detail(String userId, String courseId) {
        CourseCatalogDO course = courseCatalogMapper.selectPublishedDetail(userId, courseId);
        if (course == null) throw BusinessException.notFound("课程不存在或尚未发布");
        List<ChapterDO> chapters = chapterMapper.selectList(Wrappers.<ChapterDO>lambdaQuery()
                .eq(ChapterDO::getCourseId, courseId)
                .eq(ChapterDO::getStatus, ChapterStatus.PUBLISHED)
                .orderByAsc(ChapterDO::getSortOrder, ChapterDO::getId)
                .last("LIMIT 500"));
        return new CourseCatalogDetailBO(
                CourseCatalogConverter.toBO(course),
                chapters.stream().map(CourseCatalogConverter::toSummaryBO).toList());
    }

    private void requireActiveCategory(String categoryId) {
        CourseCategoryDO category = courseCategoryMapper.selectById(categoryId);
        if (category == null || category.getStatus() != CourseCategoryStatus.ACTIVE) {
            throw BusinessException.badRequest("课程分类不存在或不可用");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
