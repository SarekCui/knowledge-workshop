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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProgressQueryService {

    private final VideoProgressMapper progressMapper;
    private final ProgressCacheService cacheService;
    private final CourseQueryService courseQueryService;
    private final EntitlementService entitlementService;
    private final Clock clock;

    public ProgressQueryService(VideoProgressMapper progressMapper, ProgressCacheService cacheService,
                                CourseQueryService courseQueryService, EntitlementService entitlementService,
                                Clock clock) {
        this.progressMapper = progressMapper;
        this.cacheService = cacheService;
        this.courseQueryService = courseQueryService;
        this.entitlementService = entitlementService;
        this.clock = clock;
    }

    public VideoProgressBO get(String userId, String videoId) {
        ChapterBO chapter = courseQueryService.requirePublishedVideo(videoId);
        entitlementService.requireActive(userId, chapter.courseId());
        VideoProgressBO cached = cacheService.get(userId, videoId, chapter.videoVersion());
        if (cached != null) {
            return cached;
        }
        VideoProgressDO stored = progressMapper.findOne(userId, videoId, chapter.videoVersion());
        if (stored != null) {
            VideoProgressBO progress = ProgressConverter.toBO(stored);
            cacheService.put(userId, progress);
            return progress;
        }
        return new VideoProgressBO(chapter.courseId(), chapter.id(), chapter.videoId(), chapter.videoVersion(),
                0, 0, chapter.videoDurationMs(), 0, 0, ProgressStatus.NOT_STARTED, 0, 0,
                LocalDateTime.now(clock));
    }

    public List<VideoProgressBO> recent(String userId, int limit) {
        Set<String> members = cacheService.recentMembers(userId, Math.min(Math.max(limit, 1), 50));
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
