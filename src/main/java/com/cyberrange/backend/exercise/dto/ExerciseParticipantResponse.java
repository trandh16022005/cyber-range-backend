package com.cyberrange.backend.exercise.dto;

import com.cyberrange.backend.exercise.entity.ExerciseParticipant;

import java.time.Instant;

public record ExerciseParticipantResponse(
        Long id,
        Long exerciseId,
        Long userId,
        String username,
        String fullName,
        String email,
        Long teamId,
        String teamName,
        String role,
        Instant joinedAt
) {

    public static ExerciseParticipantResponse fromEntity(
            ExerciseParticipant participant
    ) {
        return new ExerciseParticipantResponse(
                participant.getId(),
                participant.getExercise().getId(),
                participant.getUser().getId(),
                participant.getUser().getUsername(),
                participant.getUser().getFullName(),
                participant.getUser().getEmail(),
                participant.getTeam() != null
                        ? participant.getTeam().getId()
                        : null,
                participant.getTeam() != null
                        ? participant.getTeam().getName()
                        : null,
                participant.getRole(),
                participant.getJoinedAt()
        );
    }
}
