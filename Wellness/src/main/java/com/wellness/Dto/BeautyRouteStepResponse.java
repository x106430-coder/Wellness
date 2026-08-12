package com.wellness.Dto;

import java.util.List;

public record BeautyRouteStepResponse(
        int step,
        String category,
        String title,
        String description,
        int estimatedMinutes,
        int estimatedPrice,
        List<String> matchedReasons
) {
}
