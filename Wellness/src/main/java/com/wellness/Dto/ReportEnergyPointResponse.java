package com.wellness.Dto;

import java.time.LocalDate;

public record ReportEnergyPointResponse(
        LocalDate date,
        Integer score
) {
}
