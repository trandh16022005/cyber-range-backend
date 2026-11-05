# Assessment Metrics

## 1. Purpose

Assessment Metrics define the measurements used to evaluate Blue Team performance during a Cyber Range exercise.

While the Assessment Engine determines whether defensive tasks satisfy their criteria and the Scoring Engine calculates points, the Metrics component focuses on measuring:

- How quickly security activities are detected.
- How quickly participants respond to incidents.
- How effectively affected assets are isolated.
- How quickly affected services are recovered.
- How completely incident information is documented.

These metrics provide additional insight into participant performance beyond the final score.

## 2. Core Metrics

V1 focuses on the following metrics.

### 2.1. Detection Time

Detection Time measures the elapsed time between the occurrence of a security event and its successful detection by the Blue Team.

**Formula:**

\[
\text{Detection Time} =
T_{\text{detection}} - T_{\text{event}}
\]

Where:

- `T_event`: Time when the relevant security event occurred.
- `T_detection`: Time when the event was successfully detected.

Examples:

- Time required to detect a port scanning activity.
- Time required to identify a brute-force attempt.
- Time required to detect a web attack.
- Time required to identify a compromised host.

A detection should be considered successful only when supported by the corresponding assessment criteria and evidence.

### 2.2. Response Time

Response Time measures the elapsed time between incident detection and the first verified defensive response.

**Formula:**

\[
\text{Response Time} =
T_{\text{response}} - T_{\text{detection}}
\]

Where:

- `T_detection`: Time when the incident was successfully detected.
- `T_response`: Time when the first relevant defensive response was verified.

Examples:

- Time between detecting an incident and initiating an appropriate response.
- Time between identifying an affected host and beginning the isolation process.

A submitted action request alone should not be considered a verified response.

### 2.3. Isolation Time

Isolation Time measures the elapsed time between the start of the isolation response and confirmation that the affected host has been successfully isolated.

**Formula:**

\[
\text{Isolation Time} =
T_{\text{isolated}} - T_{\text{isolation-start}}
\]

Where:

- `T_isolation-start`: Time when the isolation operation begins.
- `T_isolated`: Time when the isolation result is verified.

This metric helps evaluate the effectiveness of the host isolation process.

### 2.4. Recovery Time

Recovery Time measures the elapsed time between the beginning of service recovery and confirmation that the affected service has returned to the required operational state.

**Formula:**

\[
\text{Recovery Time} =
T_{\text{recovered}} - T_{\text{recovery-start}}
\]

Where:

- `T_recovery-start`: Time when the recovery operation begins.
- `T_recovered`: Time when service recovery is verified.

The system should verify the service state rather than relying only on a participant's confirmation.

### 2.5. Incident Reporting Completeness

Incident Reporting Completeness measures how completely a participant documents an incident report.

V1 evaluates whether the report contains the required information.

Suggested report elements:

- Incident description.
- Detection time.
- Affected assets.
- Observed evidence.
- Defensive actions taken.
- Isolation outcome, when applicable.
- Recovery outcome, when applicable.
- Final incident status.

The completeness result should be evaluated against predefined assessment criteria.

It should not be interpreted as a time-based metric.

## 3. Metric Data Sources

Metrics should be derived from verifiable system data.

| Metric | Required Data |
|---|---|
| Detection Time | Event occurrence timestamp and verified detection timestamp |
| Response Time | Verified detection timestamp and response timestamp |
| Isolation Time | Isolation start timestamp and verified isolation timestamp |
| Recovery Time | Recovery start timestamp and verified recovery timestamp |
| Reporting Completeness | Incident report and assessment criteria |

Potential data sources include:

- Security events.
- System logs.
- Asset status changes.
- Service status changes.
- Evidence records.
- Assessment results.
- Incident reports.
- Exercise activity records.

The exact data source and timestamp fields must be mapped to the existing database schema.

## 4. Timestamp Requirements

To calculate metrics consistently, the system should:

- Store timestamps using a consistent timezone strategy.
- Prefer UTC for persisted timestamps.
- Preserve the original event occurrence time when available.
- Distinguish event occurrence time from event collection time.
- Distinguish an action request from a verified action result.
- Avoid calculating metrics when required timestamps are missing or invalid.

For example, the time when a log is collected may differ from the time when the underlying event occurred.

The system should use the event occurrence timestamp for Detection Time whenever reliable data is available.

## 5. Metric Calculation Rules

V1 follows these rules:

1. A metric must use timestamps from identifiable system events or verified actions.
2. The start and end timestamps must belong to the same exercise and relevant incident context.
3. The calculated duration must not be negative.
4. Missing timestamps must not be replaced with invented values.
5. Metrics that cannot be calculated should be reported as unavailable.
6. The system should preserve the source information used for each calculation.
7. Metrics should provide supporting information for assessment and reporting.
8. Metrics must not independently change an Assessment Result or final score unless an explicit assessment criterion defines that behavior.

## 6. Metrics and Scoring

Metrics and scoring serve different purposes.

| Aspect | Metrics | Scoring |
|---|---|---|
| Purpose | Measure performance characteristics | Evaluate task results numerically |
| Examples | Detection Time, Response Time | Earned Points, Final Score |
| Data | Timestamps, events, verified actions | Assessment Results and task weights |
| Output | Durations and completeness measurements | Normalized score |
| Responsibility | Performance measurement | Score calculation |

For V1, the seven predefined defensive tasks determine the score.

Metrics provide additional context for understanding how participants achieved those results.

For example, two participants may both complete the task of detecting a brute-force attempt and receive the same task points, while having different Detection Times.

## 7. Reporting

Metrics should be available for exercise and participant reports.

Suggested report information:

- Detection Time for each evaluated incident.
- Response Time for each evaluated incident.
- Isolation Time when host isolation is required.
- Recovery Time when service recovery is required.
- Incident Reporting Completeness.
- Metric availability and calculation status.

Reports should distinguish between measured values and unavailable values.

## 8. V1 Scope

The V1 Metrics component focuses on:

- Detection Time.
- Response Time.
- Isolation Time.
- Recovery Time.
- Incident Reporting Completeness.
- Timestamp-based calculations.
- Integration with assessment results and reports.

Advanced capabilities such as statistical benchmarking, trend analysis across multiple exercises, automated performance rankings, and predictive analytics are outside the initial V1 scope.