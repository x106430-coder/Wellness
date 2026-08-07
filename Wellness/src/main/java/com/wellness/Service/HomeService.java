package com.wellness.Service;

import com.wellness.Dto.HomeSummaryResponse;
import com.wellness.Entity.QuestionFrequency;
import com.wellness.Entity.User;
import com.wellness.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {

    private final UserRepository userRepository;
    private final DiagnosisService diagnosisService;
    private final QuestionCatalogService questionCatalogService;

    public HomeService(
            UserRepository userRepository,
            DiagnosisService diagnosisService,
            QuestionCatalogService questionCatalogService
    ) {
        this.userRepository = userRepository;
        this.diagnosisService = diagnosisService;
        this.questionCatalogService = questionCatalogService;
    }

    @Transactional(readOnly = true)
    public HomeSummaryResponse getHomeSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int totalDailyQuestions = questionCatalogService.countByFrequency(QuestionFrequency.DAILY);
        int totalWeeklyQuestions = questionCatalogService.countByFrequency(QuestionFrequency.WEEKLY);
        int dailyAnsweredCount = diagnosisService.countAnsweredQuestions(userId, QuestionFrequency.DAILY);
        int weeklyAnsweredCount = diagnosisService.countAnsweredQuestions(userId, QuestionFrequency.WEEKLY);

        return new HomeSummaryResponse(
                user.getNickname(),
                dailyAnsweredCount,
                totalDailyQuestions,
                weeklyAnsweredCount,
                totalWeeklyQuestions,
                dailyAnsweredCount < totalDailyQuestions,
                weeklyAnsweredCount < totalWeeklyQuestions
        );
    }
}
