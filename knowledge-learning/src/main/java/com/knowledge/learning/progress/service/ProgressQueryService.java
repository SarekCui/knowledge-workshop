package com.knowledge.learning.progress.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import com.knowledge.learning.progress.converter.ProgressConverter;
import com.knowledge.learning.progress.dao.mapper.VideoProgressMapper;
import com.knowledge.learning.progress.dao.model.VideoProgressDO;
import com.knowledge.learning.progress.enums.ProgressStatus;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProgressQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressQueryService.class);
    @Resource
    private VideoProgressMapper progressMapper;
    @Resource
    private ProgressCacheService cacheService;
    @Resource
    private CourseQueryService courseQueryService;
    @Resource
    private EntitlementService entitlementService;
    @Resource
    private Clock clock;

    public VideoProgressBO get(String userId, String videoId) {
        ChapterBO chapter = courseQueryService.requirePublishedVideo(videoId);
        entitlementService.requireActive(userId, chapter.courseId());
        VideoProgressBO cached = null;
        try {
            cached = cacheService.get(userId, videoId, chapter.videoVersion());
        } catch (DataAccessException | IllegalArgumentException | DateTimeException cacheFailure) {
            LOGGER.warn("event=progress_cache_read_failed videoId={} message=断点读取回源数据库", videoId, cacheFailure);
        }
        if (cached != null) {
            return cached;
        }
        VideoProgressDO stored = progressMapper.findOne(userId, videoId, chapter.videoVersion());
        if (stored != null) {
            VideoProgressBO progress = ProgressConverter.toBO(stored);
            try {
                cacheService.put(userId, progress);
            } catch (DataAccessException cacheFailure) {
                LOGGER.warn("event=progress_cache_fill_failed videoId={} message=缓存回填失败，返回已落库断点", videoId, cacheFailure);
            }
            return progress;
        }
        return new VideoProgressBO(chapter.courseId(), chapter.id(), chapter.videoId(), chapter.videoVersion(),
                0, 0, chapter.videoDurationMs(), 0, 0, ProgressStatus.NOT_STARTED, 0, 0,
                LocalDateTime.now(clock));
    }

    public List<VideoProgressBO> recent(String userId, int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 50);
        Set<String> members;
        try {
            members = cacheService.recentMembers(userId, boundedLimit);
        } catch (DataAccessException cacheFailure) {
            LOGGER.warn("event=recent_progress_cache_failed message=最近学习列表回源数据库", cacheFailure);
            members = null;
        }
        if (members == null || members.isEmpty()) {
            return progressMapper.findRecentAvailable(userId, LocalDateTime.now(clock), boundedLimit).stream()
                    .map(ProgressConverter::toBO).toList();
        }
        List<VideoProgressBO> result = new ArrayList<>();
        for (String member : members) {
            int separator = member.lastIndexOf(':');
            if (separator > 0) {
                try {
                    result.add(get(userId, member.substring(0, separator)));
                } catch (BusinessException ignoredUnavailableItem) {
                    // A removed video or expired entitlement is omitted from Continue Watching.
                }
            }
        }
        return result;
    }

    public boolean hasProgressForChapter(String chapterId) {
        return progressMapper.countByChapter(chapterId) > 0;
    }
}
