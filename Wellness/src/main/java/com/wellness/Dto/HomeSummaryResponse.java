package com.wellness.Dto;

import java.util.List;

public record HomeSummaryResponse(
        String nickname,
        int dailyAnsweredCount,
        int totalDailyQuestions,
        int weeklyAnsweredCount,
        int totalWeeklyQuestions,
        boolean dailyDiagnosisReady,
        boolean weeklyDiagnosisReady,
        List<QuestionAnswerResponse> dailyAnswers,
        Integer energyScore,
        String energyLevel,
        String headline,
        String summary,
        List<DiagnosisRecommendationResponse> todos,
        List<DiagnosisRecommendationResponse> avoidances,
        String generatedBy,
        DiagnosisAnalysisResponse analysis
) {
}
