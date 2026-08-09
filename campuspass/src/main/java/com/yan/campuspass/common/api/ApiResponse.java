package com.yan.campuspass.common.api;

public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    public static ApiResponse<Void> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
