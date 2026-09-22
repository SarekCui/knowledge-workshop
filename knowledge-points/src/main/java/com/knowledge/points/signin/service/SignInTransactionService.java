package com.knowledge.points.signin.service;

import com.knowledge.api.points.dto.PointGrantEventDTO;
import com.knowledge.points.ranking.service.PointTaskService;
import com.knowledge.points.signin.dao.model.SignInRecordDO;
import com.knowledge.points.signin.dao.mapper.SignInRecordMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignInTransactionService {

    @Resource private SignInRecordMapper recordMapper;
    @Resource private PointTaskService pointTaskService;
    @Resource private Clock clock;


    @Transactional
    public SignInRecordDO record(String userId, LocalDate date, int continuousDays, int rewardPoints) {
        LocalDateTime now = LocalDateTime.now(clock);
        String eventId = "signin:" + userId + ':' + date;
        SignInRecordDO record = new SignInRecordDO();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setSignDate(date);
        record.setEventId(eventId);
        record.setContinuousDays(continuousDays);
        record.setRewardPoints(rewardPoints);
        record.setCreatedAt(now);
        recordMapper.insert(record);

        PointGrantEventDTO event = new PointGrantEventDTO(eventId, userId, rewardPoints, "SIGN_IN",
                record.getId(), eventId, Instant.now(clock), 1);
        pointTaskService.create(event, now);
        return record;
    }
}
