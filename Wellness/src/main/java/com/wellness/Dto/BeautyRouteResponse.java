package com.wellness.Dto;

import java.time.LocalDate;
import java.util.List;

public record BeautyRouteResponse(
        LocalDate preferredDate,
        String subscriptionPlan,
        int estimatedTotalMinutes,
        int estimatedTotalPrice,
        List<BeautyRouteStepResponse> routeSteps,
        boolean premiumDetailLocked,
        String premiumMessage,
        String reservationStatus,
        String reservationMessage
) {
}
