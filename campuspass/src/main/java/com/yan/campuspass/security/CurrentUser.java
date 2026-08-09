package com.yan.campuspass.security;

import com.yan.campuspass.user.domain.UserRole;

import java.time.Instant;

public record CurrentUser(
        Long userId,
        String username,
        String displayName,
        UserRole role,
        String tokenId,
        Instant expiresAt
) {
}
