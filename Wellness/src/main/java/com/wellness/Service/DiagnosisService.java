package com.wellness.Service;

import com.wellness.Dto.DiagnosisQuestionsResponse;
import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Dto.QuestionAnswerResponse;
import com.wellness.Entity.QuestionAnswer;
import com.wellness.Entity.QuestionCode;
import com.wellness.Entity.QuestionFrequency;
import com.wellness.Repository.WellnessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final WellnessRepository wellnessRepository;
    private final QuestionCatalogService questionCatalogService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DiagnosisQuestionsResponse getOnboardingQuestions() {
        return new DiagnosisQuestionsResponse(
                "ONBOARDING",
                questionCatalogService.getOnboardingQuestions()
        );
    }

    @Transactional(readOnly = true)
    public DiagnosisQuestionsResponse getDailyQuestions() {
        return new DiagnosisQuestionsResponse(
                "DAILY",
                questionCatalogService.getQuestionsByFrequency(QuestionFrequency.DAILY)
        );
    }

    @Transactional(readOnly = true)
    public DiagnosisQuestionsResponse getWeeklyQuestions() {
        return new DiagnosisQuestionsResponse(
                "WEEKLY",
                questionCatalogService.getQuestionsByFrequency(QuestionFrequency.WEEKLY)
        );
    }

    @Transactional
    public QuestionAnswerResponse saveOrUpdate(
            Long userId,
            QuestionAnswerRequest request
    ) {
        LocalDate answerDate = resolveAnswerDate(request.questionCode());
        LocalDateTime now = LocalDateTime.now(clock);

        QuestionAnswer answer = wellnessRepository
                .findByUserIdAndAnswerDateAndQuestionCode(
                        userId,
                        answerDate,
                        request.questionCode()
                )
                .map(existing -> {
                    existing.update(
                            request.normalizedAnswerValue(),
                            request.skipped(),
                            now
                    );
                    return existing;
                })
                .orElseGet(() -> new QuestionAnswer(
                        userId,
                        request.questionCode(),
                        request.normalizedAnswerValue(),
                        request.skipped(),
                        answerDate,
                        now
                ));

        return QuestionAnswerResponse.from(
                wellnessRepository.save(answer)
        );
    }

    @Transactional(readOnly = true)
    public int countAnsweredQuestions(
            Long userId,
            QuestionFrequency frequency
    ) {
        List<QuestionCode> codes =
                questionCatalogService.getCodesByFrequency(frequency);

        LocalDate answerDate = resolveAnswerDate(frequency);

        return (int) wellnessRepository
                .countByUserIdAndAnswerDateAndQuestionCodeIn(
                        userId,
                        answerDate,
                        codes
                );
    }

    // 홈에서 오늘의 진단 답변을 가져오기 위한 메서드
    @Transactional(readOnly = true)
    public List<QuestionAnswer> getTodayDailyAnswers(Long userId) {

        List<QuestionCode> codes =
                questionCatalogService.getCodesByFrequency(
                        QuestionFrequency.DAILY
                );

        LocalDate answerDate =
                resolveAnswerDate(QuestionFrequency.DAILY);

        return wellnessRepository
                .findByUserIdAndAnswerDateAndQuestionCodeIn(
                        userId,
                        answerDate,
                        codes
                );
    }

    private LocalDate resolveAnswerDate(QuestionCode questionCode) {
        return resolveAnswerDate(
                questionCatalogService.getFrequency(questionCode)
        );
    }

    private LocalDate resolveAnswerDate(
            QuestionFrequency frequency
    ) {
        LocalDate today = LocalDate.now(clock);

        if (frequency == QuestionFrequency.WEEKLY) {
            return today.with(
                    TemporalAdjusters.previousOrSame(
                            java.time.DayOfWeek.MONDAY
                    )
            );
        }

        return today;
    }
}