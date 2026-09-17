package com.knowledge.points.season.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.model.PageBO;
import com.knowledge.points.season.bo.SeasonRankingBO;
import com.knowledge.points.season.converter.SeasonRankingConverter;
import com.knowledge.points.season.dao.mapper.SeasonSnapshotMapper;
import com.knowledge.points.season.dao.model.SeasonSnapshotDO;
import com.knowledge.points.season.enums.SeasonStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeasonHistoryService {
    @Autowired
    private SeasonManagementService managementService;
    @Autowired
    private SeasonSnapshotMapper snapshotMapper;

    public PageBO<SeasonRankingBO> ranking(String season, int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) {
            throw BusinessException.badRequest("分页参数不合法，每页最多 100 条");
        }
        if (managementService.get(season).status() != SeasonStatus.SETTLED) {
            throw BusinessException.conflict("赛季未结算，不能查询最终历史榜单");
        }
        Page<SeasonSnapshotDO> page = snapshotMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<SeasonSnapshotDO>lambdaQuery().eq(SeasonSnapshotDO::getSeason, season)
                        .orderByAsc(SeasonSnapshotDO::getRankNo));
        return new PageBO<>(page.getRecords().stream().map(SeasonRankingConverter::toBO).toList(),
                pageNo, pageSize, page.getTotal());
    }
}
