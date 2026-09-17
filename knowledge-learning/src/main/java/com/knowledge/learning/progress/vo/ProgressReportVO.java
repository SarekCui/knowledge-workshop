package com.knowledge.learning.progress.vo;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProgressReportVO(
        String eventId,
        long sessionEpoch,
        long sequence,
        long resumePositionMs,
        @Schema(description = "MQ已确认接收且未被退回，不表示MySQL已落库或该位置赢得排序") boolean accepted,
        @Schema(description = "本次上报已更新Redis快照；false时不要用响应位置覆盖播放器本地状态") boolean cacheUpdated) {
}
