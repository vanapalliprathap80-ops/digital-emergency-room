package com.emergency.dto;

public record ServiceStatusDto(
        String service,
        String status,
        String description
) {}
