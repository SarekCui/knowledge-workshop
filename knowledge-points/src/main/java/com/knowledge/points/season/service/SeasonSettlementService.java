package com.knowledge.points.season.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.points.season.bo.SeasonBO;
import com.knowledge.points.season.dao.mapper.SeasonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeasonSettlementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonSettlementService.class);
    @Autowired
    private SeasonSettlementTransactionService transactionService;
    @Autowired
    private SeasonMapper seasonMapper;

    public SeasonBO settle(String season) {
        try {
            return transactionService.settle(season);
        } catch (RuntimeException exception) {
            // 事务已经回滚，失败记录单独提交；条件更新不覆盖并发成功的 SETTLED。
            String message = exception instanceof BusinessException ? exception.getMessage() : "结算执行失败，请查看服务日志后重试";
            seasonMapper.markFailed(season, message);
            LOGGER.warn("event=season_settlement_failed season={} message=赛季结算失败", season, exception);
            throw exception;
        }
    }
}
