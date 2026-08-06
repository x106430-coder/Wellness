package com.wellness.Dto;

public record AuthResponse(
        Long userId,
        String loginId,
        String accessToken,
        String tokenType
) {
}