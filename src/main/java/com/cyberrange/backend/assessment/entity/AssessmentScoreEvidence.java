package com.cyberrange.backend.assessment.entity;

import com.cyberrange.backend.evidence.entity.Evidence;
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

import java.time.Instant;

@Entity
@Table(
        name = "assessment_score_evidence",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_assessment_score_evidence",
                        columnNames = {
                                "assessment_score_id",
                                "evidence_id"
                        }
                )
        }
)
public class AssessmentScoreEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "assessment_score_id",
            nullable = false
    )
    private AssessmentScore assessmentScore;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "evidence_id",
            nullable = false
    )
    private Evidence evidence;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected AssessmentScoreEvidence() {
        // Required by JPA
    }

    public AssessmentScoreEvidence(
            AssessmentScore assessmentScore,
            Evidence evidence
    ) {
        this.assessmentScore = assessmentScore;
        this.evidence = evidence;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public AssessmentScore getAssessmentScore() {
        return assessmentScore;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
