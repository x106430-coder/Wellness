package com.wellness.Dto;

import com.wellness.Entity.QuestionCode;

import java.time.LocalDate;
import java.util.List;

public record DiagnosisAnalysisResponse(
        LocalDate analysisDate,
        String subscriptionPlan,
        String energyLevel,
        Integer energyScore,
        String headline,
        String summary,
        String diagnosisComment,
        String routeComment,
        String routeJudgmentComment,
        String profileComment,
        List<DiagnosisRecommendationResponse> todos,
        List<DiagnosisRecommendationResponse> avoidances,
        List<QuestionCode> usedQuestionCodes,
        List<QuestionCode> skippedQuestionCodes,
        int dataCoveragePercent,
        String generatedBy
) {
}
