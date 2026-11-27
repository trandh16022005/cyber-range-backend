package com.cyberrange.backend.event.entity;

import com.cyberrange.backend.asset.entity.Asset;
import com.cyberrange.backend.asset.entity.ManagedService;
import com.cyberrange.backend.exercise.entity.Exercise;
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
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ManagedService service;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "source", length = 100)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private JsonNode data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
        // Required by JPA
    }

    public Event(
            Exercise exercise,
            Asset asset,
            ManagedService service,
            String eventType,
            String severity,
            Instant eventTimestamp,
            String source,
            JsonNode data
    ) {
        this.exercise = exercise;
        this.asset = asset;
        this.service = service;
        this.eventType = eventType;
        this.severity = severity;
        this.eventTimestamp = eventTimestamp;
        this.source = source;
        this.data = data;
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

    public Exercise getExercise() {
        return exercise;
    }

    public Asset getAsset() {
        return asset;
    }

    public ManagedService getService() {
        return service;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public String getSource() {
        return source;
    }

    public JsonNode getData() {
        return data;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
