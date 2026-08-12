package com.wellness.Dto;

public record AuthResponse(
        Long userId,
        String email,
        String nickname,
        String gender,
        Integer age,
        String subscriptionPlan,
        String accessToken,
        String tokenType
) {
}
