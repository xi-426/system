package com.yan.campuspass.auth.service;

import com.yan.campuspass.auth.dto.LoginRequest;
import com.yan.campuspass.auth.dto.LoginResponse;
import com.yan.campuspass.common.exception.UnauthorizedException;
import com.yan.campuspass.security.GeneratedToken;
import com.yan.campuspass.security.JwtService;
import com.yan.campuspass.user.domain.SysUser;
import com.yan.campuspass.user.domain.UserRole;
import com.yan.campuspass.user.domain.UserStatus;
import com.yan.campuspass.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userMapper, passwordEncoder, jwtService);
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreCorrect() {
        SysUser user = organizer();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("CampusPass123!", user.getPasswordHash()))
                .thenReturn(true);
        when(jwtService.generate(user)).thenReturn(
                new GeneratedToken("jwt-token", Instant.parse("2026-08-01T03:00:00Z"))
        );

        LoginResponse response = authService.login(
                new LoginRequest("organizer", "CampusPass123!")
        );

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(1001L);
        assertThat(response.role()).isEqualTo(UserRole.ORGANIZER);
    }

    @Test
    void shouldRejectWrongPassword() {
        SysUser user = organizer();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("organizer", "wrong-password")
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("用户名或密码错误");
    }

    private SysUser organizer() {
        SysUser user = new SysUser();
        user.setId(1001L);
        user.setUsername("organizer");
        user.setPasswordHash("encoded-password");
        user.setDisplayName("活动组织者");
        user.setRole(UserRole.ORGANIZER);
        user.setStatus(UserStatus.ENABLED);
        return user;
    }
}
