package com.knowledge.points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.points.signin.dao.mapper.SignInRecordMapper;
import com.knowledge.points.signin.dao.model.SignInRecordDO;
import com.knowledge.points.signin.service.SignInQueryService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SignInQueryServiceTest {
    @Test
    void rejectsInvalidMonthBeforeDatabaseAccess() {
        var mapper = mock(SignInRecordMapper.class);
        var service = new SignInQueryService();
        ReflectionTestUtils.setField(service, "recordMapper", mapper);
        for (String month : List.of("2026-13", "2026-2", "0000-01", "2026-01 OR 1=1")) {
            assertThatThrownBy(() -> service.month("user-1", month)).isInstanceOf(BusinessException.class);
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void returnsDatabaseDatesAndServerUtcToday() {
        var mapper = mock(SignInRecordMapper.class);
        var record = new SignInRecordDO();
        record.setSignDate(LocalDate.of(2024, 2, 29));
        when(mapper.selectList(any())).thenReturn(List.of(record));
        var service = new SignInQueryService();
        ReflectionTestUtils.setField(service, "recordMapper", mapper);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-09-14T23:30:00Z"), ZoneOffset.UTC));
        var result = service.month("user-1", "2024-02");
        assertThat(result.today()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(result.signedDates()).containsExactly(LocalDate.of(2024, 2, 29));
    }
}
