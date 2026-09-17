package com.knowledge.learning.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.enums.ChapterStatus;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.progress.dao.mapper.VideoProgressMapper;
import com.knowledge.learning.progress.dao.model.VideoProgressDO;
import com.knowledge.learning.progress.enums.ProgressStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProgressQueryServiceTest {
    @Mock VideoProgressMapper progressMapper;
    @Mock ProgressCacheService cacheService;
    @Mock CourseQueryService courseQueryService;
    @Mock EntitlementService entitlementService;
    @InjectMocks ProgressQueryService queryService;

    @BeforeEach void prepare() {
        ReflectionTestUtils.setField(queryService, "clock", Clock.systemUTC());
    }

    private VideoProgressDO stored() {
        VideoProgressDO p = new VideoProgressDO();
        p.setCourseId("c"); p.setChapterId("ch"); p.setVideoId("v"); p.setVideoVersion(1);
        p.setResumePositionMs(45000L); p.setMaxPositionMs(45000L); p.setDurationMs(100000L);
        p.setWatchedSeconds(40L); p.setCompletionRate(4000); p.setStatus(ProgressStatus.LEARNING);
        p.setLastSessionEpoch(100L); p.setLastSequence(1L); p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private void availableVideo() {
        when(courseQueryService.requirePublishedVideo("v")).thenReturn(new ChapterBO("ch", "c", "Video", 1,
                "v", "url", 100000, 1, ChapterStatus.PUBLISHED, 0));
        when(progressMapper.findOne("u", "v", 1)).thenReturn(stored());
    }

    @Test void redisReadAndFillFailureStillReturnsStoredProgress() {
        availableVideo();
        when(cacheService.get("u", "v", 1)).thenThrow(new DataAccessResourceFailureException("offline"));
        doThrow(new DataAccessResourceFailureException("offline")).when(cacheService).put(eq("u"), any());
        assertThat(queryService.get("u", "v").resumePositionMs()).isEqualTo(45000);
    }

    @Test void malformedSnapshotFallsBackToDatabase() {
        availableVideo();
        when(cacheService.get("u", "v", 1)).thenThrow(new IllegalArgumentException("malformed"));
        assertThat(queryService.get("u", "v").resumePositionMs()).isEqualTo(45000);
    }

    @Test void recentListFallsBackWithBoundedQuery() {
        when(cacheService.recentMembers("u", 50)).thenThrow(new DataAccessResourceFailureException("offline"));
        when(progressMapper.findRecentAvailable(eq("u"), any(), eq(50))).thenReturn(List.of(stored()));
        assertThat(queryService.recent("u", 100).get(0).resumePositionMs()).isEqualTo(45000);
    }
}
