package com.wellness.Service;

import com.wellness.Dto.LoginRequest;
import com.wellness.Dto.SignupRequest;
import com.wellness.Entity.User;
import com.wellness.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupRequest request) {

        if (userRepository.existsByLoginId(request.loginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        User user = new User(
                request.loginId(),
                request.nickname(),
                encodedPassword,
                LocalDateTime.now()
        );

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {

        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() ->
                        new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.")
                );

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }
}
