package com.wellness.Controller;

import com.wellness.Dto.AuthenticatedUser;
import com.wellness.Dto.DiagnosisQuestionsResponse;
import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Dto.QuestionAnswerResponse;
import com.wellness.Service.DiagnosisService;
import com.wellness.Service.DiagnosisAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final DiagnosisAnalysisService diagnosisAnalysisService;

    @GetMapping("/diagnosis/questions/onboarding")
    public ResponseEntity<DiagnosisQuestionsResponse> getOnboardingQuestions() {
        return ResponseEntity.ok(diagnosisService.getOnboardingQuestions());
    }

    @GetMapping("/diagnosis/questions/daily")
    public ResponseEntity<DiagnosisQuestionsResponse> getDailyQuestions() {
        return ResponseEntity.ok(diagnosisService.getDailyQuestions());
    }

    @GetMapping("/diagnosis/questions/weekly")
    public ResponseEntity<DiagnosisQuestionsResponse> getWeeklyQuestions() {
        return ResponseEntity.ok(diagnosisService.getWeeklyQuestions());
    }

    @PostMapping({"/diagnosis/answers", "/question-answers"})
    public ResponseEntity<QuestionAnswerResponse> saveAnswer(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody QuestionAnswerRequest request
    ) {
        return ResponseEntity.ok(diagnosisService.saveOrUpdate(authenticatedUser.userId(), request));
    }

    @PostMapping("/diagnosis/analysis")
    public ResponseEntity<DiagnosisAnalysisResponse> analyze(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(diagnosisAnalysisService.analyze(authenticatedUser.userId()));
    }
}
