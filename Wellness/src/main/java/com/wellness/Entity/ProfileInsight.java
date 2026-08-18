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
        name = "profile_insights",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_profile_insight_user_date_days",
                columnNames = {"user_id", "profile_date", "lookback_days"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "profile_date", nullable = false)
    private LocalDate profileDate;

    @Column(name = "lookback_days", nullable = false)
    private int lookbackDays;

    @Column(name = "source_count", nullable = false)
    private int sourceCount;

    @Column(name = "comment", nullable = false, length = 500)
    private String comment;

    @Column(name = "generated_by", nullable = false, length = 40)
    private String generatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ProfileInsight(
            Long userId,
            LocalDate profileDate,
            int lookbackDays,
            int sourceCount,
            String comment,
            String generatedBy,
            LocalDateTime createdAt
    ) {
        this.userId = userId;
        this.profileDate = profileDate;
        this.lookbackDays = lookbackDays;
        this.sourceCount = sourceCount;
        this.comment = comment;
        this.generatedBy = generatedBy;
        this.createdAt = createdAt;
    }
}
