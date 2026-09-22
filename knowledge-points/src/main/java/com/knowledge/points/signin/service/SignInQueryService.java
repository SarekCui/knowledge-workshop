package com.knowledge.points.signin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.points.signin.bo.SignInMonthBO;
import com.knowledge.points.signin.dao.mapper.SignInRecordMapper;
import com.knowledge.points.signin.dao.model.SignInRecordDO;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SignInQueryService {
    @Resource
    private SignInRecordMapper recordMapper;
    @Resource
    private Clock clock;

    public SignInMonthBO month(String userId, String month) {
        if (month == null || !month.matches("[0-9]{4}-(0[1-9]|1[0-2])") || month.startsWith("0000")) {
            throw BusinessException.badRequest("月份格式必须为 YYYY-MM，年份为 0001—9999");
        }
        YearMonth period = YearMonth.parse(month);
        var records = recordMapper.selectList(Wrappers.<SignInRecordDO>lambdaQuery()
                .eq(SignInRecordDO::getUserId, userId)
                .ge(SignInRecordDO::getSignDate, period.atDay(1))
                .le(SignInRecordDO::getSignDate, period.atEndOfMonth())
                .orderByAsc(SignInRecordDO::getSignDate));
        return new SignInMonthBO(month, LocalDate.now(clock),
                records.stream().map(SignInRecordDO::getSignDate).toList());
    }
}
