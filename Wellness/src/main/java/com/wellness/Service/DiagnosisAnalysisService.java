package com.wellness.Service;

import com.wellness.Dto.DiagnosisAnalysisResponse;
import com.wellness.Entity.DiagnosisAnalysisResult;
import com.wellness.Entity.QuestionAnswer;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.QuestionFrequency;
import com.wellness.Entity.User;
import com.wellness.Repository.DiagnosisAnalysisResultRepository;
import com.wellness.Repository.UserRepository;
import com.wellness.Repository.WellnessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class DiagnosisAnalysisService {

    private final WellnessRepository wellnessRepository;
    private final DiagnosisAnalysisResultRepository diagnosisAnalysisResultRepository;
    private final UserRepository userRepository;
    private final QuestionCatalogService questionCatalogService;
    private final DiagnosisAnalysisGenerator diagnosisAnalysisGenerator;
    private final AiCommentService aiCommentService;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Transactional
    public DiagnosisAnalysisResponse analyze(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        LocalDate today = LocalDate.now(clock);
        DiagnosisAnalysisResult cached = diagnosisAnalysisResultRepository
                .findByUserIdAndAnalysisDate(userId, today)
                .orElse(null);
        if (cached != null) {
            DiagnosisAnalysisResponse cachedResponse = deserialize(cached.getResponseJson());
            if (cachedResponse.routeJudgmentComment() != null
                    && !cachedResponse.routeJudgmentComment().isBlank()) {
                return cachedResponse;
            }
            diagnosisAnalysisResultRepository.delete(cached);
            diagnosisAnalysisResultRepository.flush();
        }

        int dailyQuestionCount = questionCatalogService.countByFrequency(QuestionFrequency.DAILY);
        long savedDailyQuestionCount = wellnessRepository
                .countByUserIdAndAnswerDateAndQuestionCodeIn(
                        userId,
                        today,
                        questionCatalogService.getCodesByFrequency(QuestionFrequency.DAILY)
                );
        if (savedDailyQuestionCount < dailyQuestionCount) {
            throw new IllegalArgumentException("오늘의 일일 진단을 완료해주세요.");
        }
        long answeredDailyQuestionCount = wellnessRepository
                .countByUserIdAndAnswerDateAndQuestionCodeInAndSkippedFalse(
                        userId,
                        today,
                        questionCatalogService.getCodesByFrequency(QuestionFrequency.DAILY)
                );
        if (answeredDailyQuestionCount == 0) {
            throw new IllegalArgumentException("분석할 답변이 없습니다. 오늘의 진단을 다시 진행해주세요.");
        }

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
        AiCommentService.AiScreenCommentResult aiComment = aiCommentService.createScreenComments(
                context.answers(),
                base.energyScore(),
                base.headline(),
                base.summary(),
                base.todos(),
                base.avoidances()
        );

        DiagnosisAnalysisResponse response = new DiagnosisAnalysisResponse(
                base.analysisDate(),
                base.subscriptionPlan(),
                base.energyLevel(),
                base.energyScore(),
                aiComment.homeComment(),
                base.summary(),
                aiComment.diagnosisComment(),
                aiComment.routeComment(),
                aiComment.routeJudgmentComment(),
                aiComment.profileComment(),
                base.todos(),
                base.avoidances(),
                base.usedQuestionCodes(),
                base.skippedQuestionCodes(),
                base.dataCoveragePercent(),
                aiComment.generatedBy()
        );
        diagnosisAnalysisResultRepository.save(new DiagnosisAnalysisResult(
                userId,
                today,
                serialize(response),
                LocalDateTime.now(clock)
        ));
        return response;
    }

    @Transactional(readOnly = true)
    public DiagnosisAnalysisResponse getTodayAnalysis(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        LocalDate today = LocalDate.now(clock);
        List<QuestionCode> dailyCodes = questionCatalogService
                .getCodesByFrequency(QuestionFrequency.DAILY);
        long savedDailyQuestionCount = wellnessRepository
                .countByUserIdAndAnswerDateAndQuestionCodeIn(userId, today, dailyCodes);
        long answeredDailyQuestionCount = wellnessRepository
                .countByUserIdAndAnswerDateAndQuestionCodeInAndSkippedFalse(
                        userId, today, dailyCodes);

        if (savedDailyQuestionCount < dailyCodes.size() || answeredDailyQuestionCount == 0) {
            return null;
        }

        return diagnosisAnalysisResultRepository
                .findByUserIdAndAnalysisDate(userId, today)
                .map(DiagnosisAnalysisResult::getResponseJson)
                .map(this::deserialize)
                .orElse(null);
    }

    private String serialize(DiagnosisAnalysisResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new IllegalStateException("진단 분석 결과를 저장하지 못했습니다.", exception);
        }
    }

    private DiagnosisAnalysisResponse deserialize(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, DiagnosisAnalysisResponse.class);
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 진단 분석 결과를 읽지 못했습니다.", exception);
        }
    }
}
