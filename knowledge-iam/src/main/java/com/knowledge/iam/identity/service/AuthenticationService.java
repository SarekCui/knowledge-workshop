package com.knowledge.iam.identity.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.identity.bo.AccessTokenBO;
import com.knowledge.iam.identity.bo.AuthenticatedUserBO;
import com.knowledge.iam.identity.bo.RefreshTokenBO;
import com.knowledge.iam.identity.bo.RefreshTokenRotationBO;
import com.knowledge.iam.identity.bo.TokenPairBO;
import com.knowledge.iam.identity.dao.mapper.UserAccountMapper;
import com.knowledge.iam.identity.dao.mapper.UserRoleMapper;
import com.knowledge.iam.identity.dao.model.UserAccountDO;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final String ENABLED = "ENABLED";
    private static final String DUMMY_PASSWORD_HASH =
            "$2y$10$qN7NO4fUsdqGctuBbL1cmunHxshfCOIOnou/dLnAAzkTS7swkE.ge";

    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationService(
            UserAccountMapper userAccountMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public TokenPairBO login(String username, String password) {
        UserAccountDO account = userAccountMapper.selectByUsername(username.trim());
        String passwordHash = account == null ? DUMMY_PASSWORD_HASH : account.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(password, passwordHash);
        if (account == null || !ENABLED.equals(account.getStatus()) || !passwordMatches) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        AuthenticatedUserBO user = authenticatedUser(account);
        return tokenPair(jwtTokenService.issue(user), refreshTokenService.issue(user.userId()));
    }

    public TokenPairBO refresh(String refreshToken) {
        RefreshTokenRotationBO rotation = refreshTokenService.rotate(refreshToken);
        if (!rotation.accepted()) {
            throw BusinessException.unauthorized("刷新令牌无效或已失效");
        }
        RefreshTokenBO rotatedToken = rotation.token();
        UserAccountDO account = userAccountMapper.selectById(rotatedToken.userId());
        if (account == null || !ENABLED.equals(account.getStatus())) {
            refreshTokenService.revoke(rotatedToken.token());
            throw BusinessException.unauthorized("刷新令牌无效或已失效");
        }
        return tokenPair(jwtTokenService.issue(authenticatedUser(account)), rotatedToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthenticatedUserBO authenticatedUser(UserAccountDO account) {
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(account.getId());
        return new AuthenticatedUserBO(account.getId(), account.getUsername(), roles);
    }

    private TokenPairBO tokenPair(AccessTokenBO accessToken, RefreshTokenBO refreshToken) {
        return new TokenPairBO(
                accessToken.accessToken(),
                accessToken.expiresInSeconds(),
                refreshToken.token(),
                refreshToken.expiresInSeconds());
    }
}
