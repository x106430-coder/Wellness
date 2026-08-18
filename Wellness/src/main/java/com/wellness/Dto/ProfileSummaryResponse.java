package com.wellness.Dto;

import java.time.LocalDateTime;

public record ProfileSummaryResponse(
        Long userId,
        String email,
        String nickname,
        String gender,
        Integer age,
        String subscriptionPlan,
        LocalDateTime createdAt,
        int lookbackDays,
        int analyzedDays,
        String aiProfileComment,
        String generatedBy,
        String subscriptionFeatureStatus,
        String settingsFeatureStatus
) {
}
