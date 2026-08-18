package com.wellness.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "diagnosis_analysis_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_diagnosis_analysis_user_date",
                columnNames = {"user_id", "analysis_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;

    @Column(name = "response_json", nullable = false, columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DiagnosisAnalysisResult(
            Long userId,
            LocalDate analysisDate,
            String responseJson,
            LocalDateTime now
    ) {
        this.userId = userId;
        this.analysisDate = analysisDate;
        this.responseJson = responseJson;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
