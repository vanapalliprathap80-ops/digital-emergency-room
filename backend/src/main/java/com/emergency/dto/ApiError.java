package com.emergency.dto;

public record ApiError(
        int status,
        String error,
        String message,
        String path
) {}
