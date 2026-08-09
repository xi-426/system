package com.yan.campuspass.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public TokenBlacklistService(
            StringRedisTemplate redisTemplate,
            Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    public void blacklist(CurrentUser currentUser) {
        Duration remainingTtl = Duration.between(
                clock.instant(),
                currentUser.expiresAt()
        );
        if (remainingTtl.isZero() || remainingTtl.isNegative()) {
            return;
        }

        redisTemplate.opsForValue().set(
                key(currentUser.tokenId()),
                "1",
                remainingTtl
        );
    }

    public boolean isBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key(tokenId))
        );
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}
