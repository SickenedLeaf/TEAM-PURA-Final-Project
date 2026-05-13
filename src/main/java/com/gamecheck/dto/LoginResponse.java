package com.gamecheck.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private final String token;
    private final String tokenType;
    private final long expiresInMs;
}
