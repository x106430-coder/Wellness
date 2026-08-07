package com.wellness.Controller;

import com.wellness.Config.JwtProvider;
import com.wellness.Dto.AuthResponse;
import com.wellness.Dto.LoginRequest;
import com.wellness.Dto.SignupRequest;
import com.wellness.Entity.User;
import com.wellness.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

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
                accessToken,
                "Bearer"
        );

        return ResponseEntity.ok(response);
    }
}
