package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Dto.DiagnosisRecommendationResponse;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.SubscriptionPlan;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RuleBasedDiagnosisAnalysisGenerator implements DiagnosisAnalysisGenerator {

    private final EnergyScoreCalculator energyScoreCalculator;

    @Override
    public DiagnosisAnalysisResponse generate(DiagnosisAnalysisContext context) {
        Map<QuestionCode, String> answers = context.answers();
        String energyLevel = answers.getOrDefault(QuestionCode.TODAY_ENERGY, "UNKNOWN");
        Integer energyScore = energyScoreCalculator.calculate(answers);
        boolean premium = context.subscriptionPlan() == SubscriptionPlan.PREMIUM;

        List<DiagnosisRecommendationResponse> todos = new ArrayList<>();
        List<DiagnosisRecommendationResponse> avoidances = new ArrayList<>();

        if (energyScore != null) {
            addEnergyRecommendations(energyScore, todos, avoidances);
        }
        addAvailableTimeRecommendation(answers.get(QuestionCode.CARE_AVAILABLE_TIME), todos);
        addPlannedCareRecommendations(answers.get(QuestionCode.TODAY_PLANNED_CARE), energyScore, todos);
        addSleepRecommendations(answers.get(QuestionCode.LAST_NIGHT_SLEEP), todos, avoidances);
        addSkinRecommendations(answers.get(QuestionCode.SKIN_STATUS), todos, avoidances);
        addHydrationRecommendation(answers.get(QuestionCode.WATER_INTAKE), todos);

        int todoLimit = premium ? 6 : 3;
        int avoidanceLimit = premium ? 5 : 2;
        List<DiagnosisRecommendationResponse> selectedTodos = limitDistinct(todos, todoLimit);
        List<DiagnosisRecommendationResponse> selectedAvoidances = limitDistinct(avoidances, avoidanceLimit);
        int coverage = context.totalQuestionCount() == 0 ? 0
                : (int) Math.round(context.answers().size() * 100.0 / context.totalQuestionCount());

        return new DiagnosisAnalysisResponse(
                context.analysisDate(),
                context.subscriptionPlan().name(),
                energyLevel,
                energyScore,
                headline(energyScore),
                summary(energyScore, premium, context.answers().size(), context.skippedQuestionCodes().size()),
                selectedTodos,
                selectedAvoidances,
                sortedCodes(context.answers().keySet().stream().toList()),
                sortedCodes(context.skippedQuestionCodes()),
                coverage,
                "RULE_BASED"
        );
    }

    private void addEnergyRecommendations(
            int score,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (score <= 40) {
            todos.add(item("RECOVERY_FIRST", "회복을 먼저 챙기기", "현재 에너지가 낮아 작은 회복 행동이 우선이에요.", 1));
            todos.add(item("LIGHT_MOVEMENT", "10분 가볍게 움직이기", "부담이 적은 움직임으로 리듬만 유지해요.", 2));
            avoidances.add(item("NO_INTENSE_WORKOUT", "고강도 운동 피하기", "낮은 에너지 상태에서 무리하면 회복이 늦어질 수 있어요.", 1));
            avoidances.add(item("NO_OVERBOOKING", "관리 계획을 너무 많이 잡지 않기", "오늘은 완료 개수보다 회복이 중요해요.", 2));
        } else if (score <= 60) {
            todos.add(item("BALANCED_ROUTINE", "가벼운 관리 한두 가지 완료하기", "보통 에너지에는 짧고 균형 잡힌 루틴이 잘 맞아요.", 1));
            avoidances.add(item("NO_SUDDEN_OVERLOAD", "갑자기 강도를 높이지 않기", "현재 컨디션을 유지할 정도가 적당해요.", 2));
        } else {
            todos.add(item("ACTIVE_ROUTINE", "계획한 관리를 적극적으로 진행하기", "에너지가 좋아 주요 관리에 집중하기 좋은 날이에요.", 1));
            avoidances.add(item("NO_ALL_OUT", "한 번에 에너지를 모두 쓰지 않기", "좋은 컨디션도 회복 시간을 남겨두면 오래 유지돼요.", 3));
        }
    }

    private void addAvailableTimeRecommendation(String value, List<DiagnosisRecommendationResponse> todos) {
        if (value == null || "NONE".equals(value)) {
            return;
        }
        String title = switch (value) {
            case "TEN_MIN" -> "10분짜리 핵심 루틴 선택하기";
            case "THIRTY_MIN" -> "30분 관리 루틴 진행하기";
            case "ONE_HOUR_PLUS" -> "운동과 회복을 나눠 진행하기";
            default -> "가능한 시간 안에서 한 가지 완료하기";
        };
        todos.add(item("FIT_AVAILABLE_TIME", title, "입력한 관리 가능 시간에 맞춰 부담을 조절했어요.", 2));
    }

    private void addPlannedCareRecommendations(String value, Integer energyScore, List<DiagnosisRecommendationResponse> todos) {
        if (value == null) {
            return;
        }
        List<String> plans = List.of(value.split("\\|"));
        if (plans.contains("SKINCARE")) {
            todos.add(item("PLANNED_SKINCARE", "계획한 스킨케어 하기", "직접 선택한 오늘의 관리 항목이에요.", 3));
        }
        if (plans.contains("WORKOUT")) {
            String title = energyScore != null && energyScore <= 40
                    ? "운동 강도를 절반으로 낮추기"
                    : "계획한 운동 진행하기";
            todos.add(item("PLANNED_WORKOUT", title, "오늘의 에너지에 맞춰 운동 강도를 조절했어요.", 2));
        }
        if (plans.contains("SLEEP_EARLY")) {
            todos.add(item("PLANNED_SLEEP", "평소보다 일찍 잠들 준비하기", "직접 선택한 회복 계획을 우선 반영했어요.", 2));
        }
        if (plans.contains("MEDITATION")) {
            todos.add(item("PLANNED_MINDFULNESS", "짧게 마음 관리하기", "부담이 적고 에너지 회복에 도움이 되는 계획이에요.", 3));
        }
    }

    private void addSleepRecommendations(
            String value,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        Double hours = parseSleepHours(value);
        if (hours != null && hours < 6) {
            todos.add(item("EARLY_BEDTIME", "오늘은 수면 시간을 먼저 확보하기", "어젯밤 수면이 6시간보다 짧았어요.", 1));
            avoidances.add(item("NO_LATE_CAFFEINE", "늦은 시간 카페인 피하기", "오늘 밤 수면 회복을 방해할 수 있어요.", 1));
        }
    }

    private void addSkinRecommendations(
            String value,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (value == null || "GOOD".equals(value)) {
            return;
        }
        todos.add(item("GENTLE_SKINCARE", "순한 보습 위주로 관리하기", "오늘 입력한 피부 상태를 반영했어요.", 2));
        if (List.of("ACNE", "SENSITIVE", "DRY").contains(value)) {
            avoidances.add(item("NO_STRONG_SKINCARE", "강한 각질 제거와 새 제품 피하기", "민감해진 피부에 자극이 될 수 있어요.", 1));
        }
    }

    private void addHydrationRecommendation(String value, List<DiagnosisRecommendationResponse> todos) {
        if ("UNDER_500ML".equals(value) || "500ML".equals(value)) {
            todos.add(item("DRINK_WATER", "물을 나누어 마시기", "최근 수분 섭취량이 적은 편이에요.", 2));
        }
    }

    private String headline(Integer score) {
        if (score == null) return "답한 내용 안에서 오늘의 관리를 정리했어요";
        if (score <= 40) return "오늘은 회복을 우선하는 날이에요";
        if (score <= 60) return "무리 없이 리듬을 유지해요";
        return "좋은 에너지를 필요한 곳에 나눠 써요";
    }

    private String summary(Integer score, boolean premium, int answered, int skipped) {
        String energySummary = score == null
                ? "에너지 문항을 건너뛰어 에너지 수준은 추측하지 않았어요."
                : "오늘의 에너지 " + score + "점에 맞춘 관리 순서를 만들었어요.";
        String base = "응답한 " + answered + "개 항목을 바탕으로 " + energySummary;
        if (!premium) {
            return base;
        }
        return base + " 건너뛴 " + skipped + "개 항목은 추측하지 않고 분석에서 제외했으며, 수면·피부·주간 습관까지 함께 반영했어요.";
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

    private DiagnosisRecommendationResponse item(String code, String title, String reason, int priority) {
        return new DiagnosisRecommendationResponse(code, title, reason, priority);
    }

    private List<QuestionCode> sortedCodes(List<QuestionCode> codes) {
        return codes.stream()
                .sorted(java.util.Comparator.comparingInt(Enum::ordinal))
                .toList();
    }

    private List<DiagnosisRecommendationResponse> limitDistinct(
            List<DiagnosisRecommendationResponse> items,
            int limit
    ) {
        return items.stream()
                .collect(java.util.stream.Collectors.toMap(
                        DiagnosisRecommendationResponse::code,
                        item -> item,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .sorted(java.util.Comparator.comparingInt(DiagnosisRecommendationResponse::priority))
                .limit(limit)
                .toList();
    }
}
