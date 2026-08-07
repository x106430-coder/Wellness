package com.wellness.Dto;

import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.QuestionFrequency;
import com.wellness.Entity.QuestionInputType;

import java.util.List;

public record QuestionDefinitionResponse(
        QuestionCode code,
        String title,
        QuestionFrequency frequency,
        QuestionInputType inputType,
        boolean multiSelect,
        List<QuestionOptionResponse> options
) {
}
