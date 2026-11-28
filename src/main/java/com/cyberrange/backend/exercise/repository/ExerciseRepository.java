package com.cyberrange.backend.exercise.repository;

import com.cyberrange.backend.exercise.entity.Exercise;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exercise e where e.id = :id")
    Optional<Exercise> findByIdForUpdate(@Param("id") Long id);

    Page<Exercise> findByStatus(
            String status,
            Pageable pageable
    );

    Page<Exercise> findByScenarioId(
            Long scenarioId,
            Pageable pageable
    );

    Page<Exercise> findByCyberRangeId(
            Long cyberRangeId,
            Pageable pageable
    );

    Page<Exercise> findByStatusAndScenarioId(
            String status,
            Long scenarioId,
            Pageable pageable
    );

    Page<Exercise> findByStatusAndCyberRangeId(
            String status,
            Long cyberRangeId,
            Pageable pageable
    );

    Page<Exercise> findByScenarioIdAndCyberRangeId(
            Long scenarioId,
            Long cyberRangeId,
            Pageable pageable
    );

    Page<Exercise> findByStatusAndScenarioIdAndCyberRangeId(
            String status,
            Long scenarioId,
            Long cyberRangeId,
            Pageable pageable
    );

    boolean existsByScenarioId(Long scenarioId);

    boolean existsByCyberRangeId(Long cyberRangeId);
}
