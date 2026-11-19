package com.cyberrange.backend.exercise.entity;

import com.cyberrange.backend.lab.entity.CyberRange;
import com.cyberrange.backend.scenario.entity.Scenario;
import com.cyberrange.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cyber_range_id", nullable = false)
    private CyberRange cyberRange;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Exercise() {
        // Required by JPA
    }

    public Exercise(
            Scenario scenario,
            CyberRange cyberRange,
            String name,
            User createdBy
    ) {
        this.scenario = scenario;
        this.cyberRange = cyberRange;
        this.name = name;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "CREATED";
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public CyberRange getCyberRange() {
        return cyberRange;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

