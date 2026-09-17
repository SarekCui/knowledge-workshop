package com.knowledge.points.season.service;

import com.knowledge.points.season.dao.mapper.SeasonMapper;
import com.knowledge.points.season.enums.SeasonStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeasonWriteService {
    @Autowired
    private SeasonMapper seasonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockAndIsSettled(String season) {
        var item = seasonMapper.lock(season);
        return item != null && item.getStatus() == SeasonStatus.SETTLED;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireWritable(String season) {
        if (lockAndIsSettled(season)) {
            throw new SeasonClosedException(season);
        }
    }
}
