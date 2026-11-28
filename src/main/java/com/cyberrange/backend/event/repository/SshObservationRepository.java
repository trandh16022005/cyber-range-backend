package com.cyberrange.backend.event.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SshObservationRepository {

    public record Failure(long id, Instant at) {}

    private final JdbcClient db;

    public SshObservationRepository(JdbcClient db) {
        this.db = db;
    }

    public Optional<Long> duplicate(UUID sessionId, String recordKey) {
        return db.sql("SELECT id FROM events WHERE ssh_lab_session_id = :session AND collector_record_key = :record")
                .param("session", sessionId).param("record", recordKey).query(Long.class).optional();
    }

    public void markObserved(long eventId, UUID sessionId, String recordKey) {
        db.sql("UPDATE events SET ssh_lab_session_id = :session, collector_record_key = :record WHERE id = :id")
                .param("session", sessionId).param("record", recordKey).param("id", eventId).update();
    }

    public List<Failure> failures(UUID session, long exercise, long service, String ip, String account, Instant from, Instant to) {
        return db.sql("""
                SELECT id, event_timestamp FROM events
                WHERE exercise_id = :exercise AND service_id = :service AND ssh_lab_session_id = :session
                  AND event_type = 'SSH_AUTH_FAILURE' AND data->>'sourceIp' = :ip AND data->>'username' = :account
                  AND event_timestamp BETWEEN :from AND :to
                ORDER BY event_timestamp, id
                """)
                .param("exercise", exercise).param("service", service).param("session", session)
                .param("ip", ip).param("account", account).param("from", utc(from)).param("to", utc(to))
                .query((rs, row) -> new Failure(rs.getLong("id"), rs.getObject("event_timestamp", OffsetDateTime.class).toInstant()))
                .list();
    }

    public long findOrCreateAlert(UUID session, long exercise, long service, String ip, String account,
                                  Instant first, Instant last, Instant detected) {
        var existing = db.sql("""
                SELECT id FROM security_alerts WHERE lab_session_id = :session AND service_id = :service
                  AND source_ip = :ip AND account = :account AND first_event_at <= :last AND last_event_at >= :first
                ORDER BY detected_at, id LIMIT 1
                """)
                .param("session", session).param("service", service).param("ip", ip).param("account", account)
                .param("last", utc(last)).param("first", utc(first)).query(Long.class).optional();
        return existing.orElseGet(() -> db.sql("""
                INSERT INTO security_alerts (exercise_id, service_id, lab_session_id, source_ip, account,
                    first_event_at, last_event_at, detected_at, severity, status)
                VALUES (:exercise, :service, :session, :ip, :account, :first, :last, :detected, 'HIGH', 'OPEN')
                RETURNING id
                """)
                .param("exercise", exercise).param("service", service).param("session", session)
                .param("ip", ip).param("account", account).param("first", utc(first)).param("last", utc(last))
                .param("detected", utc(detected)).query(Long.class).single());
    }

    public void link(long alertId, long eventId) {
        db.sql("""
                INSERT INTO security_alert_events (alert_id, event_id) VALUES (:alert, :event)
                ON CONFLICT (alert_id, event_id) DO NOTHING
                """).param("alert", alertId).param("event", eventId).update();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
