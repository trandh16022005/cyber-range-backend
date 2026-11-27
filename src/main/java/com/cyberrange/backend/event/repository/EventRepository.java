package com.cyberrange.backend.event.repository;

import com.cyberrange.backend.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByExercise_IdOrderByEventTimestampDesc(Long exerciseId);

    List<Event> findByAsset_IdOrderByEventTimestampDesc(Long assetId);

    List<Event> findByExercise_IdAndEventTypeOrderByEventTimestampDesc(
            Long exerciseId,
            String eventType
    );
}
