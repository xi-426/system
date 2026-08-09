package com.yan.campuspass.checkin.controller;

import com.yan.campuspass.checkin.dto.CheckinRequest;
import com.yan.campuspass.checkin.dto.CheckinResponse;
import com.yan.campuspass.checkin.dto.CheckinTokenResponse;
import com.yan.campuspass.checkin.service.CheckinService;
import com.yan.campuspass.common.api.ApiResponse;
import com.yan.campuspass.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/api/activities/{activityId}/checkin-token")
    public ApiResponse<CheckinTokenResponse> generateToken(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long activityId) {
        return ApiResponse.ok(
                checkinService.generateToken(currentUser.userId(), activityId)
        );
    }

    @PostMapping("/api/checkins")
    public ApiResponse<CheckinResponse> checkIn(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CheckinRequest request) {
        return ApiResponse.ok(
                checkinService.checkIn(currentUser.userId(), request.token())
        );
    }
}
