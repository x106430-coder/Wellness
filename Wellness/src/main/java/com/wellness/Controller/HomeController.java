package com.wellness.Controller;

import com.wellness.Dto.AuthenticatedUser;
import com.wellness.Dto.HomeSummaryResponse;
import com.wellness.Service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/summary")
    public ResponseEntity<HomeSummaryResponse> getHomeSummary(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(homeService.getHomeSummary(authenticatedUser.userId()));
    }
}
