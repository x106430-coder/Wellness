package com.wellness.Service;

import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Dto.QuestionAnswerResponse;
import com.wellness.Entity.QuestionCode;
import com.wellness.Repository.WellnessRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WellnessServiceTest {

    @Autowired
    private WellnessService wellnessService;

    @Autowired
    private WellnessRepository wellnessRepository;

    @Autowired
    private Validator validator;

    @Test
    void sameQuestionSubmittedAgainOnSameDateUpdatesExistingRow() {
        QuestionAnswerResponse first = wellnessService.saveOrUpdate(
                1L, new QuestionAnswerRequest(QuestionCode.SLEEP_HOURS, "6", false));
        QuestionAnswerResponse updated = wellnessService.saveOrUpdate(
                1L, new QuestionAnswerRequest(QuestionCode.SLEEP_HOURS, "8", false));

        assertThat(updated.id()).isEqualTo(first.id());
        assertThat(updated.answerValue()).isEqualTo("8");
        assertThat(wellnessRepository.count()).isEqualTo(1);
    }

    @Test
    void skippedAnswerIsSavedWithNullValue() {
        QuestionAnswerResponse response = wellnessService.saveOrUpdate(
                1L, new QuestionAnswerRequest(QuestionCode.FATIGUE_LEVEL, "ignored", true));

        assertThat(response.skipped()).isTrue();
        assertThat(response.answerValue()).isNull();
    }

    @Test
    void answerValueIsRequiredWhenNotSkipped() {
        QuestionAnswerRequest request = new QuestionAnswerRequest(
                QuestionCode.AVAILABLE_TIME, null, false);

        Set<ConstraintViolation<QuestionAnswerRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("answerValue is required when skipped is false");
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-08-06T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }
}
