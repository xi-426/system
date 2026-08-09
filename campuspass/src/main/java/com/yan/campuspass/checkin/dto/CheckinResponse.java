package com.yan.campuspass.checkin.dto;

import java.time.LocalDateTime;

public record CheckinResponse(
        Long activityId,
        Long userId,
        LocalDateTime checkedAt
) {
}
