package com.wellness.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "question_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_question_answer_user_date_code",
                columnNames = {"user_id", "answer_date", "question_code"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_code", nullable = false, length = 80)
    private QuestionCode questionCode;

    @Column(name = "answer_value", length = 500)
    private String answerValue;

    @Column(nullable = false)
    private boolean skipped;

    @Column(name = "answer_date", nullable = false)
    private LocalDate answerDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public QuestionAnswer(Long userId, QuestionCode questionCode, String answerValue,
                          boolean skipped, LocalDate answerDate, LocalDateTime now) {
        this.userId = userId;
        this.questionCode = questionCode;
        this.answerDate = answerDate;
        this.createdAt = now;
        update(answerValue, skipped, now);
    }

    public void update(String answerValue, boolean skipped, LocalDateTime now) {
        this.skipped = skipped;
        this.answerValue = skipped ? null : answerValue;
        this.updatedAt = now;
    }
}
