package com.wellness.Service;

import com.wellness.Entity.QuestionCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyScoreCalculatorTest {

    private final EnergyScoreCalculator calculator = new EnergyScoreCalculator();

    @Test
    void calculatesWeightedScoreFromAnsweredDailyQuestions() {
        Integer score = calculator.calculate(Map.of(
                QuestionCode.TODAY_ENERGY, "NORMAL",
                QuestionCode.LAST_NIGHT_SLEEP, "8:00",
                QuestionCode.SKIN_STATUS, "SENSITIVE",
                QuestionCode.HARDEST_MOMENT, "EMOTION"
        ));

        assertThat(score).isEqualTo(63);
    }

    @Test
    void normalizesWeightsWhenSomeQuestionsAreSkipped() {
        Integer score = calculator.calculate(Map.of(
                QuestionCode.TODAY_ENERGY, "HIGH",
                QuestionCode.LAST_NIGHT_SLEEP, "8:00"
        ));

        assertThat(score).isEqualTo(82);
    }

    @Test
    void returnsNullWhenNoScorableQuestionWasAnswered() {
        assertThat(calculator.calculate(Map.of(
                QuestionCode.CARE_AVAILABLE_TIME, "TEN_MIN"
        ))).isNull();
    }
}
