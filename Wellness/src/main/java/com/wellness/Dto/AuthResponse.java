package com.wellness.Dto;

public record AuthResponse(
        Long userId,
        String loginId,
        String nickname,
        String accessToken,
        String tokenType
) {
}
