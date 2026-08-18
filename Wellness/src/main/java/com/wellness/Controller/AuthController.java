package com.wellness.Controller;

import com.wellness.Config.JwtProvider;
import com.wellness.Dto.AuthResponse;
import com.wellness.Dto.LoginRequest;
import com.wellness.Dto.SignupRequest;
import com.wellness.Entity.User;
import com.wellness.Service.AuthService;
import com.wellness.Service.LogoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final LogoutService logoutService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        User user = authService.signup(request);

        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail()
        );

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getGender().name(),
                user.getAge(),
                user.getSubscriptionPlan().name(),
                accessToken,
                "Bearer"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        User user = authService.login(request);

        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail()
        );

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getGender().name(),
                user.getAge(),
                user.getSubscriptionPlan().name(),
                accessToken,
                "Bearer"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        if (!authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Bearer 토큰이 필요합니다.");
        }
        logoutService.logout(authorization.substring("Bearer ".length()));
        return ResponseEntity.noContent().build();
    }
}
