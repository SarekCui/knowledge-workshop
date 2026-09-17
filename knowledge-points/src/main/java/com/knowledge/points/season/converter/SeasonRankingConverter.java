package com.knowledge.points.season.converter;

import com.knowledge.points.season.bo.SeasonRankingBO;
import com.knowledge.points.season.dao.model.SeasonSnapshotDO;
import com.knowledge.points.season.vo.SeasonRankingVO;

public final class SeasonRankingConverter {
    private SeasonRankingConverter() {
    }

    public static SeasonRankingBO toBO(SeasonSnapshotDO item) {
        return new SeasonRankingBO(item.getUserId(), item.getScore(), item.getRankNo());
    }

    public static SeasonRankingVO toVO(SeasonRankingBO item) {
        return new SeasonRankingVO(item.userId(), item.score(), item.rank());
    }
}
