package com.knowledge.learning.progress.service;

import com.knowledge.api.learning.dto.PlayedRangeEventDTO;
import com.knowledge.api.learning.dto.VideoProgressReportedEventDTO;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import com.knowledge.learning.progress.converter.ProgressConverter;
import com.knowledge.learning.progress.dao.mapper.ProgressEventInboxMapper;
import com.knowledge.learning.progress.dao.mapper.VideoProgressMapper;
import com.knowledge.learning.progress.dao.mapper.WatchedSegmentMapper;
import com.knowledge.learning.progress.dao.model.ProgressEventInboxDO;
import com.knowledge.learning.progress.dao.model.VideoProgressDO;
import com.knowledge.learning.progress.dao.model.WatchedSegmentDO;
import com.knowledge.learning.progress.enums.ProgressStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressTransactionService {

    static final String CONSUMER_NAME = "learning-progress-v1";
    private static final long SEGMENT_MS = 10_000;

    private final ProgressEventInboxMapper inboxMapper;
    private final VideoProgressMapper progressMapper;
    private final WatchedSegmentMapper segmentMapper;
    private final Clock clock;

    public ProgressTransactionService(ProgressEventInboxMapper inboxMapper, VideoProgressMapper progressMapper,
                                      WatchedSegmentMapper segmentMapper, Clock clock) {
        this.inboxMapper = inboxMapper;
        this.progressMapper = progressMapper;
        this.segmentMapper = segmentMapper;
        this.clock = clock;
    }

    @Transactional
    public VideoProgressBO process(VideoProgressReportedEventDTO event) {
        LocalDateTime now = LocalDateTime.now(clock);
        ProgressEventInboxDO inbox = new ProgressEventInboxDO();
        inbox.setId(UUID.randomUUID().toString());
        inbox.setConsumerName(CONSUMER_NAME);
        inbox.setEventId(event.eventId());
        inbox.setProcessedAt(now);
        if (inboxMapper.insertIgnore(inbox) == 0) {
            return ProgressConverter.toBO(progressMapper.findOne(
                    event.userId(), event.videoId(), event.videoVersion()));
        }

        progressMapper.ensureRow(UUID.randomUUID().toString(), event.userId(), event.courseId(), event.chapterId(),
                event.videoId(), event.videoVersion(), event.durationMs(), now);
        VideoProgressDO progress = progressMapper.findForUpdate(event.userId(), event.videoId(), event.videoVersion());
        saveFullSegments(event, now);
        long watchedSeconds = Math.min(event.durationMs() / 1000,
                segmentMapper.countSegments(event.userId(), event.videoId(), event.videoVersion()) * 10);
        int completionRate = event.durationMs() == 0 ? 0
                : (int) Math.min(10_000, watchedSeconds * 10_000_000L / event.durationMs());
        boolean newer = event.sessionEpoch() > progress.getLastSessionEpoch()
                || (event.sessionEpoch() == progress.getLastSessionEpoch()
                && event.sequence() > progress.getLastSequence());
        if (newer) {
            progress.setResumePositionMs(event.positionMs());
            progress.setLastSessionEpoch(event.sessionEpoch());
            progress.setLastSequence(event.sequence());
            progress.setLastEventId(event.eventId());
        }
        progress.setMaxPositionMs(Math.max(progress.getMaxPositionMs(), event.positionMs()));
        progress.setWatchedSeconds(watchedSeconds);
        progress.setCompletionRate(completionRate);
        progress.setStatus(resolveStatus(completionRate, event.eventType()));
        progress.setVersion(progress.getVersion() + 1);
        progress.setUpdatedAt(now);
        progressMapper.updateById(progress);
        return ProgressConverter.toBO(progress);
    }

    private void saveFullSegments(VideoProgressReportedEventDTO event, LocalDateTime now) {
        for (PlayedRangeEventDTO range : event.playedRanges()) {
            long firstFullSegment = (range.startMs() + SEGMENT_MS - 1) / SEGMENT_MS;
            long lastExclusive = range.endMs() / SEGMENT_MS;
            for (long index = firstFullSegment; index < lastExclusive; index++) {
                WatchedSegmentDO segment = new WatchedSegmentDO();
                segment.setId(UUID.randomUUID().toString());
                segment.setUserId(event.userId());
                segment.setVideoId(event.videoId());
                segment.setVideoVersion(event.videoVersion());
                segment.setSegmentIndex(Math.toIntExact(index));
                segment.setCreatedAt(now);
                segmentMapper.insertIgnore(segment);
            }
        }
    }

    private ProgressStatus resolveStatus(int completionRate, String eventType) {
        if (completionRate >= 9000 || ("ENDED".equals(eventType) && completionRate >= 8000)) {
            return ProgressStatus.COMPLETED;
        }
        return ProgressStatus.LEARNING;
    }
}
