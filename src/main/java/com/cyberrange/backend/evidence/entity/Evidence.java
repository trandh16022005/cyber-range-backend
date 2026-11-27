package com.cyberrange.backend.evidence.entity;

import com.cyberrange.backend.common.exception.BusinessException;
import com.cyberrange.backend.event.entity.Event;
import com.cyberrange.backend.exercise.entity.Exercise;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "evidence_type", nullable = false, length = 50)
    private String evidenceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private JsonNode data;

    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_comment")
    private String verificationComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Evidence() {
        // Required by JPA
    }

    public Evidence(
            Exercise exercise,
            Event event,
            String evidenceType,
            JsonNode data
    ) {
        this.exercise = exercise;
        this.event = event;
        this.evidenceType = evidenceType;
        this.data = data;
        this.verificationStatus = "PENDING";
    }

    public void verify(
            String status,
            User verifier,
            String comment
    ) {
        if (!"PENDING".equals(this.verificationStatus)) {
            throw new BusinessException(
                    "Evidence has already been verified"
            );
        }

        this.verificationStatus = status;
        this.verifiedBy = verifier;
        this.verifiedAt = Instant.now();
        this.verificationComment = comment;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (verificationStatus == null) {
            verificationStatus = "PENDING";
        }
    }

    public Long getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Event getEvent() {
        return event;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public JsonNode getData() {
        return data;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public String getVerificationComment() {
        return verificationComment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
