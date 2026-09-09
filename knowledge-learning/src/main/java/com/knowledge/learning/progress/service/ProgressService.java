package com.knowledge.learning.progress.service;

import com.knowledge.api.learning.dto.PlayedRangeEventDTO;
import com.knowledge.api.learning.dto.VideoProgressReportedEventDTO;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.progress.bo.ProgressReportBO;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import com.knowledge.learning.progress.dto.PlayedRangeDTO;
import com.knowledge.learning.progress.dto.ReportProgressDTO;
import com.knowledge.learning.progress.enums.ProgressStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressService.class);

    private final CourseQueryService courseQueryService;
    private final EntitlementService entitlementService;
    private final PlaybackSessionService sessionService;
    private final ProgressEventPublisher eventPublisher;
    private final ProgressCacheService cacheService;
    private final ProgressQueryService queryService;
    private final Clock clock;

    public ProgressService(CourseQueryService courseQueryService, EntitlementService entitlementService,
                           PlaybackSessionService sessionService, ProgressEventPublisher eventPublisher,
                           ProgressCacheService cacheService, ProgressQueryService queryService, Clock clock) {
        this.courseQueryService = courseQueryService;
        this.entitlementService = entitlementService;
        this.sessionService = sessionService;
        this.eventPublisher = eventPublisher;
        this.cacheService = cacheService;
        this.queryService = queryService;
        this.clock = clock;
    }

    public ProgressReportBO report(String userId, String videoId, ReportProgressDTO request) {
        ChapterBO chapter = courseQueryService.requirePublishedVideo(videoId);
        entitlementService.requireActive(userId, chapter.courseId());
        validate(request, chapter.videoDurationMs());
        sessionService.validate(userId, videoId, request.sessionId(), request.sessionEpoch(), chapter.videoVersion());
        List<PlayedRangeEventDTO> ranges = request.playedRanges().stream()
                .map(range -> new PlayedRangeEventDTO(range.startMs(), range.endMs())).toList();
        VideoProgressReportedEventDTO event = new VideoProgressReportedEventDTO(
                request.eventId().trim(), userId, chapter.courseId(), chapter.id(), videoId, chapter.videoVersion(),
                request.sessionId(), request.sessionEpoch(), request.sequence(), request.eventType().name(),
                request.positionMs(), chapter.videoDurationMs(), ranges, request.clientOccurredAt(),
                java.time.Instant.now(clock), 1);
        eventPublisher.publish(event);

        VideoProgressBO current = queryService.get(userId, videoId);
        VideoProgressBO snapshot = new VideoProgressBO(chapter.courseId(), chapter.id(), videoId,
                chapter.videoVersion(), request.positionMs(), Math.max(current.maxPositionMs(), request.positionMs()),
                chapter.videoDurationMs(), current.watchedSeconds(), current.completionRate(),
                current.status() == ProgressStatus.NOT_STARTED ? ProgressStatus.LEARNING : current.status(),
                request.sessionEpoch(), request.sequence(), LocalDateTime.now(clock));
        boolean accepted = true;
        try {
            accepted = cacheService.put(userId, snapshot);
        } catch (DataAccessException cacheFailure) {
            LOGGER.warn("Progress event is durable but Redis snapshot update failed, eventId={}",
                    request.eventId(), cacheFailure);
        }
        return new ProgressReportBO(request.eventId(), request.sessionEpoch(), request.sequence(),
                accepted ? request.positionMs() : current.resumePositionMs(), accepted);
    }

    private void validate(ReportProgressDTO request, long durationMs) {
        if (request.positionMs() > durationMs) {
            throw BusinessException.badRequest("播放位置不能超过视频时长");
        }
        long totalReportedMs = 0;
        for (PlayedRangeDTO range : request.playedRanges()) {
            if (range.endMs() <= range.startMs() || range.endMs() > durationMs) {
                throw BusinessException.badRequest("观看区间不合法");
            }
            totalReportedMs += range.endMs() - range.startMs();
        }
        long allowed = request.playbackRate().multiply(java.math.BigDecimal.valueOf(120_000))
                .longValue() + 5_000;
        if (totalReportedMs > allowed) {
            throw BusinessException.badRequest("单次上报的观看区间过大");
        }
    }
}
