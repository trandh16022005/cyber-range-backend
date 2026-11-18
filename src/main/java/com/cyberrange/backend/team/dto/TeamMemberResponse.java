package com.cyberrange.backend.team.dto;

import com.cyberrange.backend.team.entity.TeamMember;

import java.time.Instant;

public record TeamMemberResponse(
        Long userId,
        String username,
        String email,
        String fullName,
        String role,
        Instant joinedAt
) {

    public static TeamMemberResponse fromEntity(TeamMember teamMember) {
        return new TeamMemberResponse(
                teamMember.getUser().getId(),
                teamMember.getUser().getUsername(),
                teamMember.getUser().getEmail(),
                teamMember.getUser().getFullName(),
                teamMember.getUser().getRole().getName(),
                teamMember.getJoinedAt()
        );
    }
}

