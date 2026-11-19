package com.cyberrange.backend.scenario.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "scenarios")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "difficulty", length = 30)
    private String difficulty;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Scenario() {
        // Required by JPA
    }

    public Scenario(
            String name,
            String description,
            String type,
            String difficulty,
            User createdBy
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.difficulty = difficulty;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (status == null) {
            status = "DRAFT";
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String name,
            String description,
            String type,
            String difficulty
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.difficulty = difficulty;
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}

