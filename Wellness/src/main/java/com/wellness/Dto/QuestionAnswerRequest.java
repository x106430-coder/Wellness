package com.wellness.Dto;

import com.wellness.Entity.QuestionCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionAnswerRequest(
        @NotNull QuestionCode questionCode,
        @Size(max = 500) String answerValue,
        List<@Size(max = 100) String> answerValues,
        @NotNull Boolean skipped
) {
    @AssertTrue(message = "answerValue is required when skipped is false")
    public boolean isAnswerValueValid() {
        return skipped == null || skipped || hasSingleAnswer() || hasMultipleAnswers();
    }

    public String normalizedAnswerValue() {
        if (skipped == null || skipped) {
            return null;
        }

        if (hasMultipleAnswers()) {
            return answerValues.stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .reduce((left, right) -> left + "|" + right)
                    .orElse(null);
        }

        return hasSingleAnswer() ? answerValue.trim() : null;
    }

    private boolean hasSingleAnswer() {
        return answerValue != null && !answerValue.isBlank();
    }

    private boolean hasMultipleAnswers() {
        return answerValues != null && answerValues.stream().anyMatch(value -> value != null && !value.isBlank());
    }
}
