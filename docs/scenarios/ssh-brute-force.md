# SSH Brute Force Scenario

## 1. Scenario Overview

The SSH Brute Force scenario trains participants to identify suspicious authentication activity involving an SSH service.

Participants act as Blue Team defenders responsible for monitoring authentication events, identifying unusual login patterns, investigating affected accounts or assets, and documenting their findings.

All activities take place within the isolated Cyber Range environment.

## 2. Scenario Objectives

By completing this scenario, participants should be able to:

- Monitor SSH authentication events.
- Recognize suspicious repeated login failures.
- Identify potentially affected accounts and assets.
- Investigate authentication-related evidence.
- Record the detection outcome.
- Submit an incident report.

## 3. Environment Requirements

| Component | Purpose |
|---|---|
| Linux Server | Provides the SSH training service |
| SSH Service | Generates authentication events |
| Authentication Logs | Records login activity |
| Log Collection | Collects relevant authentication events |
| Cyber Range Backend | Manages exercise and assessment data |
| Blue Team Interface | Supports investigation and reporting |

The SSH service must be configured exclusively for the isolated training environment.

## 4. Simulated Security Activity

The scenario introduces controlled authentication events representing a brute-force attempt.

The generated events may include:

- Repeated unsuccessful authentication attempts.
- Multiple failed login events involving an account.
- Authentication attempts from an unusual source.
- A successful authentication event following suspicious activity, if required by the scenario configuration.

The simulation should use predefined test events or a controlled authentication simulator.

It must not involve attempts to access external systems or accounts.

## 5. Blue Team Tasks

### Task 1: Monitor SSH Authentication Events

Participants review authentication logs and identify unusual login activity.

Expected outcome:

- Relevant authentication events are identified.
- Suspicious login patterns are recognized.

### Task 2: Detect SSH Brute Force

Participants determine whether the authentication activity represents a suspicious brute-force attempt.

Expected outcome:

- The activity is correctly classified.
- The detection is supported by relevant evidence.

### Task 3: Identify Affected Account or Asset

Participants determine which account or server is associated with the suspicious activity.

Expected outcome:

- Relevant account and asset information is identified when available.
- Findings are supported by authentication logs.

### Task 4: Submit an Incident Report

Participants document the authentication activity and investigation results.

Expected outcome:

- The report describes the suspicious login activity.
- Relevant timestamps, accounts, assets, and evidence are included.
- The detection outcome is recorded.

## 6. Event and Evidence Requirements

The system should collect relevant authentication information, including:

- Event timestamp.
- Source address, when available.
- Target server or asset.
- Account identifier, when available.
- Authentication result.
- Authentication method, when available.
- Event classification.
- Related exercise identifier.

Authentication information should be handled carefully. Passwords, private keys, and other authentication secrets must never be stored as assessment evidence.

Evidence should contain sufficient information to verify suspicious activity without exposing credentials.

## 7. Assessment Criteria

The scenario uses the following V1 defensive task:

| Assessment Item | Expected Result | Maximum Points |
|---|---|---:|
| Detect Brute Force | Correctly identify suspicious SSH authentication activity | 10 |
| Identify Affected Account or Asset | Identify relevant account or server information | Supporting criterion, not an additional scoring category |
| Incident Report | Submit a report containing required information | 10 |

The task weights must remain consistent with the V1 scoring configuration.

Identifying an affected account or asset may support the detection assessment criteria. It must not introduce an additional score unless the approved scoring configuration is updated.

## 8. Metrics

The scenario may provide:

- Detection Time.
- Response Time, when a verified response is part of the exercise.
- Incident Reporting Completeness.

All time-based metrics must use reliable event timestamps and verified action timestamps.

## 9. Expected Outcome

A successful participant should:

- Recognize suspicious SSH authentication activity.
- Correctly identify the relevant account or asset when information is available.
- Provide supporting authentication evidence.
- Submit an appropriate incident report.

The Assessment Engine evaluates the defensive tasks, and the Scoring Engine calculates the corresponding score.

## 10. Safety and Reset Requirements

- The SSH service must remain isolated within the Cyber Range.
- Authentication simulations must use designated test accounts.
- No real credentials or external accounts may be used.
- Authentication secrets must not be included in logs or reports.
- The environment should support reset after exercise completion.
- Evidence must be associated with the correct exercise.