package com.cyberrange.backend.assessment.entity;

import com.cyberrange.backend.common.exception.BusinessException;
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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(
        name = "assessment_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_assessment_results_assessment_task",
                        columnNames = {"assessment_id", "defensive_task_id"}
                )
        }
)
public class AssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "defensive_task_id", nullable = false)
    private DefensiveTask defensiveTask;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "detection_time")
    private Duration detectionTime;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "response_time")
    private Duration responseTime;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    protected AssessmentResult() {
        // Required by JPA
    }

    public AssessmentResult(
            Assessment assessment,
            DefensiveTask defensiveTask
    ) {
        this.assessment = assessment;
        this.defensiveTask = defensiveTask;
        this.status = "PENDING";
    }

    // Getters

    public Long getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public DefensiveTask getDefensiveTask() {
        return defensiveTask;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Duration getDetectionTime() {
        return detectionTime;
    }

    public Duration getResponseTime() {
        return responseTime;
    }

    public String getReason() {
        return reason;
    }

    // Task lifecycle

    public void start() {
        if (startedAt != null) {
            throw new BusinessException(
                    "Task has already been started"
            );
        }

        if (!"PENDING".equals(status)) {
            throw new BusinessException(
                    "Only PENDING tasks can be started"
            );
        }

        this.startedAt = Instant.now();
        this.status = "IN_PROGRESS";
    }

    public void recordDetection(Instant detectedAt) {
        if (startedAt == null) {
            throw new IllegalStateException(
                    "Task must be started before detection"
            );
        }

        if (detectedAt == null || detectedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Detection time cannot be before task start"
            );
        }

        if (this.detectedAt != null) {
            throw new IllegalStateException(
                    "Detection has already been recorded"
            );
        }

        if (!"IN_PROGRESS".equals(status)) {
            throw new IllegalStateException(
                    "Task must be IN_PROGRESS to record detection"
            );
        }

        this.detectedAt = detectedAt;
        this.detectionTime = Duration.between(
                startedAt,
                detectedAt
        );
    }

    public void recordResponse(Instant respondedAt) {
        if (detectedAt == null) {
            throw new IllegalStateException(
                    "Detection must be recorded before response"
            );
        }

        if (respondedAt == null || respondedAt.isBefore(detectedAt)) {
            throw new IllegalArgumentException(
                    "Response time cannot be before detection"
            );
        }

        if (this.respondedAt != null) {
            throw new IllegalStateException(
                    "Response has already been recorded"
            );
        }

        if (!"IN_PROGRESS".equals(status)) {
            throw new IllegalStateException(
                    "Task must be IN_PROGRESS to record response"
            );
        }

        this.respondedAt = respondedAt;
        this.responseTime = Duration.between(
                detectedAt,
                respondedAt
        );
    }

    public void complete(Instant completedAt, String reason) {
        if (startedAt == null) {
            throw new IllegalStateException(
                    "Task must be started before completion"
            );
        }

        if (!"IN_PROGRESS".equals(status)) {
            throw new IllegalStateException(
                    "Only IN_PROGRESS tasks can be completed"
            );
        }

        if (completedAt == null) {
            throw new IllegalArgumentException(
                    "Completion time cannot be null"
            );
        }

        Instant latestEvent = respondedAt != null
                ? respondedAt
                : detectedAt != null
                ? detectedAt
                : startedAt;

        if (completedAt.isBefore(latestEvent)) {
            throw new IllegalArgumentException(
                    "Completion time cannot be before the latest task event"
            );
        }

        this.status = "COMPLETED";
        this.completedAt = completedAt;
        this.reason = reason;
    }

    public void fail(String reason) {
        if (!"IN_PROGRESS".equals(status)) {
            throw new IllegalStateException(
                    "Only IN_PROGRESS tasks can be marked as FAILED"
            );
        }

        this.status = "FAILED";
        this.reason = reason;
    }

    public void markNotApplicable(String reason) {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException(
                    "Only PENDING tasks can be marked as NOT_APPLICABLE"
            );
        }

        this.status = "NOT_APPLICABLE";
        this.reason = reason;
    }

    // Updates

    public void updateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Status cannot be empty"
            );
        }

        throw new UnsupportedOperationException(
                "Use task lifecycle methods to change status"
        );
    }

    public void updateReason(String reason) {
        this.reason = reason;
    }
}
