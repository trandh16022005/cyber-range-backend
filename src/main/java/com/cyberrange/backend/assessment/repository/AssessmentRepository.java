package com.cyberrange.backend.assessment.repository;

import com.cyberrange.backend.assessment.entity.Assessment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository
        extends JpaRepository<Assessment, Long> {

    @EntityGraph(attributePaths = {
            "exercise",
            "team"
    })
    List<Assessment> findByExercise_IdOrderByTeam_IdAsc(
            Long exerciseId
    );

    @EntityGraph(attributePaths = {
            "exercise",
            "team"
    })
    List<Assessment> findByExercise_IdInOrderByCreatedAtDesc(
            List<Long> exerciseIds
    );

    @EntityGraph(attributePaths = {
            "exercise",
            "team"
    })
    Optional<Assessment> findByExercise_IdAndTeam_Id(
            Long exerciseId,
            Long teamId
    );

    @EntityGraph(attributePaths = {
            "exercise",
            "team"
    })
    Optional<Assessment> findWithExerciseAndTeamById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Assessment a where a.id = :id")
    Optional<Assessment> findByIdForUpdate(@Param("id") Long id);

    boolean existsByExercise_IdAndTeam_Id(
            Long exerciseId,
            Long teamId
    );
}
