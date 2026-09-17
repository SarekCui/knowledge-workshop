package com.knowledge.learning.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.note.dao.mapper.NoteMapper;
import com.knowledge.learning.note.dao.model.NoteDO;
import com.knowledge.learning.note.dto.CreateNoteDTO;
import com.knowledge.learning.note.enums.NoteStatus;
import com.knowledge.learning.note.enums.NoteSort;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NoteSharingTest {
    @Test
    void independentCreationNeedsNeitherCourseNorEntitlement() {
        var mapper = mock(NoteMapper.class);
        when(mapper.insert(any(NoteDO.class))).thenReturn(1);
        var courses = mock(CourseQueryService.class);
        var entitlements = mock(EntitlementService.class);
        var tags = mock(NoteTagService.class);
        var images = mock(NoteImageService.class);
        when(tags.normalize(any())).thenReturn(java.util.List.of());
        var service = new NoteService();
        ReflectionTestUtils.setField(service, "noteMapper", mapper);
        ReflectionTestUtils.setField(service, "courseQueryService", courses);
        ReflectionTestUtils.setField(service, "entitlementService", entitlements);
        ReflectionTestUtils.setField(service, "noteTagService", tags);
        ReflectionTestUtils.setField(service, "noteImageService", images);
        ReflectionTestUtils.setField(service, "clock", Clock.systemUTC());
        var result = service.create("author", new CreateNoteDTO("request-1", null, null, "知识分享", "内容", null));
        assertThat(result.courseId()).isNull();
        assertThat(result.status()).isEqualTo(NoteStatus.DRAFT);
        verifyNoInteractions(courses, entitlements);
    }

    @Test
    void publicDetailNeverFallsBackToPrivateOrDeletedContent() {
        var mapper = mock(NoteMapper.class);
        var query = new NoteQueryService();
        ReflectionTestUtils.setField(query, "noteMapper", mapper);
        var note = new NoteDO();
        note.setDeleted(0);
        when(mapper.selectById("note-1")).thenReturn(note);
        for (var status : new NoteStatus[]{NoteStatus.DRAFT, NoteStatus.PRIVATE}) {
            note.setStatus(status);
            assertThatThrownBy(() -> query.getPublic("note-1")).isInstanceOf(BusinessException.class);
        }
        note.setStatus(NoteStatus.PUBLIC);
        note.setDeleted(1);
        assertThatThrownBy(() -> query.getPublic("note-1")).isInstanceOf(BusinessException.class);
    }

    @Test
    void publicPaginationRejectsUnboundedRequestsBeforeDatabase() {
        var mapper = mock(NoteMapper.class);
        var query = new NoteQueryService();
        ReflectionTestUtils.setField(query, "noteMapper", mapper);
        assertThatThrownBy(() -> query.publicPage(null, null, NoteSort.LATEST, 1001, 20))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> query.mine("author", null, null, 1, 51)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(mapper);
    }
}
