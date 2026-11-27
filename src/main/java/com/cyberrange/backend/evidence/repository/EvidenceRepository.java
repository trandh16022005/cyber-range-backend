package com.cyberrange.backend.evidence.repository;

import com.cyberrange.backend.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findByExercise_IdOrderByCreatedAtDesc(Long exerciseId);

    List<Evidence> findByEvent_IdOrderByCreatedAtDesc(Long eventId);

    List<Evidence> findByExercise_IdAndEvidenceTypeOrderByCreatedAtDesc(
            Long exerciseId,
            String evidenceType
    );

    List<Evidence> findByExercise_IdAndVerificationStatusOrderByCreatedAtDesc(
            Long exerciseId,
            String verificationStatus
    );

    /**
     * Find verified evidence linked to an assessment score.
     * Fetch related exercise and event information.
     */
    @EntityGraph(attributePaths = {
            "exercise",
            "event"
    })
    List<Evidence> findByIdInAndVerificationStatus(
            List<Long> ids,
            String verificationStatus
    );
}
