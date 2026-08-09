package com.yan.campuspass.activity.controller;

import com.yan.campuspass.activity.dto.ActivityResponse;
import com.yan.campuspass.activity.dto.CreateActivityRequest;
import com.yan.campuspass.activity.service.ActivityService;
import com.yan.campuspass.common.api.ApiResponse;
import com.yan.campuspass.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ApiResponse<ActivityResponse> createDraft(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateActivityRequest request) {
        return ApiResponse.ok(
                activityService.createDraft(currentUser.userId(), request)
        );
    }

    @PostMapping("/{activityId}/publish")
    public ApiResponse<ActivityResponse> publish(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long activityId) {
        return ApiResponse.ok(
                activityService.publish(currentUser.userId(), activityId)
        );
    }

    @GetMapping("/{activityId}")
    public ApiResponse<ActivityResponse> detail(@PathVariable Long activityId) {
        return ApiResponse.ok(activityService.getPublishedActivity(activityId));
    }

    @GetMapping
    public ApiResponse<List<ActivityResponse>> list() {
        return ApiResponse.ok(activityService.listPublishedActivities());
    }

    @GetMapping("/mine")
    public ApiResponse<List<ActivityResponse>> mine(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(
                activityService.listOrganizerActivities(currentUser.userId())
        );
    }
}
