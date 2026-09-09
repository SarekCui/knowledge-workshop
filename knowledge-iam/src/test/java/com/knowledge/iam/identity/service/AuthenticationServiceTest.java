package com.knowledge.iam.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.api.common.ErrorCode;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.identity.bo.AccessTokenBO;
import com.knowledge.iam.identity.bo.AuthenticatedUserBO;
import com.knowledge.iam.identity.bo.RefreshTokenBO;
import com.knowledge.iam.identity.bo.TokenPairBO;
import com.knowledge.iam.identity.dao.mapper.UserAccountMapper;
import com.knowledge.iam.identity.dao.mapper.UserRoleMapper;
import com.knowledge.iam.identity.dao.model.UserAccountDO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthenticationServiceTest {

    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuthenticationService service = new AuthenticationService(
            userAccountMapper, userRoleMapper, passwordEncoder, jwtTokenService, refreshTokenService);

    @Test
    void issuesTokenForEnabledAccountWithValidPassword() {
        UserAccountDO account = account("ENABLED");
        when(userAccountMapper.selectByUsername("demo")).thenReturn(account);
        when(passwordEncoder.matches("Knowledge@123", account.getPasswordHash())).thenReturn(true);
        when(userRoleMapper.selectRoleCodesByUserId("user-demo")).thenReturn(List.of("LEARNER"));
        when(jwtTokenService.issue(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AccessTokenBO("signed-token", 1800));
        when(refreshTokenService.issue("user-demo"))
                .thenReturn(new RefreshTokenBO("user-demo", "refresh-token", 2592000));

        TokenPairBO result = service.login(" demo ", "Knowledge@123");

        assertThat(result.accessToken()).isEqualTo("signed-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        ArgumentCaptor<AuthenticatedUserBO> userCaptor = ArgumentCaptor.forClass(AuthenticatedUserBO.class);
        verify(jwtTokenService).issue(userCaptor.capture());
        assertThat(userCaptor.getValue().roles()).containsExactly("LEARNER");
    }

    @Test
    void hidesWhetherUsernameOrPasswordWasWrong() {
        when(userAccountMapper.selectByUsername("missing")).thenReturn(null);
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.eq("Knowledge@123"),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.login("missing", "Knowledge@123"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED))
                .hasMessage("用户名或密码错误");
        verify(jwtTokenService, never()).issue(org.mockito.ArgumentMatchers.any());
        verify(refreshTokenService, never()).issue(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsDisabledAccountWithoutIssuingToken() {
        UserAccountDO account = account("DISABLED");
        when(userAccountMapper.selectByUsername("demo")).thenReturn(account);

        assertThatThrownBy(() -> service.login("demo", "Knowledge@123"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED))
                .hasMessage("用户名或密码错误");
        verify(passwordEncoder).matches("Knowledge@123", "bcrypt-hash");
        verify(jwtTokenService, never()).issue(org.mockito.ArgumentMatchers.any());
        verify(refreshTokenService, never()).issue(org.mockito.ArgumentMatchers.anyString());
    }

    private UserAccountDO account(String status) {
        UserAccountDO account = new UserAccountDO();
        account.setId("user-demo");
        account.setUsername("demo");
        account.setPasswordHash("bcrypt-hash");
        account.setStatus(status);
        return account;
    }
}
