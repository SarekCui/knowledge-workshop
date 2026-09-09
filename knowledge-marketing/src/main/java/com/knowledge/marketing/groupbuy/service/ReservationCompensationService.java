package com.knowledge.marketing.groupbuy.service;

import com.knowledge.marketing.groupbuy.dao.model.GroupParticipantDO;
import com.knowledge.marketing.groupbuy.enums.ParticipantStatus;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupParticipantMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.TradeOrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReservationCompensationService {

    private final SlotReservationService slotService;
    private final GroupParticipantMapper participantMapper;
    private final TradeOrderMapper orderMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ReservationCompensationService(SlotReservationService slotService,
                                          GroupParticipantMapper participantMapper,
                                          TradeOrderMapper orderMapper,
                                          TransactionTemplate transactionTemplate,
                                          Clock clock) {
        this.slotService = slotService;
        this.participantMapper = participantMapper;
        this.orderMapper = orderMapper;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

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
