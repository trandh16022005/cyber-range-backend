package com.cyberrange.backend.auth.dto;

public record LoginRequest(
        String username,
        String password
) {
}
