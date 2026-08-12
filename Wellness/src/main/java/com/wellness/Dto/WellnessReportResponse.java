package com.wellness.Dto;

import com.wellness.Entity.ReportPeriod;

import java.time.LocalDate;
import java.util.List;

public record WellnessReportResponse(
        ReportPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        String subscriptionPlan,
        boolean locked,
        String lockMessage,
        Integer averageEnergyScore,
        Integer previousPeriodAverageEnergyScore,
        Integer changeFromPreviousPeriod,
        int recoveryDays,
        int sufficientSleepDays,
        List<ReportEnergyPointResponse> energyChart,
        String insight,
        List<ReportManagementRankResponse> managementRanking
) {
    public static WellnessReportResponse locked(
            ReportPeriod period,
            LocalDate startDate,
            LocalDate endDate,
            String subscriptionPlan
    ) {
        return new WellnessReportResponse(
                period,
                startDate,
                endDate,
                subscriptionPlan,
                true,
                "구독하면 월간 에너지 변화와 상세 AI 분석을 확인할 수 있어요.",
                null,
                null,
                null,
                0,
                0,
                List.of(),
                null,
                List.of()
        );
    }
}
