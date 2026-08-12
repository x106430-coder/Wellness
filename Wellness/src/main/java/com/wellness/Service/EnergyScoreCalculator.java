package com.wellness.Service;

import com.wellness.Entity.QuestionCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EnergyScoreCalculator {

    public Integer calculate(Map<QuestionCode, String> answers) {
        WeightedScore scores = new WeightedScore();

        scores.add(fromAnswer(answers.get(QuestionCode.TODAY_ENERGY)), 55);
        scores.add(sleepScore(answers.get(QuestionCode.LAST_NIGHT_SLEEP)), 25);
        scores.add(skinScore(answers.get(QuestionCode.SKIN_STATUS)), 10);
        scores.add(hardestMomentScore(answers.get(QuestionCode.HARDEST_MOMENT)), 10);

        if (scores.isEmpty()) {
            return null;
        }

        return Math.max(0, Math.min(100, scores.average()));
    }

    public Integer fromAnswer(String answerValue) {
        if (answerValue == null) {
            return null;
        }

        return switch (answerValue) {
            case "VERY_LOW" -> 20;
            case "LOW" -> 40;
            case "NORMAL" -> 60;
            case "HIGH" -> 80;
            case "VERY_HIGH" -> 100;
            default -> null;
        };
    }

    private Integer sleepScore(String value) {
        Double hours = parseSleepHours(value);
        if (hours == null) return null;
        if (hours < 4) return 20;
        if (hours < 6) return 40;
        if (hours < 7) return 60;
        if (hours <= 9) return 85;
        if (hours <= 10) return 75;
        return 60;
    }

    private Integer skinScore(String value) {
        if (value == null) return null;
        return switch (value) {
            case "GOOD" -> 90;
            case "DRY", "OILY" -> 65;
            case "ACNE", "SENSITIVE" -> 50;
            default -> null;
        };
    }

    private Integer hardestMomentScore(String value) {
        if (value == null) return null;
        return switch (value) {
            case "WORK_STUDY" -> 55;
            case "RELATIONSHIP" -> 45;
            case "HEALTH" -> 35;
            case "EMOTION" -> 40;
            case "OTHER" -> 55;
            default -> null;
        };
    }

    private Double parseSleepHours(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.contains(":")) {
                String[] parts = value.split(":", 2);
                return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]) / 60.0;
            }
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static class WeightedScore {
        private int weightedTotal;
        private int totalWeight;

        private void add(Integer score, int weight) {
            if (score == null) return;
            weightedTotal += score * weight;
            totalWeight += weight;
        }

        private boolean isEmpty() {
            return totalWeight == 0;
        }

        private int average() {
            return (int) Math.round(weightedTotal / (double) totalWeight);
        }
    }
}
