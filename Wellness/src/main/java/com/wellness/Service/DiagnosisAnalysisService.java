package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Entity.QuestionAnswer;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.QuestionFrequency;
import com.wellness.Entity.User;
import com.wellness.Repository.UserRepository;
import com.wellness.Repository.WellnessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiagnosisAnalysisService {

    private final WellnessRepository wellnessRepository;
    private final UserRepository userRepository;
    private final QuestionCatalogService questionCatalogService;
    private final DiagnosisAnalysisGenerator diagnosisAnalysisGenerator;
    private final AiCommentService aiCommentService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DiagnosisAnalysisResponse analyze(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<QuestionAnswer> answers = new ArrayList<>();
        answers.addAll(wellnessRepository.findByUserIdAndAnswerDateAndQuestionCodeIn(
                userId, today, questionCatalogService.getCodesByFrequency(QuestionFrequency.DAILY)));
        answers.addAll(wellnessRepository.findByUserIdAndAnswerDateAndQuestionCodeIn(
                userId, weekStart, questionCatalogService.getCodesByFrequency(QuestionFrequency.WEEKLY)));

        Map<QuestionCode, String> answeredValues = new EnumMap<>(QuestionCode.class);
        List<QuestionCode> skippedCodes = new ArrayList<>();
        for (QuestionAnswer answer : answers) {
            if (answer.isSkipped()) {
                skippedCodes.add(answer.getQuestionCode());
            } else {
                answeredValues.put(answer.getQuestionCode(), answer.getAnswerValue());
            }
        }

        int totalQuestions = questionCatalogService.countByFrequency(QuestionFrequency.DAILY)
                + questionCatalogService.countByFrequency(QuestionFrequency.WEEKLY);
        DiagnosisAnalysisContext context = new DiagnosisAnalysisContext(
                today,
                user.getSubscriptionPlan(),
                Map.copyOf(answeredValues),
                List.copyOf(skippedCodes),
                totalQuestions
        );
        DiagnosisAnalysisResponse base = diagnosisAnalysisGenerator.generate(context);
        AiCommentService.AiCommentResult aiComment = aiCommentService.createComment(
                context.answers(), base.energyScore(), base.headline());

        return new DiagnosisAnalysisResponse(
                base.analysisDate(),
                base.subscriptionPlan(),
                base.energyLevel(),
                base.energyScore(),
                aiComment.comment(),
                base.summary(),
                base.todos(),
                base.avoidances(),
                base.usedQuestionCodes(),
                base.skippedQuestionCodes(),
                base.dataCoveragePercent(),
                aiComment.generatedBy()
        );
    }
}
