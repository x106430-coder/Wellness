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

    @Transactional(readOnly = true)
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

        // 오늘 진단 답변 가져오기
        List<QuestionAnswer> todayAnswers =
                diagnosisService.getTodayDailyAnswers(userId);

        List<QuestionAnswerResponse> dailyAnswers =
                todayAnswers.stream()
                        .map(QuestionAnswerResponse::from)
                        .toList();

        // 팀원이 만든 진단 분석 결과 가져오기
        DiagnosisAnalysisResponse analysis =
                diagnosisAnalysisService.analyze(userId);

        return new HomeSummaryResponse(
                user.getNickname(),
                dailyAnsweredCount,
                totalDailyQuestions,
                weeklyAnsweredCount,
                totalWeeklyQuestions,
                dailyAnsweredCount < totalDailyQuestions,
                weeklyAnsweredCount < totalWeeklyQuestions,
                dailyAnswers,

                // 홈 화면에 사용할 분석 결과
                analysis.energyScore(),
                analysis.energyLevel(),
                analysis.headline(),
                analysis.summary(),
                analysis.todos(),
                analysis.avoidances(),
                analysis.generatedBy()
        );
    }
}