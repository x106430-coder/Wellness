package com.wellness.Service;

import com.wellness.Dto.ReportEnergyPointResponse;
import com.wellness.Dto.ReportManagementRankResponse;
import com.wellness.Dto.WellnessReportResponse;
import com.wellness.Entity.QuestionAnswer;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.ReportPeriod;
import com.wellness.Entity.SubscriptionPlan;
import com.wellness.Entity.User;
import com.wellness.Repository.UserRepository;
import com.wellness.Repository.WellnessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Map<String, String> MANAGEMENT_LABELS = managementLabels();

    private final WellnessRepository wellnessRepository;
    private final UserRepository userRepository;
    private final EnergyScoreCalculator energyScoreCalculator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public WellnessReportResponse getReport(Long userId, ReportPeriod period) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        DateRange current = resolveCurrentRange(period);
        if (period == ReportPeriod.MONTHLY && user.getSubscriptionPlan() != SubscriptionPlan.PREMIUM) {
            return WellnessReportResponse.locked(
                    period, current.startDate(), current.endDate(), user.getSubscriptionPlan().name());
        }

        DateRange previous = previousRange(current);
        List<QuestionAnswer> answers = wellnessRepository
                .findByUserIdAndAnswerDateBetweenOrderByAnswerDateAsc(
                        userId, current.startDate(), current.endDate());
        List<QuestionAnswer> previousAnswers = wellnessRepository
                .findByUserIdAndAnswerDateBetweenOrderByAnswerDateAsc(
                        userId, previous.startDate(), previous.endDate());

        List<ReportEnergyPointResponse> chart = energyChart(answers);
        Integer average = averageScore(chart);
        Integer previousAverage = averageScore(energyChart(previousAnswers));
        Integer change = average != null && previousAverage != null ? average - previousAverage : null;
        int recoveryDays = (int) chart.stream()
                .filter(point -> point.score() != null && point.score() >= 60)
                .count();
        int sufficientSleepDays = countSufficientSleepDays(answers);
        List<ReportManagementRankResponse> ranking = managementRanking(answers);

        return new WellnessReportResponse(
                period,
                current.startDate(),
                current.endDate(),
                user.getSubscriptionPlan().name(),
                false,
                null,
                average,
                previousAverage,
                change,
                recoveryDays,
                sufficientSleepDays,
                chart,
                createInsight(average, change, sufficientSleepDays, chart.size()),
                ranking
        );
    }

    private DateRange resolveCurrentRange(ReportPeriod period) {
        LocalDate today = LocalDate.now(clock);
        if (period == ReportPeriod.MONTHLY) {
            return new DateRange(today.with(TemporalAdjusters.firstDayOfMonth()), today);
        }
        return new DateRange(today.minusDays(6), today);
    }

    private DateRange previousRange(DateRange current) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                current.startDate(), current.endDate()) + 1;
        LocalDate previousEnd = current.startDate().minusDays(1);
        return new DateRange(previousEnd.minusDays(days - 1), previousEnd);
    }

    private List<ReportEnergyPointResponse> energyChart(List<QuestionAnswer> answers) {
        Map<LocalDate, Map<QuestionCode, String>> answersByDate = new LinkedHashMap<>();
        answers.stream()
                .filter(answer -> !answer.isSkipped())
                .forEach(answer -> answersByDate
                        .computeIfAbsent(answer.getAnswerDate(), ignored -> new java.util.EnumMap<>(QuestionCode.class))
                        .put(answer.getQuestionCode(), answer.getAnswerValue()));

        return answersByDate.entrySet().stream()
                .map(entry -> new ReportEnergyPointResponse(
                        entry.getKey(), energyScoreCalculator.calculate(entry.getValue())))
                .filter(point -> point.score() != null)
                .toList();
    }

    private Integer averageScore(List<ReportEnergyPointResponse> chart) {
        if (chart.isEmpty()) {
            return null;
        }
        return (int) Math.round(chart.stream()
                .mapToInt(ReportEnergyPointResponse::score)
                .average()
                .orElse(0));
    }

    private int countSufficientSleepDays(List<QuestionAnswer> answers) {
        return (int) answers.stream()
                .filter(answer -> !answer.isSkipped())
                .filter(answer -> answer.getQuestionCode() == QuestionCode.LAST_NIGHT_SLEEP)
                .map(QuestionAnswer::getAnswerValue)
                .map(this::parseSleepHours)
                .filter(hours -> hours != null && hours >= 7)
                .count();
    }

    private List<ReportManagementRankResponse> managementRanking(List<QuestionAnswer> answers) {
        Map<String, Long> counts = new HashMap<>();
        answers.stream()
                .filter(answer -> !answer.isSkipped())
                .filter(answer -> answer.getQuestionCode() == QuestionCode.TODAY_PLANNED_CARE)
                .map(QuestionAnswer::getAnswerValue)
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> List.of(value.split("\\|")).stream())
                .forEach(value -> counts.merge(value, 1L, Long::sum));

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey));

        List<ReportManagementRankResponse> result = new ArrayList<>();
        for (int index = 0; index < Math.min(3, sorted.size()); index++) {
            Map.Entry<String, Long> entry = sorted.get(index);
            result.add(new ReportManagementRankResponse(
                    index + 1,
                    entry.getKey(),
                    MANAGEMENT_LABELS.getOrDefault(entry.getKey(), entry.getKey()),
                    entry.getValue()
            ));
        }
        return List.copyOf(result);
    }

    private String createInsight(Integer average, Integer change, int sleepDays, int measuredDays) {
        if (average == null) {
            return "아직 에너지 기록이 부족해요. 오늘의 진단부터 시작해보세요.";
        }
        if (sleepDays >= Math.max(1, measuredDays / 2) && average >= 60) {
            return "충분히 잔 날이 많았고 에너지도 안정적이었어요. 현재 수면 리듬을 이어가 보세요.";
        }
        if (change != null && change > 0) {
            return "이전 기간보다 에너지가 좋아지고 있어요. 부담 없는 관리를 꾸준히 이어가 보세요.";
        }
        if (average < 50) {
            return "에너지가 낮은 날이 많았어요. 다음 기간에는 수면과 회복 시간을 먼저 확보해보세요.";
        }
        return "에너지가 비교적 안정적이에요. 무리하지 않는 범위에서 현재의 관리 리듬을 유지해보세요.";
    }

    private Double parseSleepHours(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
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

    private static Map<String, String> managementLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("WORKOUT", "운동");
        labels.put("SKINCARE", "스킨케어");
        labels.put("DIET", "식단 관리");
        labels.put("SUPPLEMENT", "영양제");
        labels.put("MEDITATION", "명상·마음 관리");
        labels.put("STRETCHING", "스트레칭");
        labels.put("SLEEP_EARLY", "일찍 자기");
        labels.put("OTHER", "기타");
        return Map.copyOf(labels);
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
