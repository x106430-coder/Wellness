package com.wellness.Dto;

import com.wellness.Entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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

        @NotNull(message = "나이는 필수입니다.")
        @Positive(message = "나이는 1 이상이어야 합니다.")
        Integer age,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
        String password
) {
}
