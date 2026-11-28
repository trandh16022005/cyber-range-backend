package com.cyberrange.backend.lab.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SshPracticeRepository {

    public record Run(UUID id, UUID labSessionId, long exerciseId, long assessmentId, long redTeamId, long blueTeamId,
                      int version, String state, String connectionStatus, OffsetDateTime connectionAt,
                      JsonNode submission, Integer score) {}
    public record History(Long actorId, String action, String detail, OffsetDateTime createdAt) {}
    public record ObservedEvent(long id, String eventType, String severity, OffsetDateTime eventTimestamp,
                                OffsetDateTime collectedAt, String sourceAddress, String account, String target,
                                String outcome, String raw, String provenance, Long alertId) {}

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public SshPracticeRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public boolean schemaReady() {
        return jdbc.sql("""
                SELECT to_regclass('public.ssh_practice_runs') IS NOT NULL
                   AND to_regclass('public.ssh_practice_history') IS NOT NULL
                   AND to_regclass('public.uq_ssh_practice_reviewed_assessment') IS NOT NULL
                """).query(Boolean.class).single();
    }

    public void create(UUID id, UUID session, long exercise, long assessment, long red, long blue, int version) {
        jdbc.sql("""
                INSERT INTO ssh_practice_runs(id,lab_session_id,exercise_id,assessment_id,red_team_id,blue_team_id,scenario_version,state)
                VALUES (:id,:session,:exercise,:assessment,:red,:blue,:version,'APPROVED')
                ON CONFLICT (lab_session_id) DO NOTHING
                """).param("id", id).param("session", session).param("exercise", exercise).param("assessment", assessment)
                .param("red", red).param("blue", blue).param("version", version).update();
    }

    public Optional<Run> forSession(UUID session) {
        return jdbc.sql("SELECT * FROM ssh_practice_runs WHERE lab_session_id=:session")
                .param("session", session).query(this::map).optional();
    }

    public List<Run> forActor(long actor) {
        return jdbc.sql("""
                SELECT r.* FROM ssh_practice_runs r JOIN exercise_participants p ON p.exercise_id=r.exercise_id
                WHERE p.user_id=:actor AND (p.role='WHITE'
                    OR (p.role='RED' AND p.team_id=r.red_team_id) OR (p.role='BLUE' AND p.team_id=r.blue_team_id))
                ORDER BY r.updated_at DESC
                """).param("actor", actor).query(this::map).list();
    }

    public List<Run> forAssessment(long assessment) {
        return jdbc.sql("SELECT * FROM ssh_practice_runs WHERE assessment_id=:assessment ORDER BY updated_at DESC")
                .param("assessment", assessment).query(this::map).list();
    }

    public Optional<Run> lock(UUID id) {
        return jdbc.sql("SELECT * FROM ssh_practice_runs WHERE id=:id FOR UPDATE")
                .param("id", id).query(this::map).optional();
    }

    public void save(UUID id, String state, JsonNode submission, Integer score) {
        jdbc.sql("""
                UPDATE ssh_practice_runs SET state=:state, submission=CAST(:data AS jsonb), score=:score,
                    updated_at=clock_timestamp() WHERE id=:id
                """).param("state", state).param("data", submission.toString())
                .param("score", score, java.sql.Types.INTEGER).param("id", id).update();
    }

    public void history(UUID id, Long actor, String action, String detail) {
        jdbc.sql("INSERT INTO ssh_practice_history(run_id,actor_id,action,detail) VALUES (:id,:actor,:action,:detail)")
                .param("id", id).param("actor", actor, java.sql.Types.BIGINT)
                .param("action", action).param("detail", detail, java.sql.Types.VARCHAR).update();
    }

    public List<History> history(UUID id) {
        return jdbc.sql("SELECT actor_id,action,detail,created_at FROM ssh_practice_history WHERE run_id=:id ORDER BY id")
                .param("id", id).query(History.class).list();
    }

    public List<ObservedEvent> observedEvents(UUID session, long exercise) {
        return jdbc.sql("""
                SELECT e.id,event_type,severity,event_timestamp,e.created_at AS collected_at,
                       data->>'sourceIp' AS source_address, data->>'username' AS account,
                       data->>'targetHost' AS target, data->>'authenticationOutcome' AS outcome,
                       CASE WHEN source='SSH_PRACTICE' THEN data->>'raw' END AS raw,
                       data->>'provenance' AS provenance,
                       (SELECT min(l.alert_id) FROM security_alert_events l WHERE l.event_id=e.id) AS alert_id
                FROM events e WHERE ssh_lab_session_id=:session AND exercise_id=:exercise
                    AND source IN ('OPENSSH_COLLECTOR', 'SSH_PRACTICE')
                ORDER BY event_timestamp DESC,e.id DESC LIMIT 100
                """).param("session", session).param("exercise", exercise).query((rs, row) -> new ObservedEvent(
                rs.getLong("id"), rs.getString("event_type"), rs.getString("severity"),
                rs.getObject("event_timestamp", OffsetDateTime.class), rs.getObject("collected_at", OffsetDateTime.class),
                rs.getString("source_address"), rs.getString("account"), rs.getString("target"),
                rs.getString("outcome"), rs.getString("raw"), rs.getString("provenance"),
                rs.getObject("alert_id", Long.class))).list();
    }

    public boolean connection(UUID id, String from, String to) {
        return jdbc.sql("""
                UPDATE ssh_practice_runs SET connection_status=:to,connection_at=clock_timestamp()
                WHERE id=:id AND connection_status=:from
                """).param("id", id).param("from", from).param("to", to).update() == 1;
    }

    private Run map(ResultSet row, int number) throws SQLException {
        return new Run(row.getObject("id", UUID.class), row.getObject("lab_session_id", UUID.class),
                row.getLong("exercise_id"), row.getLong("assessment_id"), row.getLong("red_team_id"),
                row.getLong("blue_team_id"), row.getInt("scenario_version"), row.getString("state"),
                row.getString("connection_status"), row.getObject("connection_at", OffsetDateTime.class),
                json.readTree(row.getString("submission")), row.getObject("score", Integer.class));
    }
}
