package com.wellness.Dto;

import java.time.LocalDate;
import java.util.List;

public record QuestionAnswersResponse(
        String scope,
        LocalDate answerDate,
        int answeredCount,
        int totalQuestions,
        boolean complete,
        List<QuestionAnswerResponse> answers
) {
}
