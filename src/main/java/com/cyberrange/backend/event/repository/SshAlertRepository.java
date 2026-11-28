package com.cyberrange.backend.event.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SshAlertRepository {

    public record Alert(long id, long exerciseId, long serviceId, UUID labSessionId, String sourceIp, String account,
                        OffsetDateTime firstEventAt, OffsetDateTime lastEventAt, OffsetDateTime detectedAt,
                        String severity, String status, Long assessmentId, Long reportId) {}

    public record Action(long id, long alertId, long actorId, String action, String detail,
                         OffsetDateTime createdAt, OffsetDateTime verifiedAt) {}

    private final JdbcClient db;

    public SshAlertRepository(JdbcClient db) {
        this.db = db;
    }

    public List<Alert> list(long exerciseId, String status) {
        var query = db.sql("""
                SELECT * FROM security_alerts WHERE exercise_id = :exercise
                    AND (:status = '' OR status = :status)
                ORDER BY detected_at DESC, id DESC LIMIT 100
                """);
        return query.param("exercise", exerciseId).param("status", status).query(Alert.class).list();
    }

    public List<Alert> listForTeam(long exerciseId, String status, long teamId) {
        return db.sql("""
                SELECT alert.* FROM security_alerts alert
                JOIN ssh_lab_sessions lab ON lab.id = alert.lab_session_id
                JOIN assessments a ON a.id = COALESCE(alert.assessment_id, lab.assessment_id)
                WHERE alert.exercise_id = :exercise AND a.exercise_id = :exercise AND a.team_id = :team
                    AND (:status = '' OR alert.status = :status)
                ORDER BY alert.detected_at DESC, alert.id DESC LIMIT 100
                """).param("exercise", exerciseId).param("status", status).param("team", teamId)
                .query(Alert.class).list();
    }

    public Optional<Alert> find(long exerciseId, long id, boolean lock) {
        return db.sql("SELECT * FROM security_alerts WHERE exercise_id = :exercise AND id = :id"
                        + (lock ? " FOR UPDATE" : ""))
                .param("exercise", exerciseId).param("id", id).query(Alert.class).optional();
    }

    public List<Long> eventIds(long id) {
        return db.sql("""
                SELECT e.id FROM security_alert_events l JOIN events e ON e.id = l.event_id
                WHERE l.alert_id = :id ORDER BY e.event_timestamp, e.id
                """).param("id", id).query(Long.class).list();
    }

    public boolean isSimulated(long alertId) {
        return db.sql("""
                SELECT EXISTS (SELECT 1 FROM security_alert_events l JOIN events e ON e.id = l.event_id
                               WHERE l.alert_id = :id AND e.source = 'SSH_PRACTICE')
                """).param("id", alertId).query(Boolean.class).single();
    }

    public List<Action> history(long id) {
        return db.sql("SELECT * FROM ssh_response_actions WHERE alert_id = :id ORDER BY id")
                .param("id", id).query(Action.class).list();
    }

    public void assign(long id, long assessment) {
        db.sql("UPDATE security_alerts SET assessment_id = :assessment, status = 'ACKNOWLEDGED' WHERE id = :id")
                .param("id", id).param("assessment", assessment).update();
    }

    public void status(long id, String status) {
        db.sql("UPDATE security_alerts SET status = :status WHERE id = :id").param("id", id).param("status", status).update();
    }

    public void attachReport(long id, long report) {
        db.sql("UPDATE security_alerts SET report_id = :report WHERE id = :id").param("id", id).param("report", report).update();
    }

    public void append(long id, long actor, String action, String detail, boolean verified) {
        db.sql("""
                INSERT INTO ssh_response_actions (alert_id, actor_id, action, detail, verified_at)
                VALUES (:alert, :actor, :action, :detail, CASE WHEN :verified THEN clock_timestamp() END)
                """).param("alert", id).param("actor", actor).param("action", action).param("detail", detail)
                .param("verified", verified).update();
    }

    public Optional<Long> nextContainment(UUID session) {
        return db.sql("""
                SELECT a.id FROM security_alerts a JOIN ssh_response_actions r ON r.alert_id = a.id
                WHERE a.lab_session_id = :session AND a.status = 'IN_PROGRESS' AND r.action = 'CONTAINMENT_REQUESTED'
                AND NOT EXISTS (SELECT 1 FROM ssh_response_actions later WHERE later.alert_id = a.id
                    AND later.id > r.id AND later.action IN ('RECOVERY_VERIFIED','CONTAINMENT_FAILED'))
                ORDER BY r.id LIMIT 1
                """).param("session", session).query(Long.class).optional();
    }
}
