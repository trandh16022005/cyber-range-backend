-- ============================================================
-- CYBER RANGE BACKEND
-- Database Schema - V1
-- PostgreSQL
-- ============================================================

-- ============================================================
-- 1. ROLES
-- System-level roles
-- ============================================================

CREATE TABLE roles (
                       id              BIGSERIAL PRIMARY KEY,
                       name            VARCHAR(50) NOT NULL UNIQUE,
                       description     TEXT,
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 2. USERS
-- User identity and authentication information
-- ============================================================

CREATE TABLE users (
                       id              BIGSERIAL PRIMARY KEY,
                       role_id         BIGINT NOT NULL,
                       username        VARCHAR(100) NOT NULL UNIQUE,
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   VARCHAR(255) NOT NULL,
                       full_name       VARCHAR(255) NOT NULL,
                       status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_users_role
                           FOREIGN KEY (role_id)
                               REFERENCES roles(id),

                       CONSTRAINT chk_users_status
                           CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);


-- ============================================================
-- 3. TEAMS
-- Groups of users
-- ============================================================

CREATE TABLE teams (
                       id              BIGSERIAL PRIMARY KEY,
                       name            VARCHAR(100) NOT NULL UNIQUE,
                       description     TEXT,
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. TEAM MEMBERS
-- N-N relationship between users and teams
-- ============================================================

CREATE TABLE team_members (
                              team_id         BIGINT NOT NULL,
                              user_id         BIGINT NOT NULL,
                              joined_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              PRIMARY KEY (team_id, user_id),

                              CONSTRAINT fk_team_members_team
                                  FOREIGN KEY (team_id)
                                      REFERENCES teams(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_team_members_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE
);


-- ============================================================
-- 5. SCENARIOS
-- Reusable exercise templates
-- ============================================================

CREATE TABLE scenarios (
                           id              BIGSERIAL PRIMARY KEY,
                           name            VARCHAR(150) NOT NULL,
                           description     TEXT,
                           type            VARCHAR(50) NOT NULL,
                           difficulty      VARCHAR(30),
                           status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                           created_by      BIGINT,
                           created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_scenarios_created_by
                               FOREIGN KEY (created_by)
                                   REFERENCES users(id)
                                   ON DELETE SET NULL,

                           CONSTRAINT chk_scenarios_status
                               CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);


-- ============================================================
-- 6. DEFENSIVE TASKS
-- Blue Team tasks belonging to a scenario
-- ============================================================

CREATE TABLE defensive_tasks (
                                 id              BIGSERIAL PRIMARY KEY,
                                 scenario_id     BIGINT NOT NULL,
                                 name            VARCHAR(150) NOT NULL,
                                 description     TEXT,
                                 task_type       VARCHAR(50) NOT NULL,
                                 sequence_order  INTEGER NOT NULL DEFAULT 1,
                                 max_score       NUMERIC(5,2) NOT NULL DEFAULT 0,
                                 required        BOOLEAN NOT NULL DEFAULT TRUE,

                                 CONSTRAINT fk_defensive_tasks_scenario
                                     FOREIGN KEY (scenario_id)
                                         REFERENCES scenarios(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT chk_defensive_tasks_sequence
                                     CHECK (sequence_order > 0),

                                 CONSTRAINT chk_defensive_tasks_score
                                     CHECK (max_score >= 0),

                                 CONSTRAINT uq_defensive_task_order
                                     UNIQUE (scenario_id, sequence_order)
);


-- ============================================================
-- 7. ASSESSMENT CRITERIA
-- Scoring criteria for defensive tasks
-- ============================================================

CREATE TABLE assessment_criteria (
                                     id                  BIGSERIAL PRIMARY KEY,
                                     defensive_task_id   BIGINT NOT NULL,
                                     name                VARCHAR(150) NOT NULL,
                                     description         TEXT,
                                     criterion_type      VARCHAR(50) NOT NULL,
                                     max_score           NUMERIC(5,2) NOT NULL,

                                     CONSTRAINT fk_assessment_criteria_task
                                         FOREIGN KEY (defensive_task_id)
                                             REFERENCES defensive_tasks(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT chk_assessment_criteria_score
                                         CHECK (max_score >= 0)
);


-- ============================================================
-- 8. CYBER RANGES
-- Cyber Range environment definitions
-- ============================================================

CREATE TABLE cyber_ranges (
                              id              BIGSERIAL PRIMARY KEY,
                              name            VARCHAR(150) NOT NULL UNIQUE,
                              description     TEXT,
                              environment_type VARCHAR(50),
                              status          VARCHAR(30) NOT NULL DEFAULT 'INACTIVE',
                              created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT chk_cyber_ranges_status
                                  CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);


-- ============================================================
-- 9. ASSETS
-- Generic machines / servers / network devices
-- ============================================================

CREATE TABLE assets (
                        id              BIGSERIAL PRIMARY KEY,
                        cyber_range_id  BIGINT NOT NULL,
                        name            VARCHAR(150) NOT NULL,
                        asset_type      VARCHAR(50) NOT NULL,
                        hostname        VARCHAR(255),
                        ip_address      INET,
                        os              VARCHAR(100),
                        status          VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',

                        CONSTRAINT fk_assets_cyber_range
                            FOREIGN KEY (cyber_range_id)
                                REFERENCES cyber_ranges(id)
                                ON DELETE CASCADE,

                        CONSTRAINT chk_assets_status
                            CHECK (status IN ('ONLINE', 'OFFLINE', 'MAINTENANCE'))
);


-- ============================================================
-- 10. SERVICES
-- Services running on assets
-- ============================================================

CREATE TABLE services (
                          id              BIGSERIAL PRIMARY KEY,
                          asset_id        BIGINT NOT NULL,
                          name            VARCHAR(150) NOT NULL,
                          service_type    VARCHAR(50) NOT NULL,
                          port            INTEGER,
                          protocol        VARCHAR(20),
                          status          VARCHAR(30) NOT NULL DEFAULT 'STOPPED',

                          CONSTRAINT fk_services_asset
                              FOREIGN KEY (asset_id)
                                  REFERENCES assets(id)
                                  ON DELETE CASCADE,

                          CONSTRAINT chk_services_port
                              CHECK (port IS NULL OR (port >= 1 AND port <= 65535)),

                          CONSTRAINT chk_services_status
                              CHECK (status IN ('RUNNING', 'STOPPED', 'FAILED'))
);


-- ============================================================
-- 11. EXERCISES
-- Actual execution of a scenario
-- ============================================================

CREATE TABLE exercises (
                           id              BIGSERIAL PRIMARY KEY,
                           scenario_id     BIGINT NOT NULL,
                           cyber_range_id  BIGINT NOT NULL,
                           name            VARCHAR(150) NOT NULL,
                           status          VARCHAR(30) NOT NULL DEFAULT 'CREATED',
                           started_at      TIMESTAMPTZ,
                           ended_at        TIMESTAMPTZ,
                           created_by      BIGINT,
                           created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_exercises_scenario
                               FOREIGN KEY (scenario_id)
                                   REFERENCES scenarios(id),

                           CONSTRAINT fk_exercises_cyber_range
                               FOREIGN KEY (cyber_range_id)
                                   REFERENCES cyber_ranges(id),

                           CONSTRAINT fk_exercises_created_by
                               FOREIGN KEY (created_by)
                                   REFERENCES users(id)
                                   ON DELETE SET NULL,

                           CONSTRAINT chk_exercises_status
                               CHECK (
                                   status IN (
                                              'CREATED',
                                              'RUNNING',
                                              'PAUSED',
                                              'COMPLETED',
                                              'CANCELLED',
                                              'RESET'
                                       )
                                   ),

                           CONSTRAINT chk_exercises_time
                               CHECK (
                                   ended_at IS NULL
                                       OR started_at IS NULL
                                       OR ended_at >= started_at
                                   )
);


-- ============================================================
-- 12. EXERCISE PARTICIPANTS
-- Users / teams participating in an exercise
-- Exercise roles:
-- RED / BLUE / WHITE
-- ============================================================

CREATE TABLE exercise_participants (
                                       id              BIGSERIAL PRIMARY KEY,
                                       exercise_id     BIGINT NOT NULL,
                                       user_id         BIGINT NOT NULL,
                                       team_id         BIGINT,
                                       role            VARCHAR(20) NOT NULL,
                                       joined_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_exercise_participants_exercise
                                           FOREIGN KEY (exercise_id)
                                               REFERENCES exercises(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT fk_exercise_participants_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES users(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT fk_exercise_participants_team
                                           FOREIGN KEY (team_id)
                                               REFERENCES teams(id)
                                               ON DELETE SET NULL,

                                       CONSTRAINT chk_exercise_participants_role
                                           CHECK (role IN ('RED', 'BLUE', 'WHITE')),

                                       CONSTRAINT uq_exercise_participant
                                           UNIQUE (exercise_id, user_id)
);


-- ============================================================
-- 13. EVENTS
-- Raw / observed events from Cyber Range
-- ============================================================

CREATE TABLE events (
                        id              BIGSERIAL PRIMARY KEY,
                        exercise_id     BIGINT NOT NULL,
                        asset_id        BIGINT,
                        service_id      BIGINT,
                        event_type      VARCHAR(50) NOT NULL,
                        severity        VARCHAR(20),
                        event_timestamp TIMESTAMPTZ NOT NULL,
                        source          VARCHAR(100),
                        data            JSONB,
                        created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_events_exercise
                            FOREIGN KEY (exercise_id)
                                REFERENCES exercises(id)
                                ON DELETE CASCADE,

                        CONSTRAINT fk_events_asset
                            FOREIGN KEY (asset_id)
                                REFERENCES assets(id)
                                ON DELETE SET NULL,

                        CONSTRAINT fk_events_service
                            FOREIGN KEY (service_id)
                                REFERENCES services(id)
                                ON DELETE SET NULL
);


-- ============================================================
-- 14. EVIDENCE
-- Assessment-relevant evidence extracted from events
-- ============================================================

CREATE TABLE evidence (
                          id              BIGSERIAL PRIMARY KEY,
                          exercise_id     BIGINT NOT NULL,
                          event_id        BIGINT,
                          evidence_type   VARCHAR(50) NOT NULL,
                          data            JSONB,
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_evidence_exercise
                              FOREIGN KEY (exercise_id)
                                  REFERENCES exercises(id)
                                  ON DELETE CASCADE,

                          CONSTRAINT fk_evidence_event
                              FOREIGN KEY (event_id)
                                  REFERENCES events(id)
                                  ON DELETE SET NULL
);


-- ============================================================
-- 15. ASSESSMENTS
-- Main assessment for an exercise
-- V1: one assessment per exercise
-- ============================================================

CREATE TABLE assessments (
                             id              BIGSERIAL PRIMARY KEY,
                             exercise_id     BIGINT NOT NULL UNIQUE,
                             status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                             total_score     NUMERIC(7,2) NOT NULL DEFAULT 0,
                             started_at      TIMESTAMPTZ,
                             completed_at    TIMESTAMPTZ,
                             review_status   VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                             reviewed_by     BIGINT,
                             reviewed_at     TIMESTAMPTZ,
                             review_comment  TEXT,
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_assessments_exercise
                                 FOREIGN KEY (exercise_id)
                                     REFERENCES exercises(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_assessments_reviewed_by
                                 FOREIGN KEY (reviewed_by)
                                     REFERENCES users(id)
                                     ON DELETE SET NULL,

                             CONSTRAINT chk_assessments_status
                                 CHECK (
                                     status IN (
                                                'PENDING',
                                                'RUNNING',
                                                'COMPLETED',
                                                'FAILED'
                                         )
                                     ),

                             CONSTRAINT chk_assessments_review_status
                                 CHECK (
                                     review_status IN (
                                                       'PENDING',
                                                       'REVIEWED'
                                         )
                                     ),

                             CONSTRAINT chk_assessments_score
                                 CHECK (total_score >= 0)
);


-- ============================================================
-- 16. ASSESSMENT RESULTS
-- Result of each defensive task
-- ============================================================

CREATE TABLE assessment_results (
                                    id                  BIGSERIAL PRIMARY KEY,
                                    assessment_id       BIGINT NOT NULL,
                                    defensive_task_id   BIGINT NOT NULL,
                                    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                    detected_at         TIMESTAMPTZ,
                                    responded_at        TIMESTAMPTZ,
                                    completed_at        TIMESTAMPTZ,
                                    detection_time      INTERVAL,
                                    response_time       INTERVAL,
                                    reason              TEXT,

                                    CONSTRAINT fk_assessment_results_assessment
                                        FOREIGN KEY (assessment_id)
                                            REFERENCES assessments(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_assessment_results_task
                                        FOREIGN KEY (defensive_task_id)
                                            REFERENCES defensive_tasks(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT chk_assessment_results_status
                                        CHECK (
                                            status IN (
                                                       'PENDING',
                                                       'IN_PROGRESS',
                                                       'COMPLETED',
                                                       'FAILED',
                                                       'NOT_APPLICABLE'
                                                )
                                            ),

                                    CONSTRAINT uq_assessment_task
                                        UNIQUE (assessment_id, defensive_task_id)
);


-- ============================================================
-- 17. ASSESSMENT SCORES
-- Detailed score awarded for each criterion
-- ============================================================

CREATE TABLE assessment_scores (
                                   id                      BIGSERIAL PRIMARY KEY,
                                   assessment_result_id    BIGINT NOT NULL,
                                   criterion_id            BIGINT NOT NULL,
                                   awarded_score           NUMERIC(5,2) NOT NULL DEFAULT 0,
                                   max_score               NUMERIC(5,2) NOT NULL,
                                   reason                  TEXT,

                                   CONSTRAINT fk_assessment_scores_result
                                       FOREIGN KEY (assessment_result_id)
                                           REFERENCES assessment_results(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_assessment_scores_criterion
                                       FOREIGN KEY (criterion_id)
                                           REFERENCES assessment_criteria(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT chk_assessment_scores_awarded
                                       CHECK (awarded_score >= 0),

                                   CONSTRAINT chk_assessment_scores_max
                                       CHECK (max_score >= 0),

                                   CONSTRAINT chk_assessment_scores_range
                                       CHECK (awarded_score <= max_score),

                                   CONSTRAINT uq_assessment_result_criterion
                                       UNIQUE (assessment_result_id, criterion_id)
);


-- ============================================================
-- 18. INCIDENT REPORTS
-- Blue Team incident reports
-- ============================================================

CREATE TABLE incident_reports (
                                  id              BIGSERIAL PRIMARY KEY,
                                  exercise_id     BIGINT NOT NULL,
                                  submitted_by    BIGINT NOT NULL,
                                  title           VARCHAR(255) NOT NULL,
                                  description     TEXT NOT NULL,
                                  severity        VARCHAR(20),
                                  status          VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
                                  submitted_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  reviewed_by     BIGINT,
                                  reviewed_at     TIMESTAMPTZ,
                                  review_comment  TEXT,

                                  CONSTRAINT fk_incident_reports_exercise
                                      FOREIGN KEY (exercise_id)
                                          REFERENCES exercises(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_incident_reports_submitted_by
                                      FOREIGN KEY (submitted_by)
                                          REFERENCES users(id),

                                  CONSTRAINT fk_incident_reports_reviewed_by
                                      FOREIGN KEY (reviewed_by)
                                          REFERENCES users(id)
                                          ON DELETE SET NULL,

                                  CONSTRAINT chk_incident_reports_status
                                      CHECK (
                                          status IN (
                                                     'DRAFT',
                                                     'SUBMITTED',
                                                     'REVIEWED'
                                              )
                                          )
);


-- ============================================================
-- 19. AUDIT LOGS
-- System-level audit trail
-- ============================================================

CREATE TABLE audit_logs (
                            id              BIGSERIAL PRIMARY KEY,
                            user_id         BIGINT,
                            action          VARCHAR(100) NOT NULL,
                            entity_type     VARCHAR(100),
                            entity_id       BIGINT,
                            created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            metadata        JSONB,

                            CONSTRAINT fk_audit_logs_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE SET NULL
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_role_id
    ON users(role_id);

CREATE INDEX idx_team_members_user_id
    ON team_members(user_id);

CREATE INDEX idx_defensive_tasks_scenario_id
    ON defensive_tasks(scenario_id);

CREATE INDEX idx_assessment_criteria_task_id
    ON assessment_criteria(defensive_task_id);

CREATE INDEX idx_assets_cyber_range_id
    ON assets(cyber_range_id);

CREATE INDEX idx_services_asset_id
    ON services(asset_id);

CREATE INDEX idx_exercises_scenario_id
    ON exercises(scenario_id);

CREATE INDEX idx_exercises_cyber_range_id
    ON exercises(cyber_range_id);

CREATE INDEX idx_exercises_created_by
    ON exercises(created_by);

CREATE INDEX idx_exercise_participants_exercise_id
    ON exercise_participants(exercise_id);

CREATE INDEX idx_exercise_participants_user_id
    ON exercise_participants(user_id);

CREATE INDEX idx_exercise_participants_team_id
    ON exercise_participants(team_id);

CREATE INDEX idx_events_exercise_id
    ON events(exercise_id);

CREATE INDEX idx_events_asset_id
    ON events(asset_id);

CREATE INDEX idx_events_service_id
    ON events(service_id);

CREATE INDEX idx_events_timestamp
    ON events(event_timestamp);

CREATE INDEX idx_evidence_exercise_id
    ON evidence(exercise_id);

CREATE INDEX idx_evidence_event_id
    ON evidence(event_id);

CREATE INDEX idx_assessment_results_assessment_id
    ON assessment_results(assessment_id);

CREATE INDEX idx_assessment_results_task_id
    ON assessment_results(defensive_task_id);

CREATE INDEX idx_assessment_scores_result_id
    ON assessment_scores(assessment_result_id);

CREATE INDEX idx_assessment_scores_criterion_id
    ON assessment_scores(criterion_id);

CREATE INDEX idx_incident_reports_exercise_id
    ON incident_reports(exercise_id);

CREATE INDEX idx_incident_reports_submitted_by
    ON incident_reports(submitted_by);

CREATE INDEX idx_incident_reports_reviewed_by
    ON incident_reports(reviewed_by);

CREATE INDEX idx_audit_logs_user_id
    ON audit_logs(user_id);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs(entity_type, entity_id);


-- ============================================================
-- END OF SCHEMA
-- ============================================================