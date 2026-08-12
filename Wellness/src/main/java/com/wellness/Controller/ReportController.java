package com.wellness.Controller;

import com.wellness.Dto.AuthenticatedUser;
import com.wellness.Dto.WellnessReportResponse;
import com.wellness.Entity.ReportPeriod;
import com.wellness.Service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<WellnessReportResponse> getReport(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "WEEKLY") ReportPeriod period
    ) {
        return ResponseEntity.ok(reportService.getReport(authenticatedUser.userId(), period));
    }
}
