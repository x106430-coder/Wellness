package com.wellness.Service;

import com.wellness.Dto.QuestionAnswerRequest;
import com.wellness.Dto.QuestionAnswerResponse;
import com.wellness.Entiity.QuestionAnswer;
import com.wellness.Repository.WellnessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WellnessService {

    private final WellnessRepository wellnessRepository;
    private final Clock clock;

    @Transactional
    public QuestionAnswerResponse saveOrUpdate(QuestionAnswerRequest request) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        QuestionAnswer answer = wellnessRepository
                .findByUserIdAndAnswerDateAndQuestionCode(request.userId(), today, request.questionCode())
                .map(existing -> {
                    existing.update(request.answerValue(), request.skipped(), now);
                    return existing;
                })
                .orElseGet(() -> new QuestionAnswer(request.userId(), request.questionCode(),
                        request.answerValue(), request.skipped(), today, now));

        return QuestionAnswerResponse.from(wellnessRepository.save(answer));
    }
}
