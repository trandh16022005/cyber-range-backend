package com.cyberrange.backend.exercise.repository;

import com.cyberrange.backend.exercise.entity.ExerciseParticipant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseParticipantRepository
        extends JpaRepository<ExerciseParticipant, Long> {

    @EntityGraph(attributePaths = {
            "user",
            "user.role",
            "team"
    })
    List<ExerciseParticipant> findByExercise_IdOrderByJoinedAtAsc(
            Long exerciseId
    );

    @EntityGraph(attributePaths = {
            "user",
            "user.role",
            "team"
    })
    Optional<ExerciseParticipant> findByExercise_IdAndUser_Id(
            Long exerciseId,
            Long userId
    );

    boolean existsByExercise_IdAndUser_Id(
            Long exerciseId,
            Long userId
    );

    boolean existsByExercise_IdAndUser_IdAndRole(
            Long exerciseId,
            Long userId,
            String role
    );

    long countByExercise_Id(Long exerciseId);

    void deleteByExercise_IdAndUser_Id(
            Long exerciseId,
            Long userId
    );
}
