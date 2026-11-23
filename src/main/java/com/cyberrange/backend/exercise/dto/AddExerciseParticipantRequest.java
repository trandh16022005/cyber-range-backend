package com.cyberrange.backend.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

public record AddExerciseParticipantRequest(

        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be positive")
        Long userId,

        @Positive(message = "Team ID must be positive")
        Long teamId,

        @NotBlank(message = "Role is required")
        @Pattern(
                regexp = "RED|BLUE|WHITE",
                message = "Role must be RED, BLUE or WHITE"
        )
        String role

) {
}
