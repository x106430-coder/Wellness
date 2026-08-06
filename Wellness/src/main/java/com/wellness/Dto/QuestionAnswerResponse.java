package com.wellness.Dto;

import com.wellness.Entiity.QuestionAnswer;
import com.wellness.Entiity.QuestionCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record QuestionAnswerResponse(
        Long id,
        Long userId,
        QuestionCode questionCode,
        String answerValue,
        boolean skipped,
        LocalDate answerDate,
        LocalDateTime updatedAt
) {
    public static QuestionAnswerResponse from(QuestionAnswer answer) {
        return new QuestionAnswerResponse(answer.getId(), answer.getUserId(), answer.getQuestionCode(),
                answer.getAnswerValue(), answer.isSkipped(), answer.getAnswerDate(), answer.getUpdatedAt());
    }
}
