package com.knowledge.marketing.groupbuy.service;

import com.knowledge.marketing.groupbuy.dao.model.GroupParticipantDO;
import com.knowledge.marketing.groupbuy.enums.ParticipantStatus;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupParticipantMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.TradeOrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReservationCompensationService {

    @Resource private SlotReservationService slotService;
    @Resource private GroupParticipantMapper participantMapper;
    @Resource private TradeOrderMapper orderMapper;
    @Resource private TransactionTemplate transactionTemplate;
    @Resource private Clock clock;


    public int compensate(int limit) {
        int released = 0;
        for (SlotReservationService.ExpiredReservation reservation : slotService.findExpired(limit)) {
            GroupParticipantDO participant = participantMapper.findByGroupAndUser(
                    reservation.groupId(), reservation.userId());
            if (participant != null && participant.getStatus() == ParticipantStatus.CONFIRMED) {
                slotService.confirm(reservation.groupId(), reservation.userId());
                continue;
            }
            Boolean committed = transactionTemplate.execute(status -> {
                LocalDateTime now = LocalDateTime.now(clock);
                orderMapper.closePendingByGroupAndUser(reservation.groupId(), reservation.userId(), now);
                participantMapper.releaseByGroupAndUser(reservation.groupId(), reservation.userId(), now);
                return Boolean.TRUE;
            });
            if (Boolean.TRUE.equals(committed)) {
                slotService.release(reservation.groupId(), reservation.userId());
                released++;
            }
        }
        return released;
    }
}
