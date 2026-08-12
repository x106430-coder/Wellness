package com.wellness.Service;

import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.SubscriptionPlan;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DiagnosisAnalysisContext(
        LocalDate analysisDate,
        SubscriptionPlan subscriptionPlan,
        Map<QuestionCode, String> answers,
        List<QuestionCode> skippedQuestionCodes,
        int totalQuestionCount
) {
}
