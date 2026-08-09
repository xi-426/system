package com.yan.campuspass.checkin.dto;

import java.time.Instant;

public record CheckinTokenResponse(
        String token,
        Instant expiresAt
) {
}
