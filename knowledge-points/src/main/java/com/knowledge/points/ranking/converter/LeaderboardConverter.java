package com.knowledge.points.ranking.converter;

import com.knowledge.points.ranking.bo.LeaderboardItemBO;
import com.knowledge.points.ranking.vo.LeaderboardItemVO;
import java.util.List;

public final class LeaderboardConverter {

    private LeaderboardConverter() {
    }

    public static List<LeaderboardItemVO> toVOList(List<LeaderboardItemBO> items) {
        return items.stream()
                .map(item -> new LeaderboardItemVO(item.userId(), item.score(), item.rank()))
                .toList();
    }
}
