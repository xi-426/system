package com.yan.campuspass.dashboard.controller;

import com.yan.campuspass.common.api.ApiResponse;
import com.yan.campuspass.dashboard.dto.ActivityStatsResponse;
import com.yan.campuspass.dashboard.dto.NotificationResponse;
import com.yan.campuspass.dashboard.service.DashboardService;
import com.yan.campuspass.registration.domain.ActivityRegistration;
import com.yan.campuspass.security.CurrentUser;
import com.yan.campuspass.waitlist.domain.ActivityWaitlist;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/me/registrations")
    public ApiResponse<List<ActivityRegistration>> registrations(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(
                dashboardService.registrations(currentUser.userId())
        );
    }

    @GetMapping("/api/me/waitlists")
    public ApiResponse<List<ActivityWaitlist>> waitlists(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(
                dashboardService.waitlists(currentUser.userId())
        );
    }

    @GetMapping("/api/me/notifications")
    public ApiResponse<List<NotificationResponse>> notifications(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(
                dashboardService.notifications(currentUser.userId())
        );
    }

    @GetMapping("/api/activities/{activityId}/stats")
    public ApiResponse<ActivityStatsResponse> stats(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long activityId) {
        return ApiResponse.ok(
                dashboardService.stats(currentUser.userId(), activityId)
        );
    }
}
