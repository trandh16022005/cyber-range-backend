package com.cyberrange.backend.user.dto;

public record CreateUserRequest(
        String username,
        String email,
        String password,
        String fullName,
        String role
) {
}
