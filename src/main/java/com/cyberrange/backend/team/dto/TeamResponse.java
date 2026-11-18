package com.cyberrange.backend.team.dto;

import com.cyberrange.backend.team.entity.Team;

import java.time.Instant;

public record TeamResponse(
        Long id,
        String name,
        String description,
        Instant createdAt
) {

    public static TeamResponse fromEntity(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCreatedAt()
        );
    }
}
