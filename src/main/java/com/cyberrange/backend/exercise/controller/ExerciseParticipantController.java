package com.cyberrange.backend.exercise.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.exercise.dto.AddExerciseParticipantRequest;
import com.cyberrange.backend.exercise.dto.ExerciseParticipantResponse;
import com.cyberrange.backend.exercise.service.ExerciseParticipantService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exercises/{exerciseId}/participants")
public class ExerciseParticipantController {

    private final ExerciseParticipantService participantService;

    public ExerciseParticipantController(
            ExerciseParticipantService participantService
    ) {
        this.participantService = participantService;
    }

    /**
     * Retrieve all participants assigned to an exercise.
     *
     * Access rules:
     * - ADMIN: Can view all participants.
     * - INSTRUCTOR: Can view all participants.
     * - STUDENT: Can view participants only when assigned
     *   as WHITE in the requested exercise.
     *
     * @param exerciseId Exercise ID
     * @param jwt        Authenticated user's JWT
     * @return List of exercise participants
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<ExerciseParticipantResponse>>>
    getParticipants(
            @PathVariable Long exerciseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<ExerciseParticipantResponse> participants =
                participantService.getParticipants(
                        exerciseId,
                        jwt.getSubject(),
                        jwt.getClaimAsString("role")
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Exercise participants retrieved successfully",
                        participants
                )
        );
    }

    /**
     * Add a user as a participant in an exercise.
     *
     * Only ADMIN and INSTRUCTOR are allowed.
     *
     * @param exerciseId Exercise ID
     * @param request    Participant information
     * @return Created participant
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<ExerciseParticipantResponse>>
    addParticipant(
            @PathVariable Long exerciseId,
            @Valid @RequestBody AddExerciseParticipantRequest request
    ) {
        ExerciseParticipantResponse participant =
                participantService.addParticipant(
                        exerciseId,
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Participant added successfully",
                        participant
                )
        );
    }

    /**
     * Remove a user from an exercise.
     *
     * Only ADMIN and INSTRUCTOR are allowed.
     *
     * @param exerciseId Exercise ID
     * @param userId     User ID to remove
     * @return Successful removal response
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> removeParticipant(
            @PathVariable Long exerciseId,
            @PathVariable Long userId
    ) {
        participantService.removeParticipant(
                exerciseId,
                userId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Participant removed successfully",
                        null
                )
        );
    }
}
