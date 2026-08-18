package com.wellness.Service;

import com.wellness.Dto.HomeSummaryResponse;
import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Entity.Gender;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.User;
import com.wellness.Repository.DiagnosisAnalysisResultRepository;
import com.wellness.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class HomeServiceTest {

    @Autowired
    private HomeService homeService;

    @Autowired
    private DiagnosisService diagnosisService;

    @Autowired
    private DiagnosisAnalysisService diagnosisAnalysisService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiagnosisAnalysisResultRepository diagnosisAnalysisResultRepository;

    @Test
    void incompleteDailyDiagnosisReturnsOnlyDiagnosisRequiredState() {
        User user = createUser("home-incomplete@example.com");

        HomeSummaryResponse response = homeService.getHomeSummary(user.getId());

        assertThat(response.dailyDiagnosisReady()).isTrue();
        assertThat(response.energyScore()).isNull();
        assertThat(response.headline()).isNull();
        assertThat(response.todos()).isEmpty();
        assertThat(response.avoidances()).isEmpty();
        assertThat(diagnosisAnalysisResultRepository.count()).isZero();
    }

    @Test
    void completeDailyDiagnosisIsShownOnlyAfterExplicitAnalysisAndIsInvalidatedByAnswerChange() {
        User user = createUser("home-complete@example.com");
        saveCompleteDailyDiagnosis(user.getId());

        HomeSummaryResponse beforeAnalysis = homeService.getHomeSummary(user.getId());

        assertThat(beforeAnalysis.dailyDiagnosisReady()).isTrue();
        assertThat(beforeAnalysis.energyScore()).isNull();
        assertThat(diagnosisAnalysisResultRepository.count()).isZero();

        diagnosisAnalysisService.analyze(user.getId());
        HomeSummaryResponse first = homeService.getHomeSummary(user.getId());
        HomeSummaryResponse second = homeService.getHomeSummary(user.getId());

        assertThat(first.dailyDiagnosisReady()).isFalse();
        assertThat(first.headline()).isNotBlank();
        assertThat(second.headline()).isEqualTo(first.headline());
        assertThat(diagnosisAnalysisResultRepository.count()).isEqualTo(1);

        diagnosisService.saveOrUpdate(user.getId(), new QuestionAnswerRequest(
                QuestionCode.TODAY_ENERGY, "HIGH", null, false));

        assertThat(diagnosisAnalysisResultRepository.count()).isZero();
    }

    @Test
    void allSkippedDailyDiagnosisStillRequiresDiagnosisAndDoesNotAnalyze() {
        User user = createUser("home-all-skipped@example.com");
        List.of(
                QuestionCode.TODAY_ENERGY,
                QuestionCode.LAST_NIGHT_SLEEP,
                QuestionCode.SKIN_STATUS,
                QuestionCode.CARE_AVAILABLE_TIME,
                QuestionCode.TODAY_PLANNED_CARE,
                QuestionCode.HARDEST_MOMENT
        ).forEach(code -> diagnosisService.saveOrUpdate(user.getId(),
                new QuestionAnswerRequest(code, null, null, true)));

        HomeSummaryResponse response = homeService.getHomeSummary(user.getId());

        assertThat(response.dailyAnsweredCount()).isEqualTo(6);
        assertThat(response.dailyDiagnosisReady()).isTrue();
        assertThat(response.energyScore()).isNull();
        assertThat(response.todos()).isEmpty();
        assertThat(diagnosisAnalysisResultRepository.count()).isZero();
    }

    private User createUser(String email) {
        return userRepository.save(new User(
                email, "홈테스트", Gender.OTHER, 25, "encoded", LocalDateTime.now()));
    }

    private void saveCompleteDailyDiagnosis(Long userId) {
        List.of(
                new QuestionAnswerRequest(QuestionCode.TODAY_ENERGY, "NORMAL", null, false),
                new QuestionAnswerRequest(QuestionCode.LAST_NIGHT_SLEEP, "7:30", null, false),
                new QuestionAnswerRequest(QuestionCode.SKIN_STATUS, "GOOD", null, false),
                new QuestionAnswerRequest(QuestionCode.CARE_AVAILABLE_TIME, "THIRTY_MIN", null, false),
                new QuestionAnswerRequest(
                        QuestionCode.TODAY_PLANNED_CARE,
                        null,
                        List.of("SKINCARE", "STRETCHING"),
                        false
                ),
                new QuestionAnswerRequest(QuestionCode.HARDEST_MOMENT, "WORK_STUDY", null, false)
        ).forEach(request -> diagnosisService.saveOrUpdate(userId, request));
    }
}
