package com.knowledge.marketing.groupbuy.service;

import java.util.List;

public interface SlotReservationService {

    ReservationResult reserve(String groupId, String requestId, String userId, int knownOccupied, int capacity);

    void release(String groupId, String requestId, String userId);

    /** Converts a temporary reservation into a confirmed occupied slot without decrementing the counter. */
    void confirm(String groupId, String requestId, String userId);

    List<ExpiredReservation> findExpired(int limit);

    record ExpiredReservation(String groupId, String requestId, String userId) {
    }

    enum ReservationResult {
        ACQUIRED,
        IDEMPOTENT
    }
}
