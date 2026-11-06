# Cyber Range Scenarios

## 1. Purpose

Scenarios define controlled training situations used to evaluate participants' cybersecurity defense skills in the Cyber Range environment.

Each scenario describes:

- The training context.
- The simulated security situation.
- The affected assets and services.
- The expected Blue Team activities.
- The events and evidence collected by the system.
- The assessment criteria.
- The expected evaluation outcomes.

Scenarios provide a consistent foundation for exercise configuration, defensive task assignment, assessment, and scoring.

## 2. V1 Scenarios

V1 includes three predefined scenarios:

| Scenario | Main Training Focus |
|---|---|
| Port Scanning | Network activity monitoring and reconnaissance detection |
| SSH Brute Force | Authentication monitoring and suspicious login detection |
| Web Attack | Web log analysis and suspicious HTTP activity detection |

These scenarios focus on Blue Team monitoring, detection, investigation, response, and reporting.

## 3. Scenario Structure

Each scenario should define the following information.

### 3.1. Basic Information

- Scenario name.
- Scenario description.
- Scenario objectives.
- Scenario category.
- Scenario status.
- Required assets and services.

### 3.2. Environment Requirements

The scenario identifies the assets and services required for the exercise.

Examples:

- Linux Server.
- Web Server.
- SSH Service.
- Network monitoring component.
- Log collection component.

The exact environment depends on the scenario configuration.

### 3.3. Simulated Security Activity

A scenario defines the security activity that participants are expected to investigate.

For V1, activities should be generated in an isolated Cyber Range environment using controlled simulation or predefined test events.

The scenario must not depend on unauthorized activity against external systems.

### 3.4. Blue Team Tasks

Each scenario defines defensive tasks for participants.

Typical tasks include:

- Monitor relevant security events.
- Identify suspicious activity.
- Determine affected assets.
- Collect supporting evidence.
- Perform appropriate defensive responses.
- Verify the resulting system state.
- Submit an incident report.

### 3.5. Assessment Criteria

Assessment Criteria define the conditions required for successful task completion.

Examples:

- Correctly identify the suspicious activity.
- Identify the affected asset.
- Provide relevant evidence.
- Verify the result of a defensive action.
- Complete the required incident report.

## 4. Scenario Lifecycle

A scenario follows a general lifecycle:

1. Scenario definition.
2. Environment configuration.
3. Exercise creation.
4. Participant assignment.
5. Scenario execution.
6. Event and evidence collection.
7. Blue Team investigation and response.
8. Assessment.
9. Scoring.
10. Reporting.

Scenario configuration should be completed before the corresponding exercise starts.

## 5. Integration with Other Modules

### Exercise Management

Uses scenario definitions to configure exercises.

### Asset Management

Provides information about the assets required by each scenario.

### Event Collection

Collects relevant system, network, authentication, and application events.

### Assessment Engine

Evaluates defensive tasks according to predefined assessment criteria.

### Scoring Engine

Calculates participant scores based on assessment results.

### Metrics

Measures detection, response, isolation, and recovery performance when the required timestamps are available.

### Reporting

Provides scenario outcomes, assessment results, scores, and performance metrics.

## 6. V1 Scope

V1 supports three predefined scenarios:

- Port Scanning.
- SSH Brute Force.
- Web Attack.

The initial implementation focuses on:

- Controlled scenario execution.
- Predefined environment requirements.
- Event collection.
- Defensive task evaluation.
- Evidence-based assessment.
- Score calculation.
- Exercise reporting.

Dynamic scenario generation, advanced attack orchestration, and complex scenario customization are outside the initial V1 scope.

## 7. Safety and Isolation Requirements

All scenario activities must remain within the authorized Cyber Range environment.

The system should:

- Use isolated training assets.
- Prevent simulated activities from affecting external systems.
- Use controlled test events where possible.
- Preserve logs for assessment and auditing.
- Support environment reset after an exercise.
- Ensure participants operate only within their assigned exercise permissions.