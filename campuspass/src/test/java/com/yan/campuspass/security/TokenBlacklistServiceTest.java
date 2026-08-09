package com.yan.campuspass.security;

import com.yan.campuspass.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TokenBlacklistService blacklistService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-31T01:00:00Z"),
                ZoneOffset.UTC
        );
        blacklistService = new TokenBlacklistService(
                redisTemplate,
                clock
        );
    }

    @Test
    void shouldBlacklistTokenForItsRemainingLifetime() {
        CurrentUser currentUser = new CurrentUser(
                2L,
                "student",
                "学生用户",
                UserRole.STUDENT,
                "token-id-1",
                Instant.parse("2026-07-31T02:30:00Z")
        );

        blacklistService.blacklist(currentUser);

        verify(valueOperations).set(
                "jwt:blacklist:token-id-1",
                "1",
                Duration.ofMinutes(90)
        );
    }

    @Test
    void shouldReadBlacklistStateFromRedis() {
        when(redisTemplate.hasKey("jwt:blacklist:token-id-1"))
                .thenReturn(true);

        boolean blacklisted =
                blacklistService.isBlacklisted("token-id-1");

        assertThat(blacklisted).isTrue();
    }
}
