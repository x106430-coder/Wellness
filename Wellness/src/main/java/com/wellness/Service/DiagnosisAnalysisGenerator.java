package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;

/**
 * AI 공급자가 정해지면 이 인터페이스의 구현체만 교체하면 됩니다.
 */
public interface DiagnosisAnalysisGenerator {
    DiagnosisAnalysisResponse generate(DiagnosisAnalysisContext context);
}
