package com.knowledge.marketing.groupbuy.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.marketing.groupbuy.dao.model.GroupParticipantDO;
import com.knowledge.marketing.groupbuy.enums.ParticipantStatus;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupParticipantMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.TradeOrderMapper;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class ReservationCompensationServiceTest {

    @Test
    void confirmedParticipantRemovesTemporaryMarkerWithoutReleasingCapacity() {
        SlotReservationService slots = mock(SlotReservationService.class);
        GroupParticipantMapper participants = mock(GroupParticipantMapper.class);
        TradeOrderMapper orders = mock(TradeOrderMapper.class);
        TransactionTemplate transaction = mock(TransactionTemplate.class);
        var expired = new SlotReservationService.ExpiredReservation("g1", "u1");
        when(slots.findExpired(10)).thenReturn(List.of(expired));
        GroupParticipantDO participant = new GroupParticipantDO();
        participant.setStatus(ParticipantStatus.CONFIRMED);
        when(participants.findByGroupAndUser("g1", "u1")).thenReturn(participant);

        ReservationCompensationService service = new ReservationCompensationService();
        ReflectionTestUtils.setField(service, "slotService", slots);
        ReflectionTestUtils.setField(service, "participantMapper", participants);
        ReflectionTestUtils.setField(service, "orderMapper", orders);
        ReflectionTestUtils.setField(service, "transactionTemplate", transaction);
        ReflectionTestUtils.setField(service, "clock", Clock.systemUTC());
        service.compensate(10);

        verify(slots).confirm("g1", "u1");
        verify(slots, never()).release("g1", "u1");
    }
}
