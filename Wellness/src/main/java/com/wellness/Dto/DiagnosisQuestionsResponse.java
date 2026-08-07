package com.wellness.Dto;

import java.util.List;

public record DiagnosisQuestionsResponse(
        String scope,
        List<QuestionDefinitionResponse> questions
) {
}
