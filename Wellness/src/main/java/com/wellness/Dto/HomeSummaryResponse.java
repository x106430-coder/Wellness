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
        List<QuestionAnswerResponse> dailyAnswers
) {
}