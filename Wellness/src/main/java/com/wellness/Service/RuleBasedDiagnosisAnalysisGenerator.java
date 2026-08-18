package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Dto.DiagnosisRecommendationResponse;
import com.wellness.Entity.QuestionCode;
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

        List<DiagnosisRecommendationResponse> todos = new ArrayList<>();
        List<DiagnosisRecommendationResponse> avoidances = new ArrayList<>();

        if (energyScore != null) {
            addEnergyRecommendations(energyScore, todos, avoidances);
        }
        addAvailableTimeRecommendation(answers.get(QuestionCode.CARE_AVAILABLE_TIME), todos, avoidances);
        addPlannedCareRecommendations(
                answers.get(QuestionCode.TODAY_PLANNED_CARE), energyScore, todos, avoidances);
        addSleepRecommendations(answers.get(QuestionCode.LAST_NIGHT_SLEEP), todos, avoidances);
        addSkinRecommendations(answers.get(QuestionCode.SKIN_STATUS), todos, avoidances);
        addHardestMomentRecommendations(answers.get(QuestionCode.HARDEST_MOMENT), todos, avoidances);
        addExerciseHabitRecommendations(answers, energyScore, todos, avoidances);
        addSupplementRecommendations(answers.get(QuestionCode.SUPPLEMENTS), todos, avoidances);
        addHydrationRecommendation(answers.get(QuestionCode.WATER_INTAKE), todos);
        addSkinProfileRecommendations(
                answers.get(QuestionCode.SKIN_TYPE),
                answers.get(QuestionCode.SKIN_CONCERN),
                todos,
                avoidances
        );
        addSkincareFrequencyRecommendations(
                answers.get(QuestionCode.SKINCARE_FREQUENCY), todos, avoidances);

        List<DiagnosisRecommendationResponse> selectedTodos = limitDistinct(todos, 8);
        List<DiagnosisRecommendationResponse> selectedAvoidances = limitDistinct(avoidances, 7);
        int coverage = context.totalQuestionCount() == 0 ? 0
                : (int) Math.round(context.answers().size() * 100.0 / context.totalQuestionCount());

        return new DiagnosisAnalysisResponse(
                context.analysisDate(),
                context.subscriptionPlan().name(),
                energyLevel,
                energyScore,
                headline(energyScore),
                summary(energyScore, context.answers().size(), context.skippedQuestionCodes().size()),
                summary(energyScore, context.answers().size(), context.skippedQuestionCodes().size()),
                "오늘의 답변을 바탕으로 무리하지 않는 관리 순서를 만들었어요.",
                "오늘의 Not Today 항목은 현재 에너지와 답변한 상태를 바탕으로 부담을 덜기 위해 걷어냈어요.",
                "답변한 생활 습관을 바탕으로 회복 중심 관리가 잘 맞는 편이에요.",
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

    private void addAvailableTimeRecommendation(
            String value,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (value == null) {
            return;
        }
        if ("NONE".equals(value)) {
            todos.add(item("MICRO_RECOVERY", "1분만 호흡하며 쉬기", "관리 시간이 없어도 짧은 회복은 가능해요.", 2));
            avoidances.add(item("NO_FORCED_ROUTINE", "긴 관리 루틴 억지로 넣지 않기", "오늘 가능한 시간이 없다고 답했어요.", 1));
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

    private void addPlannedCareRecommendations(
            String value,
            Integer energyScore,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
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
            if (energyScore != null && energyScore <= 40) {
                avoidances.add(item("NO_PLANNED_INTENSE_WORKOUT", "계획한 운동을 고강도로 하지 않기", "낮은 에너지 상태를 반영했어요.", 1));
            }
        }
        if (plans.contains("DIET")) {
            todos.add(item("PLANNED_DIET", "규칙적인 한 끼 챙기기", "오늘 계획한 식단 관리를 무리 없는 식사로 반영했어요.", 3));
            if (energyScore != null && energyScore <= 40) {
                avoidances.add(item("NO_EXTREME_DIET", "굶거나 식사량을 갑자기 줄이지 않기", "회복이 필요한 날에는 충분한 에너지가 필요해요.", 1));
            }
        }
        if (plans.contains("SUPPLEMENT")) {
            todos.add(item("PLANNED_SUPPLEMENT", "평소 먹던 영양제만 정량 챙기기", "직접 선택한 계획을 안전한 범위로 반영했어요.", 3));
        }
        if (plans.contains("SLEEP_EARLY")) {
            todos.add(item("PLANNED_SLEEP", "평소보다 일찍 잠들 준비하기", "직접 선택한 회복 계획을 우선 반영했어요.", 2));
        }
        if (plans.contains("MEDITATION")) {
            todos.add(item("PLANNED_MINDFULNESS", "짧게 마음 관리하기", "부담이 적고 에너지 회복에 도움이 되는 계획이에요.", 3));
        }
        if (plans.contains("STRETCHING")) {
            todos.add(item("PLANNED_STRETCHING", "통증 없는 범위에서 스트레칭하기", "계획한 관리를 가볍게 실천할 수 있어요.", 3));
        }
        if (plans.contains("READING")) {
            todos.add(item("PLANNED_READING", "짧게 읽고 눈을 쉬게 하기", "집중력에 맞춰 짧은 독서로 조절했어요.", 4));
        }
        if (plans.contains("OTHER")) {
            todos.add(item("PLANNED_OTHER", "가장 부담이 적은 계획 하나만 고르기", "세부 내용을 알 수 없어 무리하지 않는 범위로 제안해요.", 4));
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
        } else if (hours != null && hours > 10) {
            todos.add(item("RESTORE_SLEEP_RHYTHM", "오늘은 일정한 시간에 잠들기", "어젯밤 수면 시간이 길어 생활 리듬을 일정하게 맞춰보세요.", 2));
            avoidances.add(item("NO_LONG_NAP", "늦은 오후에 오래 낮잠 자지 않기", "오늘 밤 수면 리듬이 다시 밀릴 수 있어요.", 2));
        }
    }

    private void addSkinRecommendations(
            String value,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (value == null) {
            return;
        }
        if ("GOOD".equals(value)) {
            todos.add(item("MAINTAIN_SKINCARE", "평소의 순한 스킨케어 유지하기", "오늘 피부 상태가 좋다고 답했어요.", 4));
            return;
        }
        todos.add(item("GENTLE_SKINCARE", "순한 보습 위주로 관리하기", "오늘 입력한 피부 상태를 반영했어요.", 2));
        if (List.of("ACNE", "SENSITIVE", "DRY").contains(value)) {
            avoidances.add(item("NO_STRONG_SKINCARE", "강한 각질 제거와 새 제품 피하기", "민감해진 피부에 자극이 될 수 있어요.", 1));
        }
    }

    private void addHardestMomentRecommendations(
            String value,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (value == null) return;
        switch (value) {
            case "WORK_STUDY" -> {
                todos.add(item("WORK_BREAK", "할 일을 작게 나누고 중간에 쉬기", "업무·학업이 가장 힘들었다고 답했어요.", 2));
                avoidances.add(item("NO_OVERTIME", "오늘 할 일을 더 늘리지 않기", "집중력이 떨어진 상태에서 무리하지 않도록 조절했어요.", 2));
            }
            case "RELATIONSHIP" -> {
                todos.add(item("RELATIONSHIP_PAUSE", "감정을 정리할 시간을 먼저 갖기", "대인관계로 소모된 에너지를 회복하는 게 우선이에요.", 2));
                avoidances.add(item("NO_IMMEDIATE_REPLY", "감정적인 답장을 바로 보내지 않기", "잠시 멈추면 불필요한 갈등을 줄일 수 있어요.", 2));
            }
            case "HEALTH" -> {
                todos.add(item("HEALTH_RECOVERY", "몸 상태를 살피며 충분히 쉬기", "건강 문제가 가장 힘들었다고 답했어요.", 1));
                avoidances.add(item("NO_IGNORE_SYMPTOMS", "불편한 증상을 참고 무리하지 않기", "증상이 계속되거나 심하면 전문가에게 확인하는 것이 좋아요.", 1));
            }
            case "EMOTION" -> {
                todos.add(item("EMOTIONAL_REST", "조용한 회복 시간을 확보하기", "감정 소모가 컸던 날에는 자극을 줄이는 게 좋아요.", 1));
                avoidances.add(item("NO_BIG_DECISION", "지친 상태에서 큰 결정을 서두르지 않기", "감정이 가라앉은 뒤 다시 판단해도 늦지 않아요.", 2));
            }
            default -> {
                todos.add(item("SIMPLE_RECOVERY", "오늘 부담을 한 가지 줄이기", "기타 어려움의 세부 내용은 추측하지 않고 회복 중심으로 제안해요.", 3));
                avoidances.add(item("NO_EXTRA_COMMITMENT", "새로운 약속을 추가하지 않기", "현재 부담이 더 커지지 않도록 조절했어요.", 3));
            }
        }
    }

    private void addExerciseHabitRecommendations(
            Map<QuestionCode, String> answers,
            Integer energyScore,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        addExerciseHabit("근력운동", "STRENGTH", answers.get(QuestionCode.STRENGTH_TRAINING), energyScore, todos, avoidances);
        addExerciseHabit("유산소", "CARDIO", answers.get(QuestionCode.CARDIO), energyScore, todos, avoidances);
        addExerciseHabit("스트레칭", "STRETCH", answers.get(QuestionCode.STRETCHING), energyScore, todos, avoidances);
    }

    private void addExerciseHabit(
            String label,
            String codePrefix,
            String frequency,
            Integer energyScore,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (frequency == null) return;
        if ("ZERO".equals(frequency)) {
            String action = "STRETCH".equals(codePrefix) ? "5분 스트레칭부터 시작하기" : label + "을 10분 가볍게 시작하기";
            todos.add(item("START_" + codePrefix, action, "최근 " + label + " 횟수가 0회라고 답했어요.", 4));
        } else if ("FOUR_PLUS".equals(frequency)) {
            todos.add(item(codePrefix + "_RECOVERY", label + " 후 회복 시간 챙기기", "주 4회 이상 습관에는 회복도 함께 필요해요.", 3));
            avoidances.add(item("NO_CONSECUTIVE_" + codePrefix, label + " 고강도 일정을 연속으로 잡지 않기", "반복되는 부하로 피로가 쌓이지 않게 조절해요.", 3));
        }
        if (energyScore != null && energyScore <= 40 && !"ZERO".equals(frequency)) {
            avoidances.add(item("NO_INTENSE_" + codePrefix + "_TODAY", "오늘 " + label + " 강도를 높이지 않기", "평소 운동 습관과 오늘의 낮은 에너지를 함께 반영했어요.", 1));
        }
    }

    private void addSupplementRecommendations(
            String value,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (value == null) return;
        List<String> supplements = List.of(value.split("\\|"));
        if (supplements.contains("NONE")) {
            todos.add(item("NUTRIENTS_FROM_MEALS", "오늘은 식사로 영양을 챙기기", "영양제를 먹지 않는다고 답했어요.", 4));
            avoidances.add(item("NO_RANDOM_SUPPLEMENT", "필요를 확인하지 않고 영양제를 시작하지 않기", "새 영양제는 개인 상태와 복용 정보를 확인한 뒤 선택하는 게 좋아요.", 4));
            return;
        }
        todos.add(item("USUAL_SUPPLEMENT", "평소 복용하던 영양제만 정량 챙기기", "입력한 영양제 습관을 반영했어요.", 4));
        avoidances.add(item("NO_DOUBLE_SUPPLEMENT", "놓친 영양제를 한꺼번에 두 배로 먹지 않기", "복용량을 임의로 늘리지 않도록 안내해요.", 3));
    }

    private void addHydrationRecommendation(String value, List<DiagnosisRecommendationResponse> todos) {
        if ("UNDER_500ML".equals(value) || "500ML".equals(value)) {
            todos.add(item("DRINK_WATER", "물을 나누어 마시기", "최근 수분 섭취량이 적은 편이에요.", 2));
        } else if ("1L".equals(value)) {
            todos.add(item("MAINTAIN_WATER", "오늘도 물을 일정하게 나누어 마시기", "최근 수분 섭취 습관을 유지하도록 반영했어요.", 4));
        } else if ("1_5L_PLUS".equals(value)) {
            todos.add(item("KEEP_HYDRATION", "갈증과 활동량에 맞춰 수분 유지하기", "최근 충분히 물을 마시는 습관을 반영했어요.", 5));
        }
    }

    private void addSkinProfileRecommendations(
            String skinType,
            String concerns,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (skinType != null) {
            switch (skinType) {
                case "DRY" -> {
                    todos.add(item("DRY_SKIN_MOISTURE", "세안 후 바로 보습하기", "평소 건성 피부라고 답했어요.", 2));
                    avoidances.add(item("NO_STRIPPING_CLEANSER", "세정력이 강한 제품 피하기", "건성 피부의 당김을 키울 수 있어요.", 2));
                }
                case "OILY" -> {
                    todos.add(item("OILY_SKIN_BALANCE", "가벼운 보습으로 유수분 맞추기", "평소 지성 피부라고 답했어요.", 2));
                    avoidances.add(item("NO_OVER_CLEANSING", "피지를 없애려고 과하게 세안하지 않기", "과도한 세안은 피부 장벽을 자극할 수 있어요.", 2));
                }
                case "COMBINATION" -> todos.add(item("COMBINATION_CARE", "부위별로 보습량 조절하기", "복합성 피부 특성을 반영했어요.", 3));
                case "UNKNOWN" -> avoidances.add(item("NO_RANDOM_ACTIVE", "피부 타입을 모른 채 강한 기능성 제품을 겹치지 않기", "먼저 순한 기본 관리로 반응을 살펴보세요.", 2));
                default -> { }
            }
        }
        if (concerns == null) return;
        List<String> values = List.of(concerns.split("\\|"));
        if (values.contains("DRYNESS")) todos.add(item("CONCERN_DRYNESS", "건조한 부위에 보습 덧바르기", "선택한 피부 고민 중 건조함을 반영했어요.", 2));
        if (values.contains("ACNE")) avoidances.add(item("NO_TOUCH_ACNE", "트러블을 손으로 만지거나 짜지 않기", "선택한 여드름 고민을 반영했어요.", 1));
        if (values.contains("SENSITIVE")) avoidances.add(item("NO_NEW_PRODUCT", "새 화장품을 여러 개 동시에 시험하지 않기", "민감함이 있을 때 원인 확인이 어려워질 수 있어요.", 1));
        if (values.contains("SEBUM") || values.contains("PORES")) todos.add(item("GENTLE_SEBUM_CARE", "순한 세안 후 가볍게 보습하기", "피지·모공 고민을 자극 없는 기본 관리로 반영했어요.", 3));
        if (values.contains("ELASTICITY")) todos.add(item("ELASTICITY_SUNCARE", "낮에는 자외선 차단 챙기기", "탄력 고민에는 꾸준한 기본 관리가 중요해요.", 3));
    }

    private void addSkincareFrequencyRecommendations(
            String value,
            List<DiagnosisRecommendationResponse> todos,
            List<DiagnosisRecommendationResponse> avoidances
    ) {
        if (value == null) return;
        switch (value) {
            case "ZERO", "MONTHLY_ONE" -> todos.add(item("BASIC_HOME_SKINCARE", "세안·보습·자외선 차단부터 꾸준히 하기", "최근 피부관리 빈도에 맞춰 기본 루틴부터 제안해요.", 4));
            case "MONTHLY_TWO_THREE" -> todos.add(item("SKINCARE_RECOVERY_GAP", "피부관리 사이에 회복 기간 두기", "월 2~3회 관리 습관을 반영했어요.", 3));
            case "WEEKLY_ONE_PLUS" -> avoidances.add(item("NO_STACKED_TREATMENT", "피부 시술과 강한 홈케어를 같은 날 겹치지 않기", "주 1회 이상 관리할 때는 자극이 누적되지 않게 조절해야 해요.", 2));
            default -> { }
        }
    }

    private String headline(Integer score) {
        if (score == null) return "답한 내용 안에서 오늘의 관리를 정리했어요";
        if (score <= 40) return "오늘은 회복을 우선하는 날이에요";
        if (score <= 60) return "무리 없이 리듬을 유지해요";
        return "좋은 에너지를 필요한 곳에 나눠 써요";
    }

    private String summary(Integer score, int answered, int skipped) {
        String energySummary = score == null
                ? "에너지 문항을 건너뛰어 에너지 수준은 추측하지 않았어요."
                : "오늘의 에너지 " + score + "점에 맞춘 관리 순서를 만들었어요.";
        String base = "응답한 " + answered + "개 항목을 바탕으로 " + energySummary;
        return base + " 건너뛴 " + skipped
                + "개 항목은 추측하지 않고 분석에서 제외했으며, 답변한 수면·피부·생활 습관을 함께 반영했어요.";
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
