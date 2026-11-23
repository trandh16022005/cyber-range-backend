package com.cyberrange.backend.scenario.entity;

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
@Table(name = "defensive_tasks")
public class DefensiveTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder = 1;

    @Column(
            name = "max_score",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal maxScore = BigDecimal.ZERO;

    @Column(name = "required", nullable = false)
    private Boolean required = true;

    protected DefensiveTask() {
        // Required by JPA
    }

    public DefensiveTask(
            Scenario scenario,
            String name,
            String description,
            String taskType,
            Integer sequenceOrder,
            BigDecimal maxScore,
            Boolean required
    ) {
        this.scenario = scenario;
        this.name = name;
        this.description = description;
        this.taskType = taskType;
        this.sequenceOrder = sequenceOrder;
        this.maxScore = maxScore;
        this.required = required;
    }

    public Long getId() {
        return id;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTaskType() {
        return taskType;
    }

    public Integer getSequenceOrder() {
        return sequenceOrder;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public Boolean getRequired() {
        return required;
    }
}
