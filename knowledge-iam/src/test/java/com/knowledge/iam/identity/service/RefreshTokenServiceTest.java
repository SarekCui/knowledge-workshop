package com.knowledge.iam.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.iam.identity.bo.RefreshTokenBO;
import com.knowledge.iam.identity.bo.RefreshTokenRotationBO;
import com.knowledge.iam.identity.dao.mapper.RefreshTokenMapper;
import com.knowledge.iam.identity.dao.model.RefreshTokenDO;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenServiceTest {

    private final RefreshTokenMapper mapper = mock(RefreshTokenMapper.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"), ZoneOffset.UTC);
    private final RefreshTokenService service = new RefreshTokenService(
            mapper, clock, new SecureRandom(), Duration.ofDays(30));

    @Test
    void rotatesActiveTokenAndLinksReplacement() {
        RefreshTokenDO current = token("token-1", "user-1", "family-1", "ACTIVE",
                LocalDateTime.of(2026, 10, 1, 0, 0));
        when(mapper.selectByTokenHashForUpdate(any())).thenReturn(current);
        when(mapper.markRotated(any(), any(), any())).thenReturn(1);

        RefreshTokenRotationBO rotation = service.rotate("old-refresh-token");
        RefreshTokenBO result = rotation.token();

        assertThat(rotation.accepted()).isTrue();
        assertThat(result.userId()).isEqualTo("user-1");
        assertThat(result.token()).isNotBlank().isNotEqualTo("old-refresh-token");
        ArgumentCaptor<RefreshTokenDO> inserted = ArgumentCaptor.forClass(RefreshTokenDO.class);
        verify(mapper).insert(inserted.capture());
        assertThat(inserted.getValue().getFamilyId()).isEqualTo("family-1");
        verify(mapper).markRotated("token-1", inserted.getValue().getId(),
                LocalDateTime.of(2026, 9, 8, 0, 0));
    }

    @Test
    void revokesWholeFamilyWhenRotatedTokenIsReused() {
        RefreshTokenDO reused = token("token-1", "user-1", "family-1", "ROTATED",
                LocalDateTime.of(2026, 10, 1, 0, 0));
        when(mapper.selectByTokenHashForUpdate(any())).thenReturn(reused);

        RefreshTokenRotationBO result = service.rotate("reused-refresh-token");

        assertThat(result.accepted()).isFalse();
        assertThat(result.token()).isNull();
        verify(mapper).revokeFamily("family-1", LocalDateTime.of(2026, 9, 8, 0, 0));
    }

    private RefreshTokenDO token(
            String id, String userId, String familyId, String status, LocalDateTime expiresAt) {
        RefreshTokenDO token = new RefreshTokenDO();
        token.setId(id);
        token.setUserId(userId);
        token.setFamilyId(familyId);
        token.setStatus(status);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
