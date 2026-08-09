package com.yan.campuspass.auth.dto;

import com.yan.campuspass.user.domain.SysUser;
import com.yan.campuspass.user.domain.UserRole;

import java.time.Instant;

public record LoginResponse(
        String tokenType,
        String accessToken,
        Instant expiresAt,
        Long userId,
        String username,
        String displayName,
        UserRole role
) {
    public static LoginResponse from(SysUser user,
                                     String accessToken,
                                     Instant expiresAt) {
        return new LoginResponse(
                "Bearer",
                accessToken,
                expiresAt,
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole()
        );
    }
}
