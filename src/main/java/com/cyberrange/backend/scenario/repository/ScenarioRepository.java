package com.cyberrange.backend.scenario.repository;

import com.cyberrange.backend.scenario.entity.Scenario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    // Check duplicate scenario name (case-insensitive)
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Pagination and filtering
    Page<Scenario> findByStatus(String status, Pageable pageable);

    Page<Scenario> findByType(String type, Pageable pageable);

    Page<Scenario> findByStatusAndType(
            String status,
            String type,
            Pageable pageable
    );

    // Check whether a scenario is referenced by any exercise
    @Query(
            value = """
                    SELECT CASE
                        WHEN COUNT(*) > 0 THEN TRUE
                        ELSE FALSE
                    END
                    FROM exercises
                    WHERE scenario_id = :scenarioId
                    """,
            nativeQuery = true
    )
    boolean existsInExercises(@Param("scenarioId") Long scenarioId);
}
