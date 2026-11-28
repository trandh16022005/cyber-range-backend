\set ON_ERROR_STOP on
BEGIN;
DO $guard$
BEGIN
    IF current_database() <> 'cyber_range_test' OR inet_server_addr() IS DISTINCT FROM '127.0.0.1'::inet
        OR COALESCE(inet_server_port(), 0) NOT IN (55439, 55440) THEN
        RAISE EXCEPTION 'This fixture is restricted to the isolated test database';
    END IF;
END
$guard$;

INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO users (role_id, username, email, password_hash, full_name)
SELECT id, 'ssh-fixture-admin', 'ssh-fixture-admin@example.invalid', 'disabled-fixture-login', 'Fixture Admin'
FROM roles WHERE name = 'ADMIN'
ON CONFLICT (username) DO NOTHING;
INSERT INTO teams (name) VALUES ('SSH Fixture Blue') ON CONFLICT (name) DO NOTHING;
INSERT INTO cyber_ranges (name, environment_type, status) VALUES ('SSH Fixture Range', 'DOCKER', 'ACTIVE')
ON CONFLICT (name) DO NOTHING;
INSERT INTO scenarios (name, type, status)
SELECT 'SSH Security Incident Response', 'SSH_SECURITY', 'PUBLISHED'
WHERE NOT EXISTS (SELECT FROM scenarios WHERE name = 'SSH Security Incident Response');
INSERT INTO defensive_tasks (scenario_id, sequence_order, name, max_score, task_type)
SELECT s.id, t.position, t.name, t.score, 'DEFENSIVE'
FROM scenarios s CROSS JOIN (VALUES
    (1, 'Detect SSH Authentication Failures', 10),
    (2, 'Identify Suspicious Login Activity', 15),
    (3, 'Analyze SSH Authentication Logs', 15),
    (4, 'Identify Affected Account and Host', 15),
    (5, 'Contain Suspicious SSH Access', 20),
    (6, 'Verify SSH Service Recovery', 15),
    (7, 'Submit Incident Report', 10)
) t(position, name, score)
WHERE s.name = 'SSH Security Incident Response'
ON CONFLICT (scenario_id, sequence_order) DO NOTHING;
INSERT INTO exercises (scenario_id, cyber_range_id, name, status)
SELECT s.id, r.id, 'SSH Fixture Exercise', 'CREATED' FROM scenarios s CROSS JOIN cyber_ranges r
WHERE s.name = 'SSH Security Incident Response' AND r.name = 'SSH Fixture Range'
AND NOT EXISTS (SELECT FROM exercises WHERE name = 'SSH Fixture Exercise');
INSERT INTO exercise_participants (exercise_id, user_id, team_id, role)
SELECT e.id, u.id, t.id, 'BLUE' FROM exercises e CROSS JOIN users u CROSS JOIN teams t
WHERE e.name = 'SSH Fixture Exercise' AND u.username = 'ssh-fixture-admin' AND t.name = 'SSH Fixture Blue'
ON CONFLICT (exercise_id, user_id) DO NOTHING;
COMMIT;
