package com.wellness.Dto;

import com.wellness.Entity.QuestionAnswer;
import com.wellness.Entity.QuestionCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record QuestionAnswerResponse(
        Long id,
        Long userId,
        QuestionCode questionCode,
        String answerValue,
        List<String> answerValues,
        boolean skipped,
        LocalDate answerDate,
        LocalDateTime updatedAt
) {
    public static QuestionAnswerResponse from(QuestionAnswer answer) {
        return new QuestionAnswerResponse(
                answer.getId(),
                answer.getUserId(),
                answer.getQuestionCode(),
                answer.getAnswerValue(),
                splitAnswerValues(answer.getAnswerValue()),
                answer.isSkipped(),
                answer.getAnswerDate(),
                answer.getUpdatedAt()
        );
    }

    private static List<String> splitAnswerValues(String answerValue) {
        if (answerValue == null || answerValue.isBlank() || !answerValue.contains("|")) {
            return List.of();
        }

        return Arrays.stream(answerValue.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
