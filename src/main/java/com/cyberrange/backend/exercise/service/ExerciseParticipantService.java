package com.cyberrange.backend.exercise.service;

import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import com.cyberrange.backend.exercise.dto.AddExerciseParticipantRequest;
import com.cyberrange.backend.exercise.dto.ExerciseParticipantResponse;
import com.cyberrange.backend.exercise.entity.Exercise;
import com.cyberrange.backend.exercise.entity.ExerciseParticipant;
import com.cyberrange.backend.exercise.repository.ExerciseParticipantRepository;
import com.cyberrange.backend.exercise.repository.ExerciseRepository;
import com.cyberrange.backend.team.entity.Team;
import com.cyberrange.backend.team.repository.TeamMemberRepository;
import com.cyberrange.backend.team.repository.TeamRepository;
import com.cyberrange.backend.user.entity.User;
import com.cyberrange.backend.user.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ExerciseParticipantService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public ExerciseParticipantService(
            ExerciseRepository exerciseRepository,
            ExerciseParticipantRepository participantRepository,
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    /**
     * Retrieve participants of an exercise.
     *
     * ADMIN and INSTRUCTOR can view all participants.
     * STUDENT can view participants only if assigned as WHITE.
     *
     * @param exerciseId   Exercise ID
     * @param currentUserId Authenticated user ID from JWT subject
     * @param systemRole   Authenticated user's system role
     * @return List of exercise participants
     */
    public List<ExerciseParticipantResponse> getParticipants(
            Long exerciseId,
            String currentUserId,
            String systemRole
    ) {
        validateId(exerciseId, "Exercise ID");

        if (!exerciseRepository.existsById(exerciseId)) {
            throw new ResourceNotFoundException(
                    "Exercise not found with ID: " + exerciseId
            );
        }

        if (currentUserId == null || currentUserId.isBlank()) {
            throw new AccessDeniedException("Access denied");
        }

        String normalizedSystemRole = systemRole == null
                ? ""
                : systemRole.trim().toUpperCase(Locale.ROOT);

        boolean canViewAll =
                "ADMIN".equals(normalizedSystemRole)
                        || "INSTRUCTOR".equals(normalizedSystemRole);

        if (!canViewAll) {
            if (!"STUDENT".equals(normalizedSystemRole)) {
                throw new AccessDeniedException("Access denied");
            }

            Long userId;

            try {
                userId = Long.parseLong(currentUserId);
            } catch (NumberFormatException exception) {
                throw new AccessDeniedException("Access denied");
            }

            boolean isWhiteParticipant =
                    participantRepository
                            .existsByExercise_IdAndUser_IdAndRole(
                                    exerciseId,
                                    userId,
                                    "WHITE"
                            );

            if (!isWhiteParticipant) {
                throw new AccessDeniedException("Access denied");
            }
        }

        return participantRepository
                .findByExercise_IdOrderByJoinedAtAsc(exerciseId)
                .stream()
                .map(ExerciseParticipantResponse::fromEntity)
                .toList();
    }

    /**
     * Add a participant to an exercise.
     *
     * Only ACTIVE users can be assigned.
     * If a team is specified, the user must already belong to it.
     */
    @Transactional
    public ExerciseParticipantResponse addParticipant(
            Long exerciseId,
            AddExerciseParticipantRequest request
    ) {
        validateId(exerciseId, "Exercise ID");

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body is required"
            );
        }

        validateId(request.userId(), "User ID");

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exercise not found with ID: " + exerciseId
                ));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + request.userId()
                ));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(
                    "Only ACTIVE users can join an exercise"
            );
        }

        String role = normalizeRole(request.role());

        if (participantRepository.existsByExercise_IdAndUser_Id(
                exerciseId,
                user.getId()
        )) {
            throw new BusinessException(
                    "User is already a participant in this exercise"
            );
        }

        Team team = null;

        if (request.teamId() != null) {
            validateId(request.teamId(), "Team ID");

            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Team not found with ID: " + request.teamId()
                    ));

            boolean isTeamMember =
                    teamMemberRepository.existsByTeam_IdAndUser_Id(
                            team.getId(),
                            user.getId()
                    );

            if (!isTeamMember) {
                throw new BusinessException(
                        "User must belong to the selected team"
                );
            }
        }

        ExerciseParticipant participant = new ExerciseParticipant(
                exercise,
                user,
                team,
                role
        );

        ExerciseParticipant saved =
                participantRepository.save(participant);

        return ExerciseParticipantResponse.fromEntity(saved);
    }

    /**
     * Remove a participant from an exercise.
     */
    @Transactional
    public void removeParticipant(Long exerciseId, Long userId) {

        validateId(exerciseId, "Exercise ID");
        validateId(userId, "User ID");

        if (!exerciseRepository.existsById(exerciseId)) {
            throw new ResourceNotFoundException(
                    "Exercise not found with ID: " + exerciseId
            );
        }

        ExerciseParticipant participant =
                participantRepository
                        .findByExercise_IdAndUser_Id(exerciseId, userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Participant not found in this exercise"
                        ));

        participantRepository.delete(participant);
    }

    /**
     * Normalize and validate exercise participant role.
     */
    private String normalizeRole(String role) {

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);

        if (!List.of("RED", "BLUE", "WHITE").contains(normalized)) {
            throw new IllegalArgumentException(
                    "Role must be RED, BLUE or WHITE"
            );
        }

        return normalized;
    }

    /**
     * Validate positive database IDs.
     */
    private void validateId(Long id, String fieldName) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be a positive number"
            );
        }
    }
}
