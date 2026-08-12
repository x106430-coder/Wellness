package com.wellness.Service;

import com.wellness.Dto.BeautyRouteRequest;
import com.wellness.Dto.BeautyRouteResponse;
import com.wellness.Dto.WellnessReportResponse;
import com.wellness.Entity.Gender;
import com.wellness.Entity.ReportPeriod;
import com.wellness.Entity.SubscriptionPlan;
import com.wellness.Entity.User;
import com.wellness.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReportAndBeautyRouteServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private BeautyRouteService beautyRouteService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void freeUserCanViewWeeklyReportButMonthlyReportIsLocked() {
        User user = saveUser("free-report@example.com");

        WellnessReportResponse weekly = reportService.getReport(user.getId(), ReportPeriod.WEEKLY);
        WellnessReportResponse monthly = reportService.getReport(user.getId(), ReportPeriod.MONTHLY);

        assertThat(weekly.locked()).isFalse();
        assertThat(monthly.locked()).isTrue();
        assertThat(monthly.lockMessage()).contains("구독");
    }

    @Test
    void premiumUserCanViewMonthlyReport() {
        User user = saveUser("premium-report@example.com");
        user.changeSubscriptionPlan(SubscriptionPlan.PREMIUM);

        WellnessReportResponse monthly = reportService.getReport(user.getId(), ReportPeriod.MONTHLY);

        assertThat(monthly.locked()).isFalse();
    }

    @Test
    void beautyRouteIsFullyAvailableToFreeUsers() {
        User user = saveUser("free-beauty@example.com");
        BeautyRouteRequest request = new BeautyRouteRequest(
                null,
                "3시간",
                "20~30만원",
                List.of("ELASTICITY"),
                List.of("MOISTURIZING")
        );

        BeautyRouteResponse response = beautyRouteService.recommend(user.getId(), request);

        assertThat(response.premiumDetailLocked()).isFalse();
        assertThat(response.routeSteps()).hasSize(4);
        assertThat(response.reservationStatus()).isEqualTo("COMING_SOON");
    }

    private User saveUser(String email) {
        return userRepository.save(new User(
                email,
                "테스트",
                Gender.OTHER,
                25,
                "encoded",
                LocalDateTime.now()
        ));
    }
}
