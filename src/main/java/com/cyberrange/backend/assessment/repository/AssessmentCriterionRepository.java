package com.cyberrange.backend.assessment.repository;

import com.cyberrange.backend.assessment.entity.AssessmentCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentCriterionRepository
        extends JpaRepository<AssessmentCriterion, Long> {

    List<AssessmentCriterion>
    findByDefensiveTask_IdOrderByIdAsc(Long defensiveTaskId);
}

