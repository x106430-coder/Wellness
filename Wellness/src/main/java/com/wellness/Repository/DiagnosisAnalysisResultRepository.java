package com.wellness.Repository;

import com.wellness.Entity.DiagnosisAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface DiagnosisAnalysisResultRepository
        extends JpaRepository<DiagnosisAnalysisResult, Long> {

    Optional<DiagnosisAnalysisResult> findByUserIdAndAnalysisDate(
            Long userId,
            LocalDate analysisDate
    );

    void deleteByUserIdAndAnalysisDate(Long userId, LocalDate analysisDate);

    List<DiagnosisAnalysisResult> findByUserIdAndAnalysisDateBetweenOrderByAnalysisDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
