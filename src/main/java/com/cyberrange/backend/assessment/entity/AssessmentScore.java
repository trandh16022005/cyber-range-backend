package com.cyberrange.backend.assessment.entity;

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

import java.math.BigDecimal;

@Entity
@Table(
        name = "assessment_scores",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_assessment_scores_result_criterion",
                        columnNames = {
                                "assessment_result_id",
                                "criterion_id"
                        }
                )
        }
)
public class AssessmentScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_result_id", nullable = false)
    private AssessmentResult assessmentResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private AssessmentCriterion criterion;

    @Column(
            name = "awarded_score",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal awardedScore = BigDecimal.ZERO;

    @Column(
            name = "max_score",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal maxScore;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    protected AssessmentScore() {
        // Required by JPA
    }

    public AssessmentScore(
            AssessmentResult assessmentResult,
            AssessmentCriterion criterion,
            BigDecimal awardedScore,
            BigDecimal maxScore,
            String reason
    ) {
        this.assessmentResult = assessmentResult;
        this.criterion = criterion;
        this.awardedScore = awardedScore;
        this.maxScore = maxScore;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public AssessmentResult getAssessmentResult() {
        return assessmentResult;
    }

    public AssessmentCriterion getCriterion() {
        return criterion;
    }

    public BigDecimal getAwardedScore() {
        return awardedScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public String getReason() {
        return reason;
    }

    public void updateScore(
            BigDecimal awardedScore,
            String reason
    ) {
        this.awardedScore = awardedScore;
        this.reason = reason;
    }
}

