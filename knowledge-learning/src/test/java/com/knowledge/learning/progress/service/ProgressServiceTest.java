package com.knowledge.learning.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.enums.ChapterStatus;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import com.knowledge.learning.progress.dto.ReportProgressDTO;
import com.knowledge.learning.progress.enums.ProgressEventType;
import com.knowledge.learning.progress.enums.ProgressStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
class ProgressServiceTest {
    @Mock CourseQueryService courseQueryService;
    @Mock EntitlementService entitlementService;
    @Mock PlaybackSessionService sessionService;
    @Mock ProgressEventPublisher eventPublisher;
    @Mock ProgressCacheService cacheService;
    @Mock ProgressQueryService queryService;
    @InjectMocks ProgressService service;
    private final ReportProgressDTO request = new ReportProgressDTO("event", "session", 100, 2,
            ProgressEventType.PAUSE, 20000, List.of(), Instant.EPOCH, BigDecimal.ONE);

    @BeforeEach void prepare() {
        ReflectionTestUtils.setField(service, "clock", Clock.systemUTC());
        when(courseQueryService.requirePublishedVideo("v")).thenReturn(new ChapterBO("ch", "c", "Video", 1,
                "v", "url", 100000, 1, ChapterStatus.PUBLISHED, 0));
        when(queryService.get("u", "v")).thenReturn(new VideoProgressBO("c", "ch", "v", 1, 5000, 5000,
                100000, 0, 0, ProgressStatus.LEARNING, 100, 1, LocalDateTime.now()));
    }

    @Test void confirmedEventRemainsAcceptedWhenCacheWriteFails() {
        when(cacheService.putOptimistic(eq("u"), any())).thenThrow(new DataAccessResourceFailureException("offline"));
        var result = service.report("u", "v", request);
        assertThat(result.accepted()).isTrue();
        assertThat(result.cacheUpdated()).isFalse();
        assertThat(result.resumePositionMs()).isEqualTo(5000);
        var order = inOrder(queryService, eventPublisher, cacheService);
        order.verify(queryService).get("u", "v");
        order.verify(eventPublisher).publish(any());
        order.verify(cacheService).putOptimistic(eq("u"), any());
        order.verifyNoMoreInteractions();
    }

    @Test void publishFailureDoesNotAdvanceCache() {
        doThrow(BusinessException.serviceUnavailable("timeout")).when(eventPublisher).publish(any());
        assertThatThrownBy(() -> service.report("u", "v", request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(cacheService);
    }
}
