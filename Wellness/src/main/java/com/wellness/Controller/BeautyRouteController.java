package com.wellness.Controller;

import com.wellness.Dto.AuthenticatedUser;
import com.wellness.Dto.BeautyRouteRequest;
import com.wellness.Dto.BeautyRouteResponse;
import com.wellness.Service.BeautyRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/beauty-routes")
@RequiredArgsConstructor
public class BeautyRouteController {

    private final BeautyRouteService beautyRouteService;

    @PostMapping("/recommendation")
    public ResponseEntity<BeautyRouteResponse> recommend(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody BeautyRouteRequest request
    ) {
        return ResponseEntity.ok(
                beautyRouteService.recommend(authenticatedUser.userId(), request));
    }
}
