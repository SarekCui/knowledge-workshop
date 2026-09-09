package com.knowledge.iam.identity.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.identity.bo.RefreshTokenBO;
import com.knowledge.iam.identity.bo.RefreshTokenRotationBO;
import com.knowledge.iam.identity.dao.mapper.RefreshTokenMapper;
import com.knowledge.iam.identity.dao.model.RefreshTokenDO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final String ACTIVE = "ACTIVE";

    private final RefreshTokenMapper refreshTokenMapper;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(
            RefreshTokenMapper refreshTokenMapper,
            Clock clock,
            SecureRandom secureRandom,
            @Value("${knowledge.security.jwt.refresh-token-ttl:30d}") Duration refreshTokenTtl) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.clock = clock;
        this.secureRandom = secureRandom;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public RefreshTokenBO issue(String userId) {
        return create(userId, UUID.randomUUID().toString()).token();
    }

    @Transactional
    public RefreshTokenRotationBO rotate(String rawToken) {
        LocalDateTime now = now();
        RefreshTokenDO current = refreshTokenMapper.selectByTokenHashForUpdate(hash(rawToken));
        if (current == null) {
            return RefreshTokenRotationBO.rejected();
        }
        if (!ACTIVE.equals(current.getStatus())) {
            refreshTokenMapper.revokeFamily(current.getFamilyId(), now);
            return RefreshTokenRotationBO.rejected();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            refreshTokenMapper.revokeFamily(current.getFamilyId(), now);
            return RefreshTokenRotationBO.rejected();
        }

        IssuedRefreshToken replacement = create(current.getUserId(), current.getFamilyId());
        int changed = refreshTokenMapper.markRotated(current.getId(), replacement.data().getId(), now);
        if (changed != 1) {
            throw BusinessException.conflict("刷新令牌状态已变化");
        }
        return RefreshTokenRotationBO.accepted(replacement.token());
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshTokenDO token = refreshTokenMapper.selectByTokenHashForUpdate(hash(rawToken));
        if (token != null) {
            refreshTokenMapper.revokeFamily(token.getFamilyId(), now());
        }
    }

    private IssuedRefreshToken create(String userId, String familyId) {
        String rawToken = generateToken();
        LocalDateTime createdAt = now();
        RefreshTokenDO token = new RefreshTokenDO();
        token.setId(UUID.randomUUID().toString());
        token.setUserId(userId);
        token.setFamilyId(familyId);
        token.setTokenHash(hash(rawToken));
        token.setStatus(ACTIVE);
        token.setCreatedAt(createdAt);
        token.setExpiresAt(createdAt.plus(refreshTokenTtl));
        refreshTokenMapper.insert(token);
        return new IssuedRefreshToken(
                token,
                new RefreshTokenBO(userId, rawToken, refreshTokenTtl.toSeconds()));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private record IssuedRefreshToken(RefreshTokenDO data, RefreshTokenBO token) {
    }
}
