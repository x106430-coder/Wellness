package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Dto.ProfileSummaryResponse;
import com.wellness.Entity.DiagnosisAnalysisResult;
import com.wellness.Entity.ProfileInsight;
import com.wellness.Entity.User;
import com.wellness.Repository.DiagnosisAnalysisResultRepository;
import com.wellness.Repository.ProfileInsightRepository;
import com.wellness.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final DiagnosisAnalysisResultRepository diagnosisAnalysisResultRepository;
    private final ProfileInsightRepository profileInsightRepository;
    private final AiCommentService aiCommentService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${profile.lookback-days:7}")
    private int lookbackDays;

    @Transactional
    public ProfileSummaryResponse getSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = today.minusDays(Math.max(1, lookbackDays) - 1L);
        List<DiagnosisAnalysisResult> results = diagnosisAnalysisResultRepository
                .findByUserIdAndAnalysisDateBetweenOrderByAnalysisDateAsc(
                        userId, startDate, today);

        ProfileInsight insight = profileInsightRepository
                .findByUserIdAndProfileDateAndLookbackDays(userId, today, lookbackDays)
                .filter(saved -> saved.getSourceCount() == results.size())
                .orElseGet(() -> createInsight(userId, today, results));

        return new ProfileSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getGender().name(),
                user.getAge(),
                user.getSubscriptionPlan().name(),
                user.getCreatedAt(),
                lookbackDays,
                results.size(),
                insight.getComment(),
                insight.getGeneratedBy(),
                "COMING_SOON",
                "COMING_SOON"
        );
    }

    private ProfileInsight createInsight(
            Long userId,
            LocalDate today,
            List<DiagnosisAnalysisResult> results
    ) {
        profileInsightRepository.deleteByUserIdAndProfileDate(userId, today);
        profileInsightRepository.flush();
        List<DiagnosisAnalysisResponse> analyses = results.stream()
                .map(this::deserialize)
                .toList();
        String fallback = fallbackComment(analyses);
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("lookbackDays", lookbackDays);
        history.put("analyzedDays", analyses.size());
        history.put("energyScores", analyses.stream()
                .map(DiagnosisAnalysisResponse::energyScore)
                .toList());
        history.put("notTodayItems", analyses.stream()
                .flatMap(analysis -> analysis.avoidances().stream())
                .map(item -> item.title())
                .toList());
        history.put("todayItems", analyses.stream()
                .flatMap(analysis -> analysis.todos().stream())
                .map(item -> item.title())
                .toList());

        AiCommentService.AiCommentResult generated =
                aiCommentService.createProfileInsight(history, fallback);
        return profileInsightRepository.save(new ProfileInsight(
                userId,
                today,
                lookbackDays,
                results.size(),
                generated.comment(),
                generated.generatedBy(),
                LocalDateTime.now(clock)
        ));
    }

    private DiagnosisAnalysisResponse deserialize(DiagnosisAnalysisResult result) {
        try {
            return objectMapper.readValue(
                    result.getResponseJson(), DiagnosisAnalysisResponse.class);
        } catch (Exception exception) {
            throw new IllegalStateException("진단 기록을 읽지 못했습니다.", exception);
        }
    }

    private String fallbackComment(List<DiagnosisAnalysisResponse> analyses) {
        if (analyses.isEmpty()) {
            return "아직 나를 설명할 진단 기록이 부족해요. 오늘의 진단부터 천천히 시작해보세요.";
        }
        long lowEnergyDays = analyses.stream()
                .map(DiagnosisAnalysisResponse::energyScore)
                .filter(score -> score != null && score < 50)
                .count();
        if (lowEnergyDays >= Math.max(1, analyses.size() / 2)) {
            return "최근에는 에너지가 낮은 날이 있어 무리해서 채우기보다 회복할 시간을 먼저 확보하는 편이 잘 맞아 보여요.";
        }
        return "최근 기록을 보면 상태에 맞춰 할 일을 줄이고 작은 관리부터 이어가는 방식이 잘 맞아 보여요.";
    }
}
