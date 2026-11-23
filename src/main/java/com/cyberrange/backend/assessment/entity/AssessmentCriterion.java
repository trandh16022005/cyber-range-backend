package com.cyberrange.backend.assessment.entity;

import com.cyberrange.backend.scenario.entity.DefensiveTask;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "assessment_criteria")
public class AssessmentCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "defensive_task_id", nullable = false)
    private DefensiveTask defensiveTask;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "criterion_type", nullable = false, length = 50)
    private String criterionType;

    @Column(
            name = "max_score",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal maxScore;

    protected AssessmentCriterion() {
        // Required by JPA
    }

    public AssessmentCriterion(
            DefensiveTask defensiveTask,
            String name,
            String description,
            String criterionType,
            BigDecimal maxScore
    ) {
        this.defensiveTask = defensiveTask;
        this.name = name;
        this.description = description;
        this.criterionType = criterionType;
        this.maxScore = maxScore;
    }

    public Long getId() {
        return id;
    }

    public DefensiveTask getDefensiveTask() {
        return defensiveTask;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCriterionType() {
        return criterionType;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }
}
