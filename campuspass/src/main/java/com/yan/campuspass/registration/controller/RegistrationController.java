package com.yan.campuspass.registration.controller;

import com.yan.campuspass.common.api.ApiResponse;
import com.yan.campuspass.registration.dto.ReservationResultResponse;
import com.yan.campuspass.registration.dto.RegistrationResponse;
import com.yan.campuspass.registration.service.RegistrationService;
import com.yan.campuspass.security.CurrentUser;
import com.yan.campuspass.waitlist.dto.WaitlistResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities/{activityId}/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ApiResponse<ReservationResultResponse> reserve(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long activityId) {
        return ApiResponse.ok(
                registrationService.reserve(currentUser.userId(), activityId)
        );
    }

    @DeleteMapping("/waitlist/me")
    public ApiResponse<WaitlistResponse> cancelWaitlist(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long activityId) {
        return ApiResponse.ok(
                registrationService.cancelWaitlist(
                        currentUser.userId(),
                        activityId
                )
        );
    }

    @DeleteMapping("/me")
    public ApiResponse<RegistrationResponse> cancel(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long activityId) {
        return ApiResponse.ok(
                registrationService.cancel(currentUser.userId(), activityId)
        );
    }
}
