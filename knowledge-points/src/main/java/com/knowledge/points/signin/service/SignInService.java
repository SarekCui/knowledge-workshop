package com.knowledge.points.signin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.lock.annotation.DistributedLock;
import com.knowledge.points.signin.bo.SignInBO;
import com.knowledge.points.signin.converter.SignInConverter;
import com.knowledge.points.signin.dao.model.SignInRecordDO;
import com.knowledge.points.signin.dao.mapper.SignInRecordMapper;
import java.time.LocalDate;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class SignInService {

    @Resource private SignInRecordMapper recordMapper;
    @Resource private SignInBitmapService bitmapService;
    @Resource private SignInRewardPolicy rewardPolicy;
    @Resource private SignInTransactionService transactionService;


    @DistributedLock(keys = SignInRedisKey.LOCK_SPEL, leaseTime = 15)
    public SignInBO sign(String userId, LocalDate date) {
        SignInRecordDO existing = find(userId, date);
        if (existing != null) {
            bitmapService.markSigned(userId, date);
            return response(existing, true);
        }

        bitmapService.markSigned(userId, date);
        int continuousDays = bitmapService.continuousDays(userId, date);
        int reward = rewardPolicy.reward(continuousDays);
        try {
            return response(transactionService.record(userId, date, continuousDays, reward), false);
        } catch (DuplicateKeyException exception) {
            SignInRecordDO raced = find(userId, date);
            if (raced != null) {
                return response(raced, true);
            }
            throw exception;
        } catch (RuntimeException exception) {
            bitmapService.clear(userId, date);
            throw exception;
        }
    }

    private SignInRecordDO find(String userId, LocalDate date) {
        return recordMapper.selectOne(Wrappers.<SignInRecordDO>lambdaQuery()
                .eq(SignInRecordDO::getUserId, userId)
                .eq(SignInRecordDO::getSignDate, date));
    }

    private SignInBO response(SignInRecordDO record, boolean duplicated) {
        return SignInConverter.toBO(record, duplicated);
    }
}
