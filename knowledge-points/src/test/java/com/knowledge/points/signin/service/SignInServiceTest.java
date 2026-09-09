package com.knowledge.points.signin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.points.signin.dao.mapper.SignInRecordMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SignInServiceTest {

    @Test
    void clearsNewBitmapWhenDatabaseTransactionFails() {
        SignInRecordMapper mapper = mock(SignInRecordMapper.class);
        SignInBitmapService bitmap = mock(SignInBitmapService.class);
        SignInTransactionService transaction = mock(SignInTransactionService.class);
        LocalDate date = LocalDate.of(2026, 9, 3);
        when(mapper.selectOne(any())).thenReturn(null);
        when(bitmap.markSigned("u1", date)).thenReturn(false);
        when(bitmap.continuousDays("u1", date)).thenReturn(3);
        when(transaction.record("u1", date, 3, 14)).thenThrow(new IllegalStateException("db down"));

        SignInService service = new SignInService(mapper, bitmap, new SignInRewardPolicy(), transaction);

        assertThatThrownBy(() -> service.sign("u1", date)).isInstanceOf(IllegalStateException.class);
        verify(bitmap).clear("u1", date);
    }
}
