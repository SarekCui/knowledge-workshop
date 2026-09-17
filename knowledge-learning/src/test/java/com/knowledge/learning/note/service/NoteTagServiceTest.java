package com.knowledge.learning.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.dao.mapper.NoteTagMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NoteTagServiceTest {

    @Test
    void normalizesUnicodeWhitespaceAndCaseInsensitiveDuplicates() {
        var service = service(mock(NoteTagMapper.class));
        assertThat(service.normalize(List.of("  Spring   Boot ", "ＳＱＬ", "spring boot")))
                .containsExactly("Spring Boot", "SQL");
        assertThat(service.normalizeFilter(" ReDiS ")).isEqualTo("redis");
    }

    @Test
    void rejectsMoreThanFiveOrInvalidTags() {
        var service = service(mock(NoteTagMapper.class));
        assertThatThrownBy(() -> service.normalize(List.of("1", "2", "3", "4", "5", "6")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.normalize(List.of("bad\u0000tag")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void replacementDeletesOldRowsBeforeWritingOrderedTags() {
        var mapper = mock(NoteTagMapper.class);
        var service = service(mapper);
        service.replace("note-1", List.of("Java", "Redis"));
        var ordered = inOrder(mapper);
        ordered.verify(mapper).deleteByNoteId("note-1");
        ordered.verify(mapper).insert("note-1", "java", "Java", 0,
                java.time.LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC));
        ordered.verify(mapper).insert("note-1", "redis", "Redis", 1,
                java.time.LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC));
    }

    private NoteTagService service(NoteTagMapper mapper) {
        var service = new NoteTagService();
        ReflectionTestUtils.setField(service, "noteTagMapper", mapper);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        return service;
    }
}
