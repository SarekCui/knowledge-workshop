package com.knowledge.points.ranking.service;

import com.knowledge.points.ranking.dao.mapper.RejectedPointEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RejectedPointEventService {
    @Autowired
    private RejectedPointEventMapper rejectedEventMapper;

    @Transactional
    public void retain(String eventId, String payload, String reason) {
        rejectedEventMapper.insertIfAbsent(eventId, payload, reason);
    }
}
