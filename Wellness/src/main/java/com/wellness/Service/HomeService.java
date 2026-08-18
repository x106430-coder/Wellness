package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Dto.HomeSummaryResponse;
import com.wellness.Dto.QuestionAnswerResponse;
import com.wellness.Entity.QuestionAnswer;
import com.wellness.Entity.QuestionFrequency;
import com.wellness.Entity.User;
import com.wellness.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HomeService {

    private final UserRepository userRepository;
    private final DiagnosisService diagnosisService;
    private final QuestionCatalogService questionCatalogService;
    private final DiagnosisAnalysisService diagnosisAnalysisService;

    public HomeService(
            UserRepository userRepository,
            DiagnosisService diagnosisService,
            QuestionCatalogService questionCatalogService,
            DiagnosisAnalysisService diagnosisAnalysisService
    ) {
        this.userRepository = userRepository;
        this.diagnosisService = diagnosisService;
        this.questionCatalogService = questionCatalogService;
        this.diagnosisAnalysisService = diagnosisAnalysisService;
    }

    @Transactional
    public HomeSummaryResponse getHomeSummary(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다.")
                );

        int totalDailyQuestions =
                questionCatalogService.countByFrequency(
                        QuestionFrequency.DAILY
                );

        int totalWeeklyQuestions =
                questionCatalogService.countByFrequency(
                        QuestionFrequency.WEEKLY
                );

        int dailyAnsweredCount =
                diagnosisService.countAnsweredQuestions(
                        userId,
                        QuestionFrequency.DAILY
                );

        int weeklyAnsweredCount =
                diagnosisService.countAnsweredQuestions(
                        userId,
                        QuestionFrequency.WEEKLY
                );

        int dailyNonSkippedAnswerCount =
                diagnosisService.countNonSkippedAnswers(
                        userId,
                        QuestionFrequency.DAILY
                );

        // 오늘 진단에서 저장된 답변 가져오기
        List<QuestionAnswer> todayAnswers =
                diagnosisService.getTodayDailyAnswers(userId);

        // Entity를 프론트에 전달할 Response 형태로 변환
        List<QuestionAnswerResponse> dailyAnswers =
                todayAnswers.stream()
                        .map(QuestionAnswerResponse::from)
                        .toList();

        boolean dailyAnswersIncomplete = dailyAnsweredCount < totalDailyQuestions
                || dailyNonSkippedAnswerCount == 0;
        // 홈 조회는 저장된 오늘 분석만 읽습니다. 분석 생성과 OpenAI 호출은
        // 사용자가 오늘의 진단에서 "AI 분석하기"를 눌렀을 때만 수행합니다.
        DiagnosisAnalysisResponse analysis = dailyAnswersIncomplete
                ? null
                : diagnosisAnalysisService.getTodayAnalysis(userId);
        boolean dailyDiagnosisRequired = dailyAnswersIncomplete || analysis == null;

        return new HomeSummaryResponse(
                user.getNickname(),
                dailyAnsweredCount,
                totalDailyQuestions,
                weeklyAnsweredCount,
                totalWeeklyQuestions,
                dailyDiagnosisRequired,
                weeklyAnsweredCount < totalWeeklyQuestions,
                dailyAnswers,
                analysis == null ? null : analysis.energyScore(),
                analysis == null ? null : analysis.energyLevel(),
                analysis == null ? null : analysis.headline(),
                analysis == null ? null : analysis.summary(),
                analysis == null ? List.of() : analysis.todos(),
                analysis == null ? List.of() : analysis.avoidances(),
                analysis == null ? null : analysis.generatedBy(),
                analysis
        );
    }
}
