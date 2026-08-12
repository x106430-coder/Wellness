package com.wellness.Dto;

public record ReportManagementRankResponse(
        int rank,
        String managementCode,
        String label,
        long count
) {
}
