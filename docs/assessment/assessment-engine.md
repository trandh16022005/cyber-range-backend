# Assessment Engine

## 1. Purpose

The Assessment Engine evaluates participants' performance during a Cyber Range exercise.

Its primary responsibilities are:

- Evaluate whether defensive tasks have been completed.
- Compare collected evidence with assessment criteria.
- Track the status of each assessment result.
- Record evaluation outcomes.
- Provide assessment data to the Scoring Engine.
- Maintain traceability between tasks, events, evidence, and assessment results.

The Assessment Engine determines **whether a task has been successfully completed**. It does not calculate the final exercise score. Score calculation belongs to the Scoring Engine.

## 2. Core Concepts

### 2.1. Assessment

An Assessment represents the evaluation of a participant's performance in an exercise.

It contains the overall evaluation context and is associated with the relevant exercise and participant.

### 2.2. Assessment Criteria

Assessment Criteria define the conditions that must be satisfied for a defensive task to be considered successful.

Examples:

- Detect a port scanning activity.
- Identify a brute-force attempt.
- Detect a web attack.
- Identify a compromised host.
- Isolate an attacked host.
- Restore an affected service.
- Submit an incident report.

Each criterion should have a clear expected outcome and a method of verification.

### 2.3. Assessment Result

An Assessment Result records the evaluation status of a task.

V1 supports the following statuses:

| Status | Description |
|---|---|
| `PENDING` | The task has not started or has not been evaluated yet. |
| `IN_PROGRESS` | The participant is currently working on the task. |
| `COMPLETED` | The task has satisfied its assessment criteria. |
| `FAILED` | The task was evaluated and did not satisfy its assessment criteria. |
| `NOT_APPLICABLE` | The task does not apply to the current exercise or assessment. |

`NOT_COMPLETED` is not a valid status in V1.

## 3. Assessment Workflow

The general assessment workflow is:

1. An exercise is created and configured with defensive tasks.
2. Assessment criteria are associated with the relevant tasks.
3. An assessment is created for the exercise and participant.
4. Assessment results are initialized with the appropriate statuses.
5. The participant performs defensive tasks.
6. The system collects events, evidence, and relevant exercise data.
7. The Assessment Engine evaluates the available information.
8. The corresponding Assessment Results are updated.
9. The Scoring Engine calculates the score using the evaluation results.
10. The assessment outcome is made available for reporting.

## 4. Evaluation Process

For each defensive task, the Assessment Engine should:

1. Identify the task and its associated assessment criteria.
2. Retrieve relevant events and evidence.
3. Verify whether the required conditions have been satisfied.
4. Determine the assessment result.
5. Record the evaluation outcome and supporting evidence.
6. Make the result available to the Scoring Engine.

An evaluation should be traceable to the evidence used to support its outcome.

The system should not mark a task as `COMPLETED` merely because a participant submitted an action request. The expected result must be verified.

For example, requesting host isolation does not automatically prove that the host was successfully isolated. The system should verify the resulting asset or network state.

## 5. Evaluation Rules

The following rules apply to V1:

- A task can be marked `COMPLETED` only when its assessment criteria are satisfied.
- A task can be marked `FAILED` when it has been evaluated and its criteria are not satisfied.
- A task that has not been evaluated should remain `PENDING`.
- A task currently being performed should be marked `IN_PROGRESS` when appropriate.
- A task that does not apply to the current exercise should be marked `NOT_APPLICABLE`.
- `PENDING` and `IN_PROGRESS` must not automatically be treated as `FAILED`.
- `NOT_APPLICABLE` results must be excluded from the maximum applicable score calculation.

## 6. Integration with Other Modules

### Event Collection

Provides security events and system activity data used during evaluation.

### Evidence Management

Stores evidence supporting assessment outcomes.

### Exercise Management

Provides exercise configuration, participants, defensive tasks, and execution context.

### Scoring Engine

Receives assessment results and calculates the final score.

### Reporting

Uses assessment outcomes and scores to generate participant and exercise reports.

## 7. Data Management

The existing database schema is the source of truth for persistence.

Relevant tables include:

- `assessments`
- `assessment_results`
- `assessment_criteria`
- `defensive_tasks`
- `events`
- `evidence`
- `exercises`
- `exercise_participants`

The exact relationships and column mappings must follow the existing SQL schema.

Do not introduce new database columns or tables solely based on this conceptual document without first reviewing the current schema.

## 8. V1 Scope

The V1 Assessment Engine focuses on:

- Evaluating predefined defensive tasks.
- Using predefined assessment criteria.
- Recording assessment result statuses.
- Using collected events and evidence for verification.
- Providing assessment results to the Scoring Engine.
- Supporting traceable evaluation outcomes.

Advanced capabilities such as machine-learning-based evaluation, customizable evaluation scripts, and complex behavioral analysis are outside the initial V1 scope.