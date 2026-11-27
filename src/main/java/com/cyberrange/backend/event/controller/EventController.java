package com.cyberrange.backend.event.controller;

import com.cyberrange.backend.common.response.ApiResponse;
import com.cyberrange.backend.event.dto.CreateEventRequest;
import com.cyberrange.backend.event.dto.EventResponse;
import com.cyberrange.backend.event.service.EventService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // 1. Get events by Exercise, optionally filtered by Event Type
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEvents(
            @RequestParam Long exerciseId,
            @RequestParam(required = false) String eventType
    ) {
        List<EventResponse> events =
                eventService.getEventsByExercise(exerciseId, eventType);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Events retrieved successfully",
                        events
                )
        );
    }

    // 2. Get Event details
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(
            @PathVariable Long id
    ) {
        EventResponse event = eventService.getEventById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Event retrieved successfully",
                        event
                )
        );
    }

    // 3. Create Event
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @RequestBody CreateEventRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Event request must not be null"
            );
        }

        EventResponse event = eventService.createEvent(request);

        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        "Event created successfully",
                        event
                )
        );
    }
}
