BEGIN;
SET LOCAL search_path TO public, pg_catalog;
SELECT pg_advisory_xact_lock(84120722);

DO $seed$
DECLARE
    scenario_key BIGINT;
    range_record RECORD;
    target_key BIGINT;
    task_record RECORD;
    expected RECORD;
    rule JSONB;
BEGIN
    SELECT id INTO STRICT scenario_key FROM scenarios
        WHERE name = 'SSH Security Incident Response' AND status = 'PUBLISHED';

    FOR expected IN SELECT * FROM (VALUES
        (1, 'Detect SSH Authentication Failures', 10, 'FAILURE_OBSERVED', 'AUTOMATIC'),
        (2, 'Identify Suspicious Login Activity', 15, 'ALERT_DETECTED', 'AUTOMATIC'),
        (3, 'Analyze SSH Authentication Logs', 15, 'LOG_ANALYZED', 'MANUAL'),
        (4, 'Identify Affected Account and Host', 15, 'IDENTITY_VERIFIED', 'MANUAL'),
        (5, 'Contain Suspicious SSH Access', 20, 'CONTAINMENT_VERIFIED', 'AUTOMATIC'),
        (6, 'Verify SSH Service Recovery', 15, 'RECOVERY_VERIFIED', 'AUTOMATIC'),
        (7, 'Submit Incident Report', 10, 'REPORT_SUBMITTED', 'AUTOMATIC')
    ) AS configuration(sequence_order, name, maximum, proof, criterion_type)
    LOOP
        SELECT * INTO STRICT task_record FROM defensive_tasks
            WHERE scenario_id = scenario_key AND sequence_order = expected.sequence_order;
        IF task_record.name <> expected.name OR task_record.max_score <> expected.maximum THEN
            RAISE EXCEPTION 'Existing SSH task has different configuration at position %', expected.sequence_order;
        END IF;

        rule := CASE WHEN expected.criterion_type = 'AUTOMATIC' THEN
            jsonb_build_object('ruleType', 'EVIDENCE_FIELD_MATCH', 'evidenceType', 'SSH_VERIFIED_TASK',
                'conditions', jsonb_build_object('proof', expected.proof), 'score', expected.maximum)
            ELSE NULL END;

        IF NOT EXISTS (SELECT FROM assessment_criteria WHERE defensive_task_id = task_record.id) THEN
            INSERT INTO assessment_criteria (defensive_task_id, name, description, criterion_type, max_score, scoring_rule)
            VALUES (task_record.id, expected.name, 'Proof required: ' || expected.proof,
                    expected.criterion_type, expected.maximum, rule);
        ELSIF (SELECT count(*) FROM assessment_criteria WHERE defensive_task_id = task_record.id
               AND name = expected.name AND criterion_type = expected.criterion_type AND max_score = expected.maximum
               AND scoring_rule IS NOT DISTINCT FROM rule) <> 1
            OR (SELECT count(*) FROM assessment_criteria WHERE defensive_task_id = task_record.id) <> 1 THEN
            RAISE EXCEPTION 'Existing Criteria conflict with assessed SSH task %', task_record.name;
        END IF;
    END LOOP;

    IF (SELECT count(*) FROM defensive_tasks WHERE scenario_id = scenario_key) <> 7 THEN
        RAISE EXCEPTION 'The SSH Scenario must have exactly seven verified task definitions';
    END IF;
    IF NOT EXISTS (SELECT FROM exercises WHERE scenario_id = scenario_key) THEN
        RAISE EXCEPTION 'Configure an Exercise and a BLUE Team using Exercise Management before seeding the SSH Lab';
    END IF;

    FOR range_record IN SELECT DISTINCT cyber_range_id FROM exercises WHERE scenario_id = scenario_key
    LOOP
        IF NOT EXISTS (SELECT FROM assets WHERE cyber_range_id = range_record.cyber_range_id AND hostname = 'ubuntu-target') THEN
            INSERT INTO assets (cyber_range_id, name, asset_type, hostname, os, status)
            VALUES (range_record.cyber_range_id, 'Ubuntu Target', 'SERVER', 'ubuntu-target', 'Ubuntu 24.04', 'OFFLINE');
        END IF;
        SELECT id INTO STRICT target_key FROM assets
            WHERE cyber_range_id = range_record.cyber_range_id AND hostname = 'ubuntu-target';

        IF NOT EXISTS (SELECT FROM services WHERE asset_id = target_key AND service_type = 'SSH') THEN
            INSERT INTO services (asset_id, name, service_type, port, protocol, status)
            VALUES (target_key, 'OpenSSH', 'SSH', 22, 'TCP', 'STOPPED');
        ELSIF (SELECT count(*) FROM services WHERE asset_id = target_key AND service_type = 'SSH') <> 1
            OR NOT EXISTS (SELECT FROM services WHERE asset_id = target_key AND service_type = 'SSH' AND port = 22 AND protocol = 'TCP') THEN
            RAISE EXCEPTION 'Existing SSH Service configuration is ambiguous or mismatched';
        END IF;
    END LOOP;
END
$seed$;

COMMIT;
