package com.cyberrange.backend.assessment.entity;

import com.cyberrange.backend.exercise.entity.Exercise;
import com.cyberrange.backend.team.entity.Team;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "assessments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_assessments_exercise_team",
                        columnNames = {"exercise_id", "team_id"}
                )
        }
)
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(
            name = "total_score",
            nullable = false,
            precision = 7,
            scale = 2
    )
    private BigDecimal totalScore;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "review_status", nullable = false, length = 30)
    private String reviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Assessment() {
        // Required by JPA
    }

    public Assessment(Exercise exercise, Team team) {
        this.exercise = exercise;
        this.team = team;
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }

        if (totalScore == null) {
            totalScore = BigDecimal.ZERO;
        }

        if (reviewStatus == null) {
            reviewStatus = "PENDING";
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Team getTeam() {
        return team;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void start() {
        this.status = "RUNNING";

        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    public void complete(BigDecimal totalScore) {
        this.status = "COMPLETED";
        this.totalScore = totalScore;
        this.completedAt = Instant.now();
    }

    public void fail() {
        this.status = "FAILED";
    }

    public void review(User reviewer, String comment) {
        this.reviewStatus = "REVIEWED";
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        this.reviewComment = comment;
    }
}
