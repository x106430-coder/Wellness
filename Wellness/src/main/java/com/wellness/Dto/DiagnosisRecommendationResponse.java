package com.wellness.Dto;

public record DiagnosisRecommendationResponse(
        String code,
        String title,
        String reason,
        int priority
) {
}
