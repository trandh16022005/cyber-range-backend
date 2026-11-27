package com.cyberrange.backend.evidence.repository;

import com.cyberrange.backend.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findByExercise_IdOrderByCreatedAtDesc(Long exerciseId);

    List<Evidence> findByEvent_IdOrderByCreatedAtDesc(Long eventId);

    List<Evidence> findByExercise_IdAndEvidenceTypeOrderByCreatedAtDesc(
            Long exerciseId,
            String evidenceType
    );
}
