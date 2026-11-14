package com.cyberrange.backend.user.dto;

import com.cyberrange.backend.user.entity.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

