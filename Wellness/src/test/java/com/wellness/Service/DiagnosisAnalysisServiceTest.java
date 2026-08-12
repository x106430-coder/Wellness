package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Entity.Gender;
import com.wellness.Entity.QuestionCode;
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

        DiagnosisAnalysisResponse response = diagnosisAnalysisService.analyze(user.getId());

        assertThat(response.energyScore()).isEqualTo(40);
        assertThat(response.subscriptionPlan()).isEqualTo("FREE");
        assertThat(response.usedQuestionCodes()).contains(QuestionCode.TODAY_ENERGY, QuestionCode.LAST_NIGHT_SLEEP);
        assertThat(response.skippedQuestionCodes()).containsExactly(QuestionCode.SKIN_STATUS);
        assertThat(response.todos()).extracting(item -> item.code()).contains("RECOVERY_FIRST", "EARLY_BEDTIME");
        assertThat(response.avoidances()).extracting(item -> item.code()).contains("NO_INTENSE_WORKOUT");
    }

    @Test
    void premiumUserReceivesMoreDetailedRecommendationList() {
        User user = new User(
                "premium@example.com", "구독", Gender.FEMALE, 28, "encoded", LocalDateTime.now());
        user.changeSubscriptionPlan(SubscriptionPlan.PREMIUM);
        userRepository.save(user);
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.TODAY_ENERGY, "LOW", null, false));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.CARE_AVAILABLE_TIME, "THIRTY_MIN", null, false));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.TODAY_PLANNED_CARE, null,
                        java.util.List.of("WORKOUT", "SKINCARE", "SLEEP_EARLY"), false));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.LAST_NIGHT_SLEEP, "5", null, false));

        DiagnosisAnalysisResponse response = diagnosisAnalysisService.analyze(user.getId());

        assertThat(response.subscriptionPlan()).isEqualTo("PREMIUM");
        assertThat(response.todos()).hasSizeGreaterThan(3);
        assertThat(response.summary()).contains("수면·피부·주간 습관");
    }

    @Test
    void skippedEnergyIsNotConvertedToAnInventedScore() {
        User user = userRepository.save(new User(
                "skip-energy@example.com", "건너뜀", Gender.MALE, 31, "encoded", LocalDateTime.now()));
        diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(QuestionCode.TODAY_ENERGY, null, null, true));

        DiagnosisAnalysisResponse response = diagnosisAnalysisService.analyze(user.getId());

        assertThat(response.energyScore()).isNull();
        assertThat(response.summary()).contains("추측하지 않았어요");
    }
}
