package com.wellness.Dto;

import com.wellness.Entity.Gender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SignupRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void birthYearCanBeUsedInsteadOfAge() {
        int birthYear = Year.now().getValue() - 25;
        SignupRequest request = new SignupRequest(
                "닉네임", "front@example.com", Gender.FEMALE,
                null, birthYear, "password123"
        );

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.resolvedAge()).isEqualTo(25);
    }

    @Test
    void eitherAgeOrBirthYearIsRequired() {
        SignupRequest request = new SignupRequest(
                "닉네임", "front@example.com", Gender.FEMALE,
                null, null, "password123"
        );

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .contains("나이 또는 출생연도는 필수입니다.");
    }
}
