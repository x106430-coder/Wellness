package com.wellness.Controller;

import com.wellness.Dto.AuthenticatedUser;
import com.wellness.Dto.ProfileSummaryResponse;
import com.wellness.Service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/summary")
    public ResponseEntity<ProfileSummaryResponse> getSummary(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(profileService.getSummary(authenticatedUser.userId()));
    }
}
