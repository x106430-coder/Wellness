package com.wellness.Repository;

import com.wellness.Entity.ProfileInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ProfileInsightRepository extends JpaRepository<ProfileInsight, Long> {

    Optional<ProfileInsight> findByUserIdAndProfileDateAndLookbackDays(
            Long userId,
            LocalDate profileDate,
            int lookbackDays
    );

    void deleteByUserIdAndProfileDate(Long userId, LocalDate profileDate);
}
