# Port Scanning Scenario

## 1. Scenario Overview

The Port Scanning scenario trains participants to identify and investigate suspicious network reconnaissance activity.

Participants act as Blue Team defenders responsible for monitoring network events, recognizing scanning behavior, identifying potentially affected assets, and documenting their findings.

All activities take place within the isolated Cyber Range environment.

## 2. Scenario Objectives

By completing this scenario, participants should be able to:

- Recognize suspicious network connection patterns.
- Identify potential port scanning activity.
- Determine the affected target asset.
- Analyze relevant network evidence.
- Record the detection outcome.
- Submit an incident report.

## 3. Environment Requirements

The scenario may use the following components:

| Component | Purpose |
|---|---|
| Linux Server | Training target asset |
| Network Monitoring | Provides network activity information |
| Log Collection | Collects relevant network events |
| Cyber Range Backend | Manages the exercise and assessment |
| Blue Team Interface | Allows participants to review and report findings |

The actual environment configuration must be defined when creating the exercise.

## 4. Simulated Security Activity

The scenario introduces controlled network reconnaissance activity within the training environment.

The simulation generates network telemetry representing suspicious connection patterns, such as:

- Multiple connection attempts involving a target asset.
- Attempts involving different service ports.
- Unusual connection frequency.
- Repeated connection patterns within a limited period.

These events are intended to represent port scanning behavior for defensive analysis.

The scenario should use controlled simulation or predefined network events rather than scanning external systems.

## 5. Blue Team Tasks

### Task 1: Monitor Network Activity

Participants review the available network events and identify unusual connection patterns.

Expected outcome:

- Relevant network activity is identified.
- Supporting event information is available.

### Task 2: Detect Port Scanning

Participants determine whether the observed activity represents suspicious port scanning behavior.

Expected outcome:

- The activity is correctly classified.
- The detection is supported by relevant evidence.

### Task 3: Identify the Target Asset

Participants determine which asset is associated with the suspicious activity.

Expected outcome:

- The affected or targeted asset is correctly identified.
- The identification is supported by event information.

### Task 4: Submit an Incident Report

Participants document the observed activity and their investigation results.

Expected outcome:

- The report describes the suspicious activity.
- Relevant assets and evidence are included.
- The detection outcome is documented.

## 6. Event and Evidence Requirements

The system should collect relevant information, including:

- Event timestamp.
- Source address, when available.
- Destination address.
- Destination port, when available.
- Network protocol.
- Connection result.
- Target asset reference.
- Event classification.
- Related exercise identifier.

Sensitive or unnecessary information should not be collected.

Evidence should allow the assessment process to verify the detection and target identification outcomes.

## 7. Assessment Criteria

The following criteria apply to the scenario:

| Assessment Item | Expected Result | Maximum Points |
|---|---|---:|
| Detect Port Scan | Correctly identify suspicious scanning activity | 10 |
| Identify Target Asset | Identify the relevant target asset | Included in the corresponding task configuration |
| Incident Report | Submit a report containing required information | 10 |

The task weights must remain consistent with the V1 scoring configuration.

The Identify Target Asset activity should be mapped to the existing defensive task and assessment criteria rather than creating an additional scoring category without approval.

## 8. Metrics

The scenario may provide the following metrics:

- Detection Time.
- Incident Reporting Completeness.

Detection Time is calculated from the relevant event occurrence timestamp and the verified detection timestamp.

## 9. Expected Outcome

A successful participant should:

- Recognize the suspicious network activity.
- Correctly identify the target asset.
- Provide supporting evidence.
- Submit an appropriate incident report.

The Assessment Engine verifies the task results, while the Scoring Engine calculates the corresponding score.

## 10. Safety and Reset Requirements

- All simulated activity must remain inside the authorized Cyber Range.
- The scenario must not target external networks.
- Training events should be isolated from production systems.
- The environment should support reset after exercise completion.
- Collected evidence should remain associated with the correct exercise.