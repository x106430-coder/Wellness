package com.wellness.Service;

import com.wellness.Config.JwtProvider;
import com.wellness.Dto.ProfileSummaryResponse;
import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Entity.Gender;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.User;
import com.wellness.Repository.RevokedTokenRepository;
import com.wellness.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthAndProfileServiceTest {

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private LogoutService logoutService;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiagnosisService diagnosisService;

    @Autowired
    private DiagnosisAnalysisService diagnosisAnalysisService;

    @Autowired
    private ProfileService profileService;

    @Test
    void accessTokenLastsThirtyDaysAndIsRevokedOnLogout() {
        String token = jwtProvider.createAccessToken(1L, "login@example.com");
        JwtProvider.ParsedToken parsed = jwtProvider.parseToken(token);

        assertThat(Duration.between(Instant.now(), parsed.expiresAt()).toDays())
                .isBetween(29L, 30L);

        logoutService.logout(token);

        assertThat(revokedTokenRepository.existsByTokenId(parsed.tokenId())).isTrue();
    }

    @Test
    void profileSummarizesRecentSavedDiagnosis() {
        User user = userRepository.save(new User(
                "profile@example.com", "프로필", Gender.FEMALE, 27,
                "encoded", LocalDateTime.now()));
        saveCompleteDailyDiagnosis(user.getId());
        diagnosisAnalysisService.analyze(user.getId());

        ProfileSummaryResponse response = profileService.getSummary(user.getId());

        assertThat(response.lookbackDays()).isEqualTo(7);
        assertThat(response.analyzedDays()).isEqualTo(1);
        assertThat(response.aiProfileComment()).isNotBlank();
        assertThat(response.subscriptionFeatureStatus()).isEqualTo("COMING_SOON");
    }

    private void saveCompleteDailyDiagnosis(Long userId) {
        List.of(
                new QuestionAnswerRequest(QuestionCode.TODAY_ENERGY, "NORMAL", null, false),
                new QuestionAnswerRequest(QuestionCode.LAST_NIGHT_SLEEP, "7", null, false),
                new QuestionAnswerRequest(QuestionCode.SKIN_STATUS, "GOOD", null, false),
                new QuestionAnswerRequest(QuestionCode.CARE_AVAILABLE_TIME, "THIRTY_MIN", null, false),
                new QuestionAnswerRequest(
                        QuestionCode.TODAY_PLANNED_CARE,
                        null,
                        List.of("SKINCARE"),
                        false),
                new QuestionAnswerRequest(QuestionCode.HARDEST_MOMENT, "WORK_STUDY", null, false)
        ).forEach(request -> diagnosisService.saveOrUpdate(userId, request));
    }
}
