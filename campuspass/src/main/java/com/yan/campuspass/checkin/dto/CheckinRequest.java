package com.yan.campuspass.checkin.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckinRequest(
        @NotBlank(message = "签到 Token 不能为空")
        String token
) {
}
