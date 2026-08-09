package com.yan.campuspass.security;

import java.time.Instant;

public record GeneratedToken(
        String value,
        Instant expiresAt
) {
}
