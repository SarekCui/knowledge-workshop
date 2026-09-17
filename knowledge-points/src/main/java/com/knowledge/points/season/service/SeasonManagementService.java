package com.knowledge.points.season.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.model.PageBO;
import com.knowledge.points.season.bo.SeasonBO;
import com.knowledge.points.season.converter.SeasonConverter;
import com.knowledge.points.season.dao.mapper.SeasonMapper;
import com.knowledge.points.season.dao.model.SeasonDO;
import com.knowledge.points.season.dto.SeasonCreateDTO;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeasonManagementService {
    @Autowired
    private SeasonMapper seasonMapper;
    @Autowired
    private Clock clock;

    @Transactional
    public SeasonBO create(SeasonCreateDTO dto) {
        SeasonDO candidate = SeasonConverter.toDO(dto, Instant.now(clock));
        seasonMapper.insertIfAbsent(candidate);
        SeasonDO saved = seasonMapper.selectById(candidate.getSeason());
        if (!saved.getName().equals(candidate.getName())) {
            throw BusinessException.conflict("该赛季已存在，重复创建必须使用相同名称");
        }
        return SeasonConverter.toBO(saved);
    }

    public SeasonBO get(String season) {
        SeasonDO item = seasonMapper.selectById(season);
        if (item == null) {
            throw BusinessException.notFound("赛季不存在");
        }
        return SeasonConverter.toBO(item);
    }

    public PageBO<SeasonBO> list(int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) {
            throw BusinessException.badRequest("分页参数不合法，每页最多 100 条");
        }
        Page<SeasonDO> page = seasonMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<SeasonDO>lambdaQuery().orderByDesc(SeasonDO::getStartsAt));
        return new PageBO<>(page.getRecords().stream().map(SeasonConverter::toBO).toList(),
                pageNo, pageSize, page.getTotal());
    }
}
