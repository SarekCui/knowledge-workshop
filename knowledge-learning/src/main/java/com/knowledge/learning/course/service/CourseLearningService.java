package com.knowledge.learning.course.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.model.PageBO;
import com.knowledge.learning.course.bo.CourseLearningBO;
import com.knowledge.learning.course.converter.CourseLearningConverter;
import com.knowledge.learning.course.dao.mapper.CourseLearningMapper;
import com.knowledge.learning.course.dao.model.CourseLearningDO;
import com.knowledge.learning.entitlement.service.EntitlementService;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseLearningService {
    @Autowired
    private CourseLearningMapper learningMapper;
    @Autowired
    private CourseQueryService courseQueryService;
    @Autowired
    private EntitlementService entitlementService;
    @Autowired
    private Clock clock;

    public CourseLearningBO get(String userId, String courseId) {
        courseQueryService.getPublished(courseId);
        entitlementService.requireActive(userId, courseId);
        var item = learningMapper.findMine(userId, courseId, LocalDateTime.now(clock));
        if (item == null) {
            throw BusinessException.forbidden("课程当前不可学习或权益已失效");
        }
        return CourseLearningConverter.toBO(item);
    }

    public PageBO<CourseLearningBO> pageMine(String userId, int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) {
            throw BusinessException.badRequest("分页参数不合法，每页最多100条");
        }
        Page<CourseLearningDO> page = new Page<>(pageNo, pageSize);
        page.setOptimizeCountSql(false);
        learningMapper.pageMine(page, userId, LocalDateTime.now(clock));
        return new PageBO<>(page.getRecords().stream().map(CourseLearningConverter::toBO).toList(),
                pageNo, pageSize, page.getTotal());
    }
}
