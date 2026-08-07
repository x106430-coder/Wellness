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
    private DiagnosisService diagnosisService;

    @Autowired
    private WellnessRepository wellnessRepository;

    @Autowired
    private Validator validator;

    @Test
    void sameQuestionSubmittedAgainOnSameDateUpdatesExistingRow() {
        QuestionAnswerResponse first = diagnosisService.saveOrUpdate(
                1L, new QuestionAnswerRequest(QuestionCode.LAST_NIGHT_SLEEP, "6", null, false));
        QuestionAnswerResponse updated = diagnosisService.saveOrUpdate(
                1L, new QuestionAnswerRequest(QuestionCode.LAST_NIGHT_SLEEP, "8", null, false));

        assertThat(updated.id()).isEqualTo(first.id());
        assertThat(updated.answerValue()).isEqualTo("8");
        assertThat(wellnessRepository.count()).isEqualTo(1);
    }

    @Test
    void skippedAnswerIsSavedWithNullValue() {
        QuestionAnswerResponse response = diagnosisService.saveOrUpdate(
                1L, new QuestionAnswerRequest(QuestionCode.TODAY_ENERGY, "ignored", null, true));

        assertThat(response.skipped()).isTrue();
        assertThat(response.answerValue()).isNull();
    }

    @Test
    void sameWeeklyQuestionSubmittedAgainInSameWeekUpdatesExistingRow() {
        QuestionAnswerResponse first = diagnosisService.saveOrUpdate(
                1L, new QuestionAnswerRequest(QuestionCode.WATER_INTAKE, "1L", null, false));
        QuestionAnswerResponse updated = diagnosisService.saveOrUpdate(
                1L, new QuestionAnswerRequest(QuestionCode.WATER_INTAKE, "1_5L", null, false));

        assertThat(updated.id()).isEqualTo(first.id());
        assertThat(updated.answerValue()).isEqualTo("1_5L");
        assertThat(wellnessRepository.count()).isEqualTo(1);
    }

    @Test
    void answerValueIsRequiredWhenNotSkipped() {
        QuestionAnswerRequest request = new QuestionAnswerRequest(
                QuestionCode.CARE_AVAILABLE_TIME, null, null, false);

        Set<ConstraintViolation<QuestionAnswerRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("answerValue is required when skipped is false");
    }

    @Test
    void multipleAnswersAreSerializedWhenMultiSelectQuestionIsSubmitted() {
        QuestionAnswerResponse response = diagnosisService.saveOrUpdate(
                1L,
                new QuestionAnswerRequest(
                        QuestionCode.TODAY_PLANNED_CARE,
                        null,
                        java.util.List.of("WORKOUT", "SKINCARE"),
                        false
                )
        );

        assertThat(response.answerValue()).isEqualTo("WORKOUT|SKINCARE");
        assertThat(response.answerValues()).containsExactly("WORKOUT", "SKINCARE");
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
