package com.emergency.dto;

public record EvaluationResultResponse(
        String incidentId,
        boolean serviceCorrect,
        boolean componentCorrect,
        boolean failureTypeCorrect,
        boolean severityCorrect,
        boolean pinpointAccurate
) {}
