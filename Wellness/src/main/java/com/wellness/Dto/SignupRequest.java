package com.wellness.Dto;

import com.wellness.Entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Year;

public record SignupRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
        String nickname,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @NotNull(message = "성별은 필수입니다.")
        Gender gender,

        @Positive(message = "나이는 1 이상이어야 합니다.")
        Integer age,

        @Min(value = 1900, message = "출생연도는 1900년 이후여야 합니다.")
        @Max(value = 2100, message = "출생연도가 올바르지 않습니다.")
        Integer birthYear,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
        String password
) {
    @AssertTrue(message = "나이 또는 출생연도는 필수입니다.")
    public boolean isAgeOrBirthYearValid() {
        if (age != null) {
            return age > 0;
        }
        return birthYear != null && birthYear >= 1900 && birthYear <= Year.now().getValue();
    }

    public int resolvedAge() {
        if (age != null) {
            return age;
        }
        return Year.now().getValue() - birthYear;
    }
}
