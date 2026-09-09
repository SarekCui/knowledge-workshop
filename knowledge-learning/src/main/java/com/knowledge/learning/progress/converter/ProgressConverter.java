package com.knowledge.learning.progress.converter;

import com.knowledge.learning.progress.bo.PlaybackSessionBO;
import com.knowledge.learning.progress.bo.ProgressReportBO;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import com.knowledge.learning.progress.dao.model.VideoProgressDO;
import com.knowledge.learning.progress.vo.PlaybackSessionVO;
import com.knowledge.learning.progress.vo.ProgressReportVO;
import com.knowledge.learning.progress.vo.VideoProgressVO;

public final class ProgressConverter {

    private ProgressConverter() {
    }

    public static VideoProgressBO toBO(VideoProgressDO source) {
        return new VideoProgressBO(source.getCourseId(), source.getChapterId(), source.getVideoId(),
                source.getVideoVersion(), source.getResumePositionMs(), source.getMaxPositionMs(),
                source.getDurationMs(), source.getWatchedSeconds(), source.getCompletionRate(), source.getStatus(),
                source.getLastSessionEpoch(), source.getLastSequence(), source.getUpdatedAt());
    }

    public static VideoProgressVO toVO(VideoProgressBO source) {
        return new VideoProgressVO(source.courseId(), source.chapterId(), source.videoId(), source.videoVersion(),
                source.resumePositionMs(), source.maxPositionMs(), source.durationMs(), source.watchedSeconds(),
                source.completionRate(), source.status().name(), source.sessionEpoch(), source.sequence(),
                source.updatedAt());
    }

    public static PlaybackSessionVO toVO(PlaybackSessionBO source) {
        return new PlaybackSessionVO(source.sessionId(), source.sessionEpoch(), source.videoId(),
                source.videoVersion(), source.resumePositionMs());
    }

    public static ProgressReportVO toVO(ProgressReportBO source) {
        return new ProgressReportVO(source.eventId(), source.sessionEpoch(), source.sequence(),
                source.resumePositionMs(), source.accepted());
    }
}
