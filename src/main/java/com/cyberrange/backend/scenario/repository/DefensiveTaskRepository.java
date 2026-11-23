package com.cyberrange.backend.scenario.repository;

import com.cyberrange.backend.scenario.entity.DefensiveTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefensiveTaskRepository
        extends JpaRepository<DefensiveTask, Long> {

    List<DefensiveTask> findByScenario_IdOrderBySequenceOrderAsc(
            Long scenarioId
    );
}
