package com.cyberrange.backend.assessment.repository;

import com.cyberrange.backend.assessment.entity.Assessment;
import org.springframework.data.jpa.repository.EntityGraph;
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
    Optional<Assessment> findByExercise_IdAndTeam_Id(
            Long exerciseId,
            Long teamId
    );

    boolean existsByExercise_IdAndTeam_Id(
            Long exerciseId,
            Long teamId
    );
}
