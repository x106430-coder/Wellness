package com.wellness.Repository;

import com.wellness.Entity.QuestionAnswer;
import com.wellness.Entity.QuestionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WellnessRepository extends JpaRepository<QuestionAnswer, Long> {

    Optional<QuestionAnswer> findByUserIdAndAnswerDateAndQuestionCode(
            Long userId,
            LocalDate answerDate,
            QuestionCode questionCode
    );

    long countByUserIdAndAnswerDateAndQuestionCodeIn(
            Long userId,
            LocalDate answerDate,
            Collection<QuestionCode> questionCodes
    );

    long countByUserIdAndAnswerDateAndQuestionCodeInAndSkippedFalse(
            Long userId,
            LocalDate answerDate,
            Collection<QuestionCode> questionCodes
    );

    List<QuestionAnswer> findByUserIdAndAnswerDateAndQuestionCodeIn(
            Long userId,
            LocalDate answerDate,
            Collection<QuestionCode> questionCodes
    );

    List<QuestionAnswer> findByUserIdAndAnswerDateBetweenOrderByAnswerDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
