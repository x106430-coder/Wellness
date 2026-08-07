package com.wellness.Dto;

public record HomeSummaryResponse(
        String nickname,
        int dailyAnsweredCount,
        int totalDailyQuestions,
        int weeklyAnsweredCount,
        int totalWeeklyQuestions,
        boolean dailyDiagnosisReady,
        boolean weeklyDiagnosisReady
) {
}
