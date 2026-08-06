package com.wellness.Dto;

import com.wellness.Entiity.QuestionCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record QuestionAnswerRequest(
        @NotNull @Positive Long userId,
        @NotNull QuestionCode questionCode,
        @Size(max = 500) String answerValue,
        @NotNull Boolean skipped
) {
    @AssertTrue(message = "answerValue is required when skipped is false")
    public boolean isAnswerValueValid() {
        return skipped == null || skipped || (answerValue != null && !answerValue.isBlank());
    }
}
