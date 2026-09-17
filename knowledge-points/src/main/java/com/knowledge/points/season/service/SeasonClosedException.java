package com.knowledge.points.season.service;

public class SeasonClosedException extends RuntimeException {
    public SeasonClosedException(String season) {
        super("赛季已结算，晚到积分需人工处理: " + season);
    }
}
