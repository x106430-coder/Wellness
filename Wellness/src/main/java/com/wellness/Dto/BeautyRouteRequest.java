package com.wellness.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record BeautyRouteRequest(
        LocalDate preferredDate,

        @NotBlank(message = "가능 시간은 필수입니다.")
        @Size(max = 30)
        String availableTime,

        @NotBlank(message = "예산은 필수입니다.")
        @Size(max = 30)
        String budget,

        @NotEmpty(message = "피부 고민을 한 개 이상 선택해주세요.")
        @Size(max = 10)
        List<@Size(max = 50) String> concerns,

        @NotEmpty(message = "선호 관리를 한 개 이상 선택해주세요.")
        @Size(max = 10)
        List<@Size(max = 50) String> preferences
) {
}
