package com.cyberrange.backend.incident.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class IncidentReportRepository {

    public record Report(long id, long exerciseId, long teamId, long submittedBy, String title, String description,
                         String severity, String status, OffsetDateTime submittedAt) {}

    private static final String COLUMNS = "id, exercise_id, team_id, submitted_by, title, description, severity, status, submitted_at";
    private final JdbcClient db;

    public IncidentReportRepository(JdbcClient db) {
        this.db = db;
    }

    public Report submit(long exercise, long team, long actor, String title, String description, String severity) {
        return db.sql("""
                INSERT INTO incident_reports (exercise_id, team_id, submitted_by, title, description, severity, status, submitted_at)
                VALUES (:exercise, :team, :actor, :title, :description, :severity, 'SUBMITTED', clock_timestamp())
                RETURNING
                """ + COLUMNS)
                .param("exercise", exercise).param("team", team).param("actor", actor)
                .param("title", title).param("description", description).param("severity", severity)
                .query(Report.class).single();
    }

    public List<Report> list(long exercise) {
        return db.sql("SELECT " + COLUMNS + " FROM incident_reports WHERE exercise_id = :exercise ORDER BY id DESC")
                .param("exercise", exercise).query(Report.class).list();
    }

    public Optional<Report> find(long id) {
        return db.sql("SELECT " + COLUMNS + " FROM incident_reports WHERE id = :id").param("id", id)
                .query(Report.class).optional();
    }
}
