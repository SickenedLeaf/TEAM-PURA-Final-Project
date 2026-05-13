package com.gamecheck.controller;

import com.gamecheck.dto.LoginRequest;
import com.gamecheck.dto.LoginResponse;
import com.gamecheck.dto.RegisterRequest;
import com.gamecheck.dto.RegisterResponse;
import com.gamecheck.dto.UserProfileResponse;
import com.gamecheck.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse("Registration successful."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse body = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getProfile(userId));
    }
}
