package com.gamecheck.service;

import com.gamecheck.dto.LoginResponse;
import com.gamecheck.dto.UserProfileResponse;
import com.gamecheck.exception.EmailAlreadyRegisteredException;
import com.gamecheck.model.User;
import com.gamecheck.repository.UserRepository;
import com.gamecheck.security.JwtUtil;
import com.gamecheck.security.LoginRateLimiter;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter loginRateLimiter;

    @Transactional
    public void register(String email, String password) {
        String normalized = normalizeEmail(email);
        if (userRepository.findByEmail(normalized).isPresent()) {
            throw new EmailAlreadyRegisteredException("An account with this email already exists.");
        }
        User user =
                User.builder()
                        .email(normalized)
                        .passwordHash(passwordEncoder.encode(password))
                        .createdAt(LocalDateTime.now())
                        .isActive(true)
                        .build();
        userRepository.save(user);
    }

    public LoginResponse login(String email, String password) {
        String normalized = normalizeEmail(email);
        loginRateLimiter.assertNotLocked(normalized);

        User user =
                userRepository
                        .findByEmail(normalized)
                        .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                        .orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            loginRateLimiter.recordFailure(normalized);
            throw new BadCredentialsException("Invalid email or password.");
        }

        loginRateLimiter.clear(normalized);
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, "Bearer", jwtUtil.getExpirationMs());
    }

    public UserProfileResponse getProfile(Integer userId) {
        User user =
                userRepository
                        .findById(userId)
                        .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                        .orElseThrow(() -> new BadCredentialsException("Invalid or inactive user."));
        return new UserProfileResponse(user.getUserId(), user.getEmail());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
