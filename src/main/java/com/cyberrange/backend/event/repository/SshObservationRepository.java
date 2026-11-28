package com.cyberrange.backend.event.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import com.cyberrange.backend.event.service.SshObservationService.PracticeDetection;

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

    public Optional<PracticeDetection> practiceDetection(UUID session, String prefix) {
        return db.sql("""
                SELECT DISTINCT a.id, a.detected_at FROM security_alerts a
                JOIN security_alert_events l ON l.alert_id = a.id JOIN events e ON e.id = l.event_id
                WHERE a.lab_session_id = :session AND e.source = 'SSH_PRACTICE' AND starts_with(e.collector_record_key, :prefix)
                ORDER BY a.detected_at, a.id LIMIT 1
                """).param("session", session).param("prefix", prefix)
                .query((rs, row) -> new PracticeDetection(rs.getLong("id"), rs.getObject("detected_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    public List<Long> practiceObservationIds(long alertId, UUID runId) {
        return db.sql("""
                SELECT split_part(e.collector_record_key, ':', 3)::bigint
                FROM security_alert_events l JOIN events e ON e.id = l.event_id
                WHERE l.alert_id = :alert AND e.source = 'SSH_PRACTICE'
                  AND starts_with(e.collector_record_key, :prefix)
                """).param("alert", alertId).param("prefix", "practice:" + runId + ":")
                .query(Long.class).list();
    }

    public List<Failure> failures(UUID session, long exercise, long service, String ip, String account, Instant from, Instant to, String source) {
        return db.sql("""
                SELECT id, event_timestamp FROM events
                WHERE exercise_id = :exercise AND service_id = :service AND ssh_lab_session_id = :session
                  AND event_type = 'SSH_AUTH_FAILURE' AND source = :source AND data->>'sourceIp' = :ip AND data->>'username' = :account
                  AND event_timestamp BETWEEN :from AND :to
                ORDER BY event_timestamp, id
                """)
                .param("exercise", exercise).param("service", service).param("session", session)
                .param("ip", ip).param("account", account).param("source", source).param("from", utc(from)).param("to", utc(to))
                .query((rs, row) -> new Failure(rs.getLong("id"), rs.getObject("event_timestamp", OffsetDateTime.class).toInstant()))
                .list();
    }

    public long findOrCreateAlert(UUID session, long exercise, long service, String ip, String account,
                                  Instant first, Instant last, Instant detected, String source) {
        var existing = db.sql("""
                SELECT id FROM security_alerts a WHERE lab_session_id = :session AND service_id = :service
                  AND source_ip = :ip AND account = :account AND first_event_at <= :last AND last_event_at >= :first
                  AND EXISTS (SELECT 1 FROM security_alert_events l JOIN events e ON e.id = l.event_id
                              WHERE l.alert_id = a.id AND e.source = :source)
                ORDER BY detected_at, id LIMIT 1
                """)
                .param("session", session).param("service", service).param("ip", ip).param("account", account)
                .param("last", utc(last)).param("first", utc(first)).param("source", source).query(Long.class).optional();
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
