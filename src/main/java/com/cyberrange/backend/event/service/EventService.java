package com.cyberrange.backend.event.service;

import com.cyberrange.backend.asset.entity.Asset;
import com.cyberrange.backend.asset.entity.ManagedService;
import com.cyberrange.backend.asset.repository.AssetRepository;
import com.cyberrange.backend.asset.repository.ManagedServiceRepository;
import com.cyberrange.backend.event.dto.CreateEventRequest;
import com.cyberrange.backend.event.dto.EventResponse;
import com.cyberrange.backend.event.entity.Event;
import com.cyberrange.backend.event.repository.EventRepository;
import com.cyberrange.backend.exercise.entity.Exercise;
import com.cyberrange.backend.exercise.repository.ExerciseRepository;
import com.cyberrange.backend.common.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final ExerciseRepository exerciseRepository;
    private final AssetRepository assetRepository;
    private final ManagedServiceRepository managedServiceRepository;

    public EventService(
            EventRepository eventRepository,
            ExerciseRepository exerciseRepository,
            AssetRepository assetRepository,
            ManagedServiceRepository managedServiceRepository
    ) {
        this.eventRepository = eventRepository;
        this.exerciseRepository = exerciseRepository;
        this.assetRepository = assetRepository;
        this.managedServiceRepository = managedServiceRepository;
    }

    // 1. Get Events by Exercise
    public List<EventResponse> getEventsByExercise(
            Long exerciseId,
            String eventType
    ) {
        validateRequiredId(exerciseId, "Exercise ID");

        findExerciseById(exerciseId);

        List<Event> events;

        if (eventType == null || eventType.isBlank()) {
            events = eventRepository
                    .findByExercise_IdOrderByEventTimestampDesc(exerciseId);
        } else {
            String normalizedEventType = validateRequiredText(
                    eventType,
                    "Event type",
                    50
            );

            events = eventRepository
                    .findByExercise_IdAndEventTypeOrderByEventTimestampDesc(
                            exerciseId,
                            normalizedEventType
                    );
        }

        return events.stream()
                .map(EventResponse::fromEntity)
                .toList();
    }

    // 2. Get Event details
    public EventResponse getEventById(Long id) {
        Event event = findEventById(id);

        return EventResponse.fromEntity(event);
    }

    // 3. Create Event
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Event request must not be null"
            );
        }

        validateRequiredId(request.exerciseId(), "Exercise ID");

        Exercise exercise = findExerciseById(
                request.exerciseId()
        );

        String eventType = validateRequiredText(
                request.eventType(),
                "Event type",
                50
        );

        if (request.eventTimestamp() == null) {
            throw new IllegalArgumentException(
                    "Event timestamp is required"
            );
        }

        String severity = normalizeOptional(
                request.severity(),
                20,
                "Severity"
        );

        String source = normalizeOptional(
                request.source(),
                100,
                "Source"
        );

        Asset asset = null;

        if (request.assetId() != null) {
            validateRequiredId(request.assetId(), "Asset ID");

            asset = findAssetById(request.assetId());

            Long exerciseCyberRangeId =
                    exercise.getCyberRange().getId();

            Long assetCyberRangeId =
                    asset.getCyberRange().getId();

            if (!exerciseCyberRangeId.equals(assetCyberRangeId)) {
                throw new IllegalArgumentException(
                        "Asset does not belong to the Exercise Cyber Range"
                );
            }
        }

        ManagedService service = null;

        if (request.serviceId() != null) {

            validateRequiredId(request.serviceId(), "Service ID");

            if (asset == null) {
                throw new IllegalArgumentException(
                        "Asset ID is required when Service ID is provided"
                );
            }

            service = findServiceById(request.serviceId());

            if (!service.getAsset().getId().equals(asset.getId())) {
                throw new IllegalArgumentException(
                        "Service does not belong to the selected Asset"
                );
            }
        }

        JsonNode data = request.data();

        Event event = new Event(
                exercise,
                asset,
                service,
                eventType,
                severity,
                request.eventTimestamp(),
                source,
                data
        );

        Event savedEvent = eventRepository.save(event);

        return EventResponse.fromEntity(savedEvent);
    }

    // Helper methods

    private Event findEventById(Long id) {
        validateRequiredId(id, "Event ID");

        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found with ID: " + id
                ));
    }

    private Exercise findExerciseById(Long id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exercise not found with ID: " + id
                ));
    }

    private Asset findAssetById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found with ID: " + id
                ));
    }

    private ManagedService findServiceById(Long id) {
        return managedServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with ID: " + id
                ));
    }

    private void validateRequiredId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be a positive number"
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
                    fieldName + " is required"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }

    private String normalizeOptional(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }
}
