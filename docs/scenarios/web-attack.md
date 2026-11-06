# Web Attack Scenario

## 1. Scenario Overview

The Web Attack scenario trains participants to identify and investigate suspicious activity targeting a web application.

Participants act as Blue Team defenders responsible for monitoring web access logs, identifying suspicious HTTP requests, investigating relevant application events, and documenting their findings.

All activities take place within the isolated Cyber Range environment.

## 2. Scenario Objectives

By completing this scenario, participants should be able to:

- Monitor web application activity.
- Identify suspicious HTTP request patterns.
- Recognize potential web attacks.
- Determine the affected application or asset.
- Collect relevant web and application evidence.
- Submit an incident report.

## 3. Environment Requirements

| Component | Purpose |
|---|---|
| Web Server | Hosts the training application |
| Web Application | Provides the application environment |
| Web Access Logs | Records HTTP request activity |
| Application Logs | Provides application-level events |
| Log Collection | Collects relevant web events |
| Cyber Range Backend | Manages exercise and assessment data |
| Blue Team Interface | Supports investigation and reporting |

The web application must be deployed in an isolated training environment.

## 4. Simulated Security Activity

The scenario introduces controlled web request events representing suspicious application activity.

Depending on the scenario configuration, generated events may represent:

- Unusual request patterns.
- Repeated requests to sensitive application resources.
- Unexpected request parameters.
- Suspicious HTTP response patterns.
- Application errors associated with abnormal requests.

The simulation should use predefined test requests, sanitized logs, or a controlled event generator.

No real production application or external service should be targeted.

## 5. Blue Team Tasks

### Task 1: Monitor Web Activity

Participants review web access logs and application events.

Expected outcome:

- Relevant web events are identified.
- Unusual request patterns are recognized.

### Task 2: Detect Web Attack

Participants determine whether the observed activity represents suspicious web application behavior.

Expected outcome:

- The activity is correctly classified.
- Relevant web and application evidence is provided.

### Task 3: Identify the Affected Application or Asset

Participants identify the application or asset associated with the suspicious activity.

Expected outcome:

- The affected application or asset is identified when supported by available evidence.
- The finding is documented.

### Task 4: Submit an Incident Report

Participants document the observed activity and investigation results.

Expected outcome:

- The report describes the suspicious web activity.
- Relevant timestamps and affected assets are included.
- Supporting evidence is referenced.
- The detection outcome is documented.

## 6. Event and Evidence Requirements

The system should collect relevant web security information, including:

- Event timestamp.
- HTTP method.
- Requested resource or endpoint.
- HTTP response status.
- Source address, when available.
- Target application or asset.
- User agent, when relevant.
- Application event classification.
- Related exercise identifier.

Sensitive information such as session tokens, authentication cookies, passwords, and personal data must be excluded or appropriately sanitized.

Evidence should preserve the information needed for investigation without exposing secrets.

## 7. Assessment Criteria

The scenario uses the following V1 defensive task:

| Assessment Item | Expected Result | Maximum Points |
|---|---|---:|
| Detect Web Attack | Correctly identify suspicious web application activity | 15 |
| Identify Affected Application or Asset | Identify the relevant application or asset | Supporting criterion, not an additional scoring category |
| Incident Report | Submit a report containing required information | 10 |

The task weights must remain consistent with the V1 scoring configuration.

Identifying the affected application or asset supports the relevant assessment criteria. It must not introduce an additional scoring category unless the approved scoring configuration is updated.

## 8. Metrics

The scenario may provide:

- Detection Time.
- Response Time, when a verified response is part of the exercise.
- Incident Reporting Completeness.

The metrics must be calculated using reliable timestamps and verified system events.

## 9. Expected Outcome

A successful participant should:

- Recognize suspicious web application activity.
- Correctly identify the affected application or asset when supported by evidence.
- Provide relevant web and application logs.
- Submit an appropriate incident report.

The Assessment Engine verifies the defensive task results, while the Scoring Engine calculates the corresponding score.

## 10. Safety and Reset Requirements

- The web application must remain isolated within the Cyber Range.
- Simulated activity must not target external websites.
- Use designated training data and test accounts.
- Sensitive request information must be sanitized before storage.
- The environment should support reset after exercise completion.
- Evidence must remain associated with the correct exercise.