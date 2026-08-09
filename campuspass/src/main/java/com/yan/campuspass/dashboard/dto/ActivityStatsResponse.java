package com.yan.campuspass.dashboard.dto;

public record ActivityStatsResponse(
        Long activityId,
        Integer capacity,
        Integer registeredCount,
        long checkedInCount,
        double attendanceRate
) {
}
