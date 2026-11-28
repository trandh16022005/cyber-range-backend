package com.cyberrange.backend.lab.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SshLabRepository {

    public record Session(UUID id, long exerciseId, long assessmentId, long assetId, long serviceId,
                          boolean desiredRunning, String status, JsonNode health, Instant heartbeatAt, Instant createdAt) {}

    public record Target(long assetId, long serviceId) {}

    private final JdbcClient db;
    private final JsonMapper json;

    public SshLabRepository(JdbcClient db, JsonMapper json) {
        this.db = db;
        this.json = json;
    }

    public void lockLifecycle() {
        db.sql("SELECT pg_advisory_xact_lock(84120721)").query((rs, row) -> true).single();
    }

    public Optional<Session> active() {
        return db.sql("SELECT * FROM ssh_lab_sessions WHERE status <> 'STOPPED'").query(this::map).optional();
    }

    public Optional<Session> latest(long exerciseId) {
        return db.sql("SELECT * FROM ssh_lab_sessions WHERE exercise_id = :exercise ORDER BY created_at DESC, id DESC LIMIT 1")
                .param("exercise", exerciseId).query(this::map).optional();
    }

    public Optional<Session> find(UUID id) {
        return db.sql("SELECT * FROM ssh_lab_sessions WHERE id = :id")
                .param("id", id).query(this::map).optional();
    }

    public Optional<Session> lock(UUID id) {
        return db.sql("SELECT * FROM ssh_lab_sessions WHERE id = :id FOR UPDATE")
                .param("id", id).query(this::map).optional();
    }

    public void insert(UUID id, long exerciseId, long assessmentId, Target target) {
        db.sql("""
                INSERT INTO ssh_lab_sessions (id, exercise_id, assessment_id, asset_id, service_id, status)
                VALUES (:id, :exercise, :assessment, :asset, :service, 'STARTING')
                """).param("id", id).param("exercise", exerciseId).param("assessment", assessmentId)
                .param("asset", target.assetId()).param("service", target.serviceId()).update();
    }

    public void requestStop(UUID id) {
        db.sql("UPDATE ssh_lab_sessions SET desired_running = false WHERE id = :id")
                .param("id", id).update();
    }

    public void recordHealth(UUID id, String status, JsonNode health) {
        db.sql("""
                UPDATE ssh_lab_sessions SET status = :status, health = CAST(:health AS jsonb),
                    heartbeat_at = clock_timestamp()
                WHERE id = :id
                """).param("id", id).param("status", status).param("health", health.toString()).update();
    }

    public Optional<Target> resolveTarget(long exerciseId) {
        return db.sql("""
                SELECT a.id AS asset_id, s.id AS service_id FROM exercises e
                JOIN assets a ON a.cyber_range_id = e.cyber_range_id AND a.hostname = 'ubuntu-target'
                JOIN services s ON s.asset_id = a.id AND s.service_type = 'SSH' AND s.port = 22 AND s.protocol = 'TCP'
                WHERE e.id = :exercise
                """).param("exercise", exerciseId).query(Target.class).optional();
    }

    private Session map(ResultSet result, int row) throws SQLException {
        String health = result.getString("health");
        OffsetDateTime heartbeat = result.getObject("heartbeat_at", OffsetDateTime.class);
        return new Session(result.getObject("id", UUID.class), result.getLong("exercise_id"),
                result.getLong("assessment_id"), result.getLong("asset_id"), result.getLong("service_id"),
                result.getBoolean("desired_running"), result.getString("status"),
                health == null ? null : json.readTree(health), heartbeat == null ? null : heartbeat.toInstant(),
                result.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
