package com.yan.campuspass.activity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateActivityRequest(
        @NotBlank(message = "活动标题不能为空")
        @Size(max = 100, message = "活动标题不能超过100个字符")
        String title,

        @NotBlank(message = "活动分类不能为空")
        @Size(max = 30, message = "活动分类不能超过30个字符")
        String category,

        @NotBlank(message = "活动地点不能为空")
        @Size(max = 200, message = "活动地点不能超过200个字符")
        String location,

        @Size(max = 2000, message = "活动介绍不能超过2000个字符")
        String description,

        @NotNull(message = "活动容量不能为空")
        @Min(value = 1, message = "活动容量至少为1")
        @Max(value = 10000, message = "活动容量不能超过10000")
        Integer capacity,

        @NotNull(message = "报名开始时间不能为空")
        LocalDateTime registrationStartTime,

        @NotNull(message = "报名结束时间不能为空")
        LocalDateTime registrationEndTime,

        @NotNull(message = "活动开始时间不能为空")
        LocalDateTime activityStartTime,

        @NotNull(message = "活动结束时间不能为空")
        LocalDateTime activityEndTime
) {
}
