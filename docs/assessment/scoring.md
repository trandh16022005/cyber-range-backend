# Scoring Engine

## 1. Purpose

The Scoring Engine calculates participant performance scores based on the results produced by the Assessment Engine.

Its responsibilities include:

- Assigning predefined weights to defensive tasks.
- Calculating earned points.
- Calculating the maximum applicable score.
- Excluding tasks marked as `NOT_APPLICABLE`.
- Producing a normalized final score.
- Providing score details for reports and performance evaluation.

The Scoring Engine is responsible for score calculation. It does not independently determine whether a defensive task has been completed.

## 2. Scoring Model

V1 uses a weighted scoring model.

Each defensive task has a predefined maximum point value. Participants receive points according to the corresponding Assessment Result.

The total possible score for an exercise is 100 points when all predefined tasks are applicable.

### 2.1. V1 Task Weights

| Defensive Task | Maximum Points |
|---|---:|
| Detect Port Scan | 10 |
| Detect Brute Force | 10 |
| Detect Web Attack | 15 |
| Identify Compromised Host | 15 |
| Isolate Attacked Host | 20 |
| Recover Service | 20 |
| Submit Incident Report | 10 |
| **Total** | **100** |

These weights represent the V1 scoring configuration.

## 3. Assessment Result Mapping

The Scoring Engine uses the Assessment Result status to determine how points are handled.

| Assessment Result | Earned Points | Applicable Maximum Points |
|---|---:|---:|
| `COMPLETED` | Full task points | Full task points |
| `FAILED` | 0 | Full task points |
| `PENDING` | Not finalized | Full task points |
| `IN_PROGRESS` | Not finalized | Full task points |
| `NOT_APPLICABLE` | Excluded | Excluded |

`PENDING` and `IN_PROGRESS` are not automatically converted into `FAILED`.

If an exercise is still running, its score should be treated as provisional. The system should distinguish an ongoing assessment from a finalized assessment.

## 4. Score Calculation

### 4.1. Earned Points

For a finalized assessment:

- `COMPLETED`: receives the task's full maximum points.
- `FAILED`: receives zero points.
- `NOT_APPLICABLE`: excluded from the calculation.

### 4.2. Applicable Maximum Points

The applicable maximum score is the sum of maximum points for all tasks except those marked `NOT_APPLICABLE`.

### 4.3. Final Score Formula

\[
\text{Final Score} =
\frac{\text{Total Earned Points}}
{\text{Applicable Maximum Points}}
\times 100
\]

The final score is normalized to a 100-point scale.

### 4.4. Example

Assume the following results:

| Task | Maximum Points | Result | Earned Points |
|---|---:|---|---:|
| Detect Port Scan | 10 | COMPLETED | 10 |
| Detect Brute Force | 10 | COMPLETED | 10 |
| Detect Web Attack | 15 | FAILED | 0 |
| Identify Compromised Host | 15 | COMPLETED | 15 |
| Isolate Attacked Host | 20 | NOT_APPLICABLE | Excluded |
| Recover Service | 20 | COMPLETED | 20 |
| Submit Incident Report | 10 | FAILED | 0 |

Applicable Maximum Points:

\[
10 + 10 + 15 + 15 + 20 + 10 = 80
\]

Total Earned Points:

\[
10 + 10 + 0 + 15 + 20 + 0 = 55
\]

Final Score:

\[
\frac{55}{80} \times 100 = 68.75
\]

The final score is **68.75/100**.

## 5. Scoring Rules

V1 follows these rules:

1. Each applicable defensive task has a predefined maximum point value.
2. A `COMPLETED` task receives its full maximum points.
3. A `FAILED` task receives zero points.
4. `PENDING` and `IN_PROGRESS` results are not automatically treated as failed.
5. `NOT_APPLICABLE` tasks are excluded from both earned points and applicable maximum points.
6. The final score is calculated only when the assessment has sufficient finalized results.
7. The system must distinguish provisional scores from finalized scores.
8. The Scoring Engine must use assessment results produced by the Assessment Engine, not client-submitted scores.

## 6. Edge Cases

### 6.1. No Applicable Tasks

If all tasks are marked `NOT_APPLICABLE`, the applicable maximum score is zero.

The system must not divide by zero. The score should be treated as unavailable rather than automatically assigning a score of zero.

### 6.2. Assessment Still in Progress

If one or more tasks remain `PENDING` or `IN_PROGRESS`, the assessment is not fully finalized.

The system may present progress information, but must not silently treat unfinished tasks as failed.

### 6.3. Missing Assessment Results

If an expected result is missing, the system should identify the incomplete assessment data instead of silently assigning points.

## 7. Integration

### Assessment Engine

Provides task evaluation results.

### Exercise Management

Provides the exercise and participant context.

### Reporting

Displays the final score, earned points, applicable maximum points, and task-level breakdown.

## 8. V1 Scope

The V1 Scoring Engine supports:

- Fixed task weights.
- Status-based point calculation.
- Exclusion of `NOT_APPLICABLE` tasks.
- Normalized final scores.
- Provisional and finalized score distinction.
- Task-level score breakdown.

Advanced scoring features such as dynamic weights, penalty systems, difficulty multipliers, and ranking algorithms are outside the initial V1 scope.