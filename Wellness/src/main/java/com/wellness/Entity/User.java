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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_email",
                columnNames = "email"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 20)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User(String email, String nickname, Gender gender, Integer age,
                String password, LocalDateTime createdAt) {
        this.email = email;
        this.nickname = nickname;
        this.gender = gender;
        this.age = age;
        this.subscriptionPlan = SubscriptionPlan.FREE;
        this.password = password;
        this.createdAt = createdAt;
    }

    public void changeSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        if (subscriptionPlan == null) {
            throw new IllegalArgumentException("구독 등급은 필수입니다.");
        }
        this.subscriptionPlan = subscriptionPlan;
    }
}
