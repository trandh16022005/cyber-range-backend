package com.cyberrange.backend.evidence.service;

import com.cyberrange.backend.event.entity.Event;
import com.cyberrange.backend.event.repository.EventRepository;
import com.cyberrange.backend.evidence.dto.CreateEvidenceRequest;
import com.cyberrange.backend.evidence.dto.EvidenceResponse;
import com.cyberrange.backend.evidence.entity.Evidence;
import com.cyberrange.backend.evidence.repository.EvidenceRepository;
import com.cyberrange.backend.exercise.entity.Exercise;
import com.cyberrange.backend.exercise.repository.ExerciseRepository;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final ExerciseRepository exerciseRepository;
    private final EventRepository eventRepository;

    public EvidenceService(
            EvidenceRepository evidenceRepository,
            ExerciseRepository exerciseRepository,
            EventRepository eventRepository
    ) {
        this.evidenceRepository = evidenceRepository;
        this.exerciseRepository = exerciseRepository;
        this.eventRepository = eventRepository;
    }

    public List<EvidenceResponse> getEvidenceByExercise(Long exerciseId) {
        validateRequiredId(exerciseId, "Exercise ID");

        if (!exerciseRepository.existsById(exerciseId)) {
            throw new ResourceNotFoundException(
                    "Exercise not found with ID: " + exerciseId
            );
        }

        return evidenceRepository
                .findByExercise_IdOrderByCreatedAtDesc(exerciseId)
                .stream()
                .map(EvidenceResponse::fromEntity)
                .toList();
    }

    public EvidenceResponse getEvidenceById(Long id) {
        Evidence evidence = findEvidenceById(id);
        return EvidenceResponse.fromEntity(evidence);
    }

    @Transactional
    public EvidenceResponse createEvidence(CreateEvidenceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Evidence request must not be null"
            );
        }

        validateRequiredId(request.exerciseId(), "Exercise ID");

        String evidenceType = validateRequiredText(
                request.evidenceType(),
                "Evidence type",
                50
        );

        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exercise not found with ID: " + request.exerciseId()
                ));

        Event event = null;

        if (request.eventId() != null) {
            validateRequiredId(request.eventId(), "Event ID");

            event = eventRepository.findById(request.eventId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Event not found with ID: " + request.eventId()
                    ));

            if (!event.getExercise().getId().equals(exercise.getId())) {
                throw new IllegalArgumentException(
                        "Event does not belong to the specified Exercise"
                );
            }
        }

        Evidence evidence = new Evidence(
                exercise,
                event,
                evidenceType,
                request.data()
        );

        Evidence savedEvidence = evidenceRepository.save(evidence);

        return EvidenceResponse.fromEntity(savedEvidence);
    }

    private Evidence findEvidenceById(Long id) {
        validateRequiredId(id, "Evidence ID");

        return evidenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evidence not found with ID: " + id
                ));
    }

    private void validateRequiredId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than 0"
            );
        }
    }

    private String validateRequiredText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters"
            );
        }

        return normalized;
    }
}
