package com.wellness.Dto;

public record AuthenticatedUser(
        Long userId,
        String email
) {
}
