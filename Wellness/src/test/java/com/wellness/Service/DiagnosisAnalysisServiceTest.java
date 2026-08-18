package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Entity.Gender;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.QuestionFrequency;
import com.wellness.Entity.SubscriptionPlan;
import com.wellness.Entity.User;
import com.wellness.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DiagnosisAnalysisServiceTest {

    @Autowired
    private DiagnosisAnalysisService diagnosisAnalysisService;

    @Autowired
    private DiagnosisService diagnosisService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void lowEnergyAndSkippedAnswersChangeAnalysisWithoutGuessingSkippedValues() {
        User user = userRepository.save(new User(
                "analysis@example.com", "분석", Gender.OTHER, 25, "encoded", LocalDateTime.now()));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.TODAY_ENERGY, "LOW", null, false));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.SKIN_STATUS, null, null, true));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.LAST_NIGHT_SLEEP, "5:30", null, false));
        completeRemainingDailyQuestions(user.getId());

        DiagnosisAnalysisResponse response = diagnosisAnalysisService.analyze(user.getId());

        assertThat(response.energyScore()).isEqualTo(40);
        assertThat(response.subscriptionPlan()).isEqualTo("FREE");
        assertThat(response.usedQuestionCodes()).contains(QuestionCode.TODAY_ENERGY, QuestionCode.LAST_NIGHT_SLEEP);
        assertThat(response.skippedQuestionCodes()).contains(QuestionCode.SKIN_STATUS);
        assertThat(response.todos()).extracting(item -> item.code()).contains("RECOVERY_FIRST", "EARLY_BEDTIME");
        assertThat(response.avoidances()).extracting(item -> item.code()).contains("NO_INTENSE_WORKOUT");
        assertThat(response.routeJudgmentComment()).contains("고강도 운동");
    }

    @Test
    void freeAndPremiumUsersReceiveTheSameDiagnosisRecommendations() {
        User freeUser = userRepository.save(new User(
                "free@example.com", "무료", Gender.FEMALE, 28, "encoded", LocalDateTime.now()));
        User premiumUser = new User(
                "premium@example.com", "구독", Gender.FEMALE, 28, "encoded", LocalDateTime.now());
        premiumUser.changeSubscriptionPlan(SubscriptionPlan.PREMIUM);
        userRepository.save(premiumUser);

        saveSameDiagnosisAnswers(freeUser.getId());
        saveSameDiagnosisAnswers(premiumUser.getId());

        DiagnosisAnalysisResponse freeResponse = diagnosisAnalysisService.analyze(freeUser.getId());
        DiagnosisAnalysisResponse premiumResponse = diagnosisAnalysisService.analyze(premiumUser.getId());

        assertThat(freeResponse.subscriptionPlan()).isEqualTo("FREE");
        assertThat(premiumResponse.subscriptionPlan()).isEqualTo("PREMIUM");
        assertThat(premiumResponse.energyScore()).isEqualTo(freeResponse.energyScore());
        assertThat(premiumResponse.todos()).isEqualTo(freeResponse.todos());
        assertThat(premiumResponse.avoidances()).isEqualTo(freeResponse.avoidances());
        assertThat(premiumResponse.summary()).isEqualTo(freeResponse.summary());
    }

    private void saveSameDiagnosisAnswers(Long userId) {
        diagnosisService.saveOrUpdate(userId,
                new QuestionAnswerRequest(QuestionCode.TODAY_ENERGY, "LOW", null, false));
        diagnosisService.saveOrUpdate(userId,
                new QuestionAnswerRequest(QuestionCode.CARE_AVAILABLE_TIME, "THIRTY_MIN", null, false));
        diagnosisService.saveOrUpdate(userId,
                new QuestionAnswerRequest(QuestionCode.TODAY_PLANNED_CARE, null,
                        java.util.List.of("WORKOUT", "SKINCARE", "SLEEP_EARLY"), false));
        diagnosisService.saveOrUpdate(userId,
                new QuestionAnswerRequest(QuestionCode.LAST_NIGHT_SLEEP, "5", null, false));
        completeRemainingDailyQuestions(userId);
    }

    @Test
    void skippedEnergyIsNotConvertedToAnInventedScore() {
        User user = userRepository.save(new User(
                "skip-energy@example.com", "건너뜀", Gender.MALE, 31, "encoded", LocalDateTime.now()));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.TODAY_ENERGY, null, null, true));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.LAST_NIGHT_SLEEP, "7", null, false));
        completeRemainingDailyQuestions(user.getId());

        DiagnosisAnalysisResponse response = diagnosisAnalysisService.analyze(user.getId());

        assertThat(response.energyScore()).isEqualTo(85);
        assertThat(response.usedQuestionCodes()).doesNotContain(QuestionCode.TODAY_ENERGY);
        assertThat(response.skippedQuestionCodes()).contains(QuestionCode.TODAY_ENERGY);
    }

    private void completeRemainingDailyQuestions(Long userId) {
        java.util.Set<QuestionCode> savedCodes = diagnosisService
                .getAnswers(userId, QuestionFrequency.DAILY)
                .answers()
                .stream()
                .map(answer -> answer.questionCode())
                .collect(java.util.stream.Collectors.toSet());

        java.util.List.of(
                        QuestionCode.TODAY_ENERGY,
                        QuestionCode.LAST_NIGHT_SLEEP,
                        QuestionCode.SKIN_STATUS,
                        QuestionCode.CARE_AVAILABLE_TIME,
                        QuestionCode.TODAY_PLANNED_CARE,
                        QuestionCode.HARDEST_MOMENT
                ).stream()
                .filter(code -> !savedCodes.contains(code))
                .forEach(code -> diagnosisService.saveOrUpdate(
                        userId,
                        new QuestionAnswerRequest(code, null, null, true)
                ));
    }
}
