# CLAUDE.md

# 1. Project Context

This repository contains the backend of a **Cyber Range and Automated Defensive Skills Assessment System**.

The system is designed for cybersecurity training and defensive-skill assessment in a controlled Cyber Range environment.

The backend is responsible for:

- User and system-role management
- Team management
- Scenario management
- Cyber Range and asset management
- Exercise management
- Event and evidence management
- Automated assessment
- Automated scoring
- Incident report management
- Audit logging
- REST API for the frontend

The system supports Red Team, Blue Team, and White Team roles within an exercise.

## Exercise Roles

- `RED` — performs attack activities defined by the exercise.
- `BLUE` — performs defensive activities.
- `WHITE` — administers, monitors, controls, and reviews the exercise.

White Team is an **exercise role**, not a system-level RBAC role.

## System Roles

- `ADMIN`
- `INSTRUCTOR`
- `STUDENT`

System roles and exercise roles are separate concepts.

---

# 2. Documentation and Source of Truth

Before modifying code, read `CLAUDE.md` and the relevant documentation in `docs/`.

The documentation is organized into the following groups.

## 2.1 Core Project Documentation

These documents define the overall system:

| Document | Purpose |
|---|---|
| [01-overview.md](docs/01-overview.md) | Project goals, scope, and core concepts |
| [02-architecture.md](docs/02-architecture.md) | System architecture and component responsibilities |
| [03-database.md](docs/03-database.md) | Database design and entity relationships |
| [04-security.md](docs/04-security.md) | Authentication, authorization, and security rules |
| [05-api.md](docs/05-api.md) | REST API contract |
| [06-assessment-and-scoring.md](docs/06-assessment-and-scoring.md) | Overall assessment and scoring specification |
| [07-project-structure-coding-convention.md](docs/07-project-structure-coding-convention.md) | Package structure and coding conventions |

## 2.2 Blue Team Documentation

Read these documents when implementing or modifying defensive workflows:

| Document | Purpose |
|---|---|
| [Blue Team Overview](docs/blue-team/overview.md) | Blue Team responsibilities and defensive workflow |
| [Detection](docs/blue-team/detection.md) | Detection concepts and defensive event analysis |
| [Incident Response](docs/blue-team/incident-response.md) | Incident response and containment workflow |
| [Blue Team Assessment](docs/blue-team/assessment.md) | Blue Team assessment and evaluation |

## 2.3 Assessment Documentation

Read these documents when working on assessment, scoring, or performance metrics:

| Document | Purpose |
|---|---|
| [Assessment Engine](docs/assessment/assessment-engine.md) | Assessment Engine responsibilities and evaluation logic |
| [Scoring](docs/assessment/scoring.md) | Scoring rules and score calculation |
| [Metrics](docs/assessment/metrics.md) | Detection time, response time, and other assessment metrics |

## 2.4 Scenario Documentation

Read these documents when implementing or modifying scenario-related functionality:

| Document | Purpose |
|---|---|
| [Scenario Overview](docs/scenarios/overview.md) | Scenario concepts and structure |
| [Port Scanning](docs/scenarios/port-scanning.md) | Port scanning scenario |
| [SSH Brute Force](docs/scenarios/ssh-brute-force.md) | SSH brute-force scenario |
| [Web Attack](docs/scenarios/web-attack.md) | Web attack scenario |

## 2.5 Documentation Reading Rules

Select documents according to the task:

- User or authentication tasks: read the relevant core, security, database, and API documents.
- Blue Team tasks: read the relevant Blue Team documents and related assessment/scoring documents.
- Assessment tasks: read the Assessment Engine, Scoring, Metrics, and overall assessment specification.
- Scenario tasks: read Scenario Overview and the specific scenario document.
- Database tasks: read the database design before changing entities or relationships.
- API tasks: read the API contract before adding or modifying endpoints.

Do not read every document unnecessarily. Read the documents that directly affect the requested task and its dependencies.

## Documentation Priority

When implementing a feature:

1. Follow the existing project documentation.
2. Follow the architecture and database design.
3. Follow the API contract.
4. Follow the security rules.
5. Follow the relevant Blue Team, scenario, assessment, and scoring specifications.
6. Follow the coding conventions.

Do **not** invent a new architecture, database entity, API contract, security model, or assessment model when an existing design already defines it.

If requirements conflict with the documentation, stop and ask for clarification instead of silently changing the architecture.

---

# 3. Technology Stack

The backend uses:

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- Bean Validation
- PostgreSQL
- Maven
- JUnit

Use the existing dependencies and project configuration in `pom.xml`.

Do not introduce a new framework, library, database, or architectural pattern unless explicitly requested.

---

# 4. Architecture Rules

The project follows a **package-by-feature** structure.

Main modules include:

```text
com.cyberrange.backend/
├── auth/
├── user/
├── team/
├── scenario/
├── lab/
├── event/
├── assessment/
├── scoring/
├── report/
├── common/
└── config/
```

A feature may contain:

```text
feature/
├── controller/
├── dto/
├── entity/
├── repository/
├── service/
└── mapper/
```

## Layer Responsibilities

### Controller

Responsible for:

- HTTP endpoints
- Request parameters
- Request/response DTOs
- HTTP status codes

Controllers should not contain complex business logic.

### Service

Responsible for:

- Business logic
- Authorization decisions that depend on business context
- Transaction boundaries
- Coordination between repositories and other services

### Repository

Responsible for:

- Database access
- Query methods
- Persistence operations

Do not put business logic in repositories.

### Entity

Responsible for:

- Database persistence mapping
- Entity relationships
- Persistent state

Do not expose JPA entities directly as public API contracts when DTOs are appropriate.

### DTO

Use DTOs for API requests and responses.

Do not allow API clients to directly control internal fields such as:

- Assessment status
- Awarded score
- Total score
- Audit information
- System-controlled timestamps

### Mapper

Responsible for converting between:

```text
Entity ↔ DTO
```

Do not put business logic in mappers.

---

# 5. Database Rules

The current V1 database design contains these main entities:

```text
users
roles
teams
team_members

scenarios
defensive_tasks
assessment_criteria

cyber_ranges
assets
services

exercises
exercise_participants

events
evidence

assessments
assessment_results
assessment_scores

incident_reports
audit_logs
```

Do not create separate tables for:

```text
red_teams
blue_teams
white_teams
attack_scenarios
scores
assessment_reviews
linux_servers
windows_servers
web_servers
database_servers
ssh_servers
active_directory
```

unless the architecture documentation is explicitly changed.

## Important Relationships

- Users belong to teams through `team_members`.
- Scenarios define reusable exercise templates.
- Exercises are actual executions of scenarios.
- Exercise participants connect users to exercises and define exercise roles.
- Events represent observed/raw events.
- Evidence represents assessment-relevant evidence.
- Assessment results represent defensive task assessment outcomes.
- Assessment scores represent awarded points.
- Incident reports represent Blue Team reporting.
- Audit logs record security-sensitive system actions.

The existing database design is the source of truth for entity fields, relationships, constraints, and table mappings.

Do not assume a field or relationship exists without checking `docs/03-database.md` and the existing SQL schema.

Do not change the database schema merely to simplify an implementation.

---

# 6. Security Rules

Security rules defined in `docs/04-security.md` must be enforced server-side.

## Authentication

Use JWT-based authentication for the V1 API.

Passwords must:

- Never be stored in plaintext.
- Never be logged.
- Never be returned through API responses.

## Authorization

System RBAC:

```text
ADMIN
INSTRUCTOR
STUDENT
```

Exercise roles:

```text
RED
BLUE
WHITE
```

Do not treat an exercise role as a system role.

For exercise-specific authorization, verify the user's participation in the specific exercise.

Do not rely only on a role value supplied by the client.

## Exercise Isolation

A user must only access exercises and resources they are authorized to access.

Do not expose another exercise's:

- Events
- Evidence
- Assessment results
- Scores
- Incident reports
- Participants

## Server-Controlled Assessment

Clients must not be allowed to directly submit:

- Final score
- Awarded score
- Assessment result
- Task completion status
- Detection time
- Response time

These values must be calculated or validated by the backend.

## Audit

Sensitive actions should be auditable.

Examples include:

- Login/security events
- Exercise state changes
- Participant changes
- Assessment operations
- Score-related operations
- Administrative operations

Never put passwords, JWTs, secrets, or other sensitive credentials into logs.

---

# 7. Assessment and Scoring Rules

The Assessment Engine and Scoring Engine are separate components.

The pipeline is:

```text
Cyber Range
    ↓
Events
    ↓
Evidence
    ↓
Assessment Engine
    ↓
Assessment Result
    ↓
Scoring Engine
    ↓
Assessment Score
    ↓
Report
```

Read the following documents before changing this functionality:

- `docs/06-assessment-and-scoring.md`
- `docs/assessment/assessment-engine.md`
- `docs/assessment/scoring.md`
- `docs/assessment/metrics.md`
- `docs/blue-team/assessment.md`

## Important Distinction

Do not confuse:

```text
Raw Event
Evidence
Assessment Result
Score
```

They are different concepts.

## Assessment Engine

The Assessment Engine determines whether a defensive task has been completed based on observable evidence and configured assessment criteria.

It must not depend on a value submitted by the client.

## Scoring Engine

The Scoring Engine calculates points after assessment results are determined.

The client must never be trusted to calculate or submit the final score.

## Assessment Result Statuses

The only valid V1 Assessment Result statuses are:

```text
PENDING
IN_PROGRESS
COMPLETED
FAILED
NOT_APPLICABLE
```

Do not introduce `NOT_COMPLETED` or other status values without an approved documentation change.

`PENDING` and `IN_PROGRESS` must not automatically be treated as `FAILED`.

`NOT_APPLICABLE` means the task does not apply to the current exercise assessment. It must be deliberately determined and justified, not selected freely by a participant.

Where supported by the database design, record the reason and audit relevant changes to applicability.

## Exercise Statuses

Exercise status is separate from Assessment Result status.

The valid V1 Exercise statuses are:

```text
CREATED
READY
RUNNING
PAUSED
STOPPED
COMPLETED
CANCELLED
```

`RESET` is an action, not an Exercise status.

After a successful reset, the Exercise returns to `READY`. If environment initialization is unsuccessful, it returns to `CREATED`.

Do not mix Exercise lifecycle states with defensive task assessment states.

## V1 Task Scoring

The current V1 task weights are:

| Defensive Task | Maximum Points |
|---|---:|
| Detect port scan | 10 |
| Detect brute force | 10 |
| Detect web attack | 15 |
| Identify compromised host | 15 |
| Isolate attacked host | 20 |
| Recover service | 20 |
| Incident report | 10 |
| **Total** | **100** |

These weights are configured task maximums. Do not award the same task weight repeatedly through multiple criteria.

Assessment criteria determine whether the task has been completed. The Scoring Engine awards the configured task points according to the V1 scoring rules.

## NOT_APPLICABLE Scoring Rule

Tasks with status `NOT_APPLICABLE` must be excluded from both:

- Total earned points.
- Applicable maximum points.

The final score is normalized against the maximum points of applicable tasks:

```text
Applicable Maximum Points =
    Sum of maximum points for tasks not marked NOT_APPLICABLE

Final Score =
    (Total Earned Points / Applicable Maximum Points) × 100
```

Example:

```text
All task weights                         = 100
One task marked NOT_APPLICABLE           = 10
Applicable maximum points                = 90
Total earned points                      = 75

Final Score = (75 / 90) × 100 = 83.33
```

If the applicable maximum points equal zero, the final score must be unavailable/undefined. Do not divide by zero or represent the result as `NaN` or an artificial score of zero.

## Task Scoring and Finalization

For V1 binary task scoring:

- `COMPLETED`: earns the configured task maximum points.
- `FAILED`: earns zero points.
- `NOT_APPLICABLE`: excluded from the numerator and denominator.
- `PENDING`: not finalized.
- `IN_PROGRESS`: not finalized.

Do not automatically convert unresolved tasks into `FAILED`.

A final score must not be marked as finalized while applicable tasks remain `PENDING` or `IN_PROGRESS`. A provisional score may be displayed during an exercise, but it must be clearly identified as provisional.

Awarded points must never exceed the configured maximum points.

Do not duplicate a task's maximum points across multiple assessment criteria.

## Time Metrics

Detection time:

```text
Detection Time = T_detect - T_attack
```

Response time:

```text
Response Time = T_response - T_detect
```

Store timestamps and calculated durations according to the database design.

Use `docs/assessment/metrics.md` as the detailed reference for metric definitions and calculations.

---

# 8. Blue Team Rules

Blue Team functionality must follow the specifications in:

```text
docs/blue-team/overview.md
docs/blue-team/detection.md
docs/blue-team/incident-response.md
docs/blue-team/assessment.md
```

Important principles:

- Blue Team performs defensive activities within an assigned exercise.
- Detection must be based on observable events and evidence.
- Incident response must follow the documented defensive workflow.
- Assessment must evaluate defensive outcomes using configured criteria.
- Assessment results must remain traceable to their supporting evidence.
- White Team reviews and monitors exercise results.

Do not introduce unrelated offensive functionality into Blue Team modules.

Do not treat a participant's unverified claim as sufficient evidence of task completion.

---

# 9. Scenario Rules

Scenario functionality must follow:

```text
docs/scenarios/overview.md
docs/scenarios/port-scanning.md
docs/scenarios/ssh-brute-force.md
docs/scenarios/web-attack.md
```

V1 scenarios include:

- Port Scanning
- SSH Brute Force
- Web Attack

Important principles:

- A Scenario is a reusable exercise template.
- An Exercise is an execution of a Scenario.
- Scenario tasks and assessment criteria must follow the documented design.
- Scenario-specific assessment rules must not conflict with the overall assessment specification.
- Do not hard-code scenario IDs or task IDs as the only way to identify scenario behavior.

When adding or changing a scenario, check its relationship with defensive tasks, assessment criteria, events, and evidence.

---

# 10. API Rules

Follow the API contract defined in:

```text
docs/05-api.md
```

Use the `/api/v1` API prefix.

Use REST-style resource endpoints.

Examples of resource groups:

```text
/api/v1/auth
/api/v1/users
/api/v1/teams
/api/v1/scenarios
/api/v1/cyber-ranges
/api/v1/exercises
/api/v1/events
/api/v1/assessment
/api/v1/incident-reports
/api/v1/audit-logs
```

Use action endpoints only when an operation represents a state transition.

Examples:

```text
POST /api/v1/exercises/{id}/start
POST /api/v1/exercises/{id}/pause
POST /api/v1/exercises/{id}/resume
POST /api/v1/exercises/{id}/stop
POST /api/v1/exercises/{id}/reset
```

Do not create unnecessary endpoints.

Before adding a new endpoint, check whether the existing API contract already covers the requirement.

---

# 11. Validation and Error Handling

Use Bean Validation for API input validation.

Examples:

java
@NotBlank
@Email
@Size
@NotNull
```

Use centralized exception handling with:

```java
@RestControllerAdvice
```

API errors should have a consistent structure.

Use the documented HTTP status conventions, including:

- `400 Bad Request` for validation errors.
- `409 Conflict` for invalid state transitions or state conflicts.

Do not expose the following to API clients:

- Stack traces
- Internal exception details
- Database credentials
- Sensitive configuration
- Security-sensitive implementation details

---

# 12. Transaction Rules

Use transactions where multiple database operations must succeed or fail together.

Examples:

- Creating an exercise and its participants.
- Changing exercise state with related records.
- Creating assessment results and scores.
- Updating related assessment information.

Avoid unnecessarily large transactions.

Keep transaction boundaries primarily in the service layer.

---

# 13. Logging Rules

Use application logs for debugging and operational information.

Use `audit_logs` for security-sensitive actions.

Do not log:

```text
passwords
JWT tokens
secrets
API keys
database credentials
```

Logs should contain enough context to diagnose problems without exposing sensitive information.

---

# 14. Testing Rules

Every new business feature should have appropriate tests.

Prioritize tests for:

1. Service business logic.
2. Security and authorization.
3. Assessment Engine.
4. Scoring Engine.
5. API validation.
6. Repository behavior when necessary.

Assessment and scoring logic should have strong automated test coverage because incorrect scoring directly affects exercise results.

Important scoring test cases include:

- `COMPLETED` tasks receive their configured points.
- `FAILED` tasks receive zero points.
- `NOT_APPLICABLE` tasks are excluded from the applicable maximum.
- `PENDING` and `IN_PROGRESS` do not automatically become failures.
- Final scores are normalized against applicable maximum points.
- Zero applicable maximum points do not cause division by zero.
- Awarded points cannot exceed task maximum points.
- A task's points are not awarded more than once.

Before considering a task complete, run the relevant tests.

At minimum:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

If formatting or static analysis tools are configured, use them as appropriate.

---

# 15. Code Quality Rules

Follow the conventions in:

```text
docs/07-project-structure-coding-convention.md
```

Use:

- Clear names.
- Small focused methods.
- Meaningful DTOs.
- Clear service boundaries.
- Constructor injection.
- Consistent exception handling.
- Consistent API responses.

Avoid:

- Unnecessary abstractions.
- Duplicated business logic.
- Giant service classes.
- Giant controllers.
- Magic numbers.
- Hard-coded business rules where configuration is appropriate.
- Premature microservices.
- Unnecessary design patterns.

Prefer the simplest implementation that satisfies the documented architecture.

---

# 16. Scope Control

Work only on the requested task.

Do not modify unrelated modules unless necessary.

For example, if implementing the Team module, do not redesign:

- Assessment
- Scoring
- Authentication
- Database architecture
- API architecture

without a clear requirement.

If a task requires a change outside the current module:

1. Identify the dependency.
2. Check the relevant documentation.
3. Make the smallest necessary change.
4. Explain the cross-module change.

Do not perform large refactors while implementing an unrelated feature.

---

# 17. Documentation Consistency

When implementation changes a documented contract, check whether the documentation must also be updated.

Relevant documents include:

```text
docs/01-overview.md
docs/02-architecture.md
docs/03-database.md
docs/04-security.md
docs/05-api.md
docs/06-assessment-and-scoring.md
docs/07-project-structure-coding-convention.md

docs/blue-team/overview.md
docs/blue-team/detection.md
docs/blue-team/incident-response.md
docs/blue-team/assessment.md

docs/assessment/assessment-engine.md
docs/assessment/scoring.md
docs/assessment/metrics.md

docs/scenarios/overview.md
docs/scenarios/port-scanning.md
docs/scenarios/ssh-brute-force.md
docs/scenarios/web-attack.md
```

Do not silently change the documented design.

If the implementation requires a design change:

1. Identify the affected documents.
2. Check for conflicts with related specifications.
3. Update the relevant documentation consistently.
4. Explain the design change in the final report.

Do not update unrelated documents unnecessarily.

---

# 18. Git and Team Collaboration

This is a two-person development project.

Each developer may use Claude Code, so changes must remain within the assigned scope.

Before making changes:

```bash
git status
git branch
```

Review the current working tree before modifying files.

Do not overwrite another developer's unfinished work.

Do not modify or delete unrelated changes.

## Commit Principles

Use clear commits describing one logical change.

Examples:

```text
feat: implement user management
feat: add exercise participant API
fix: validate exercise access
test: add assessment scoring tests
docs: update API contract
refactor: simplify team service
```

Do not commit:

```text
.env
passwords
JWT secrets
API keys
database credentials
private certificates
temporary files
IDE-specific generated files
```

Before committing:

```bash
git status
git diff
```

Review the changes and make sure only intended files are included.

Prefer staging specific files instead of blindly staging the entire working tree.

---

# 19. Claude Code Workflow

When starting a task:

## Step 1 — Understand

Read:

```text
CLAUDE.md
```

Then read only the relevant documents from `docs/`, following the Documentation Reading Rules in Section 2.

For example:

- Assessment implementation: read the assessment specification, Assessment Engine, Scoring, and Metrics documents.
- Blue Team detection: read Blue Team Overview and Detection.
- Incident response: read Blue Team Overview and Incident Response.
- Scenario implementation: read Scenario Overview and the relevant scenario specification.

## Step 2 — Inspect

Inspect the existing code before creating new files.

Do not assume that a class, endpoint, entity, or service does not already exist.

## Step 3 — Plan

Identify:

- Files that need to change.
- Existing code that can be reused.
- Dependencies with other modules.
- Tests that should be added.
- Documentation that may be affected.

## Step 4 — Implement

Implement the smallest complete solution consistent with the project documentation.

## Step 5 — Test

Run relevant tests.

For backend changes, normally run:

```powershell
.\mvnw.cmd test
```

## Step 6 — Review

Review:

```bash
git diff
git status
```

Check for:

- Unintended changes.
- Security issues.
- Missing validation.
- Broken API contracts.
- Duplicated logic.
- Missing tests.
- Inconsistencies with documentation.

## Step 7 — Report

Clearly report:

- What was changed.
- Which files were changed.
- Tests executed.
- Any remaining issues.
- Any documentation that may need updating.

---

# 20. Rules for Ambiguous Requirements

When a requirement is ambiguous, do not invent important architectural behavior.

Ask for clarification when ambiguity affects:

- Database schema.
- API contract.
- Authentication.
- Authorization.
- Assessment logic.
- Scoring rules.
- Exercise roles.
- System roles.
- Architecture.
- Data ownership.

For minor implementation details, choose the simplest solution consistent with the existing documentation.

If two documents conflict, identify the conflict and ask for clarification rather than silently choosing a new design.

---

# 21. Important Project Constraints

Always preserve these principles:

1. System roles and exercise roles are separate.
2. White Team is an exercise role, not a system RBAC role.
3. Do not create separate Red/Blue/White team tables.
4. Scenario and Exercise are different concepts.
5. Events and Evidence are different concepts.
6. Assessment determines task completion.
7. Scoring determines awarded points.
8. Clients cannot determine their own assessment result or score.
9. Authorization must be enforced server-side.
10. Exercise data must be isolated between users/exercises.
11. Assessment and scoring must remain traceable to evidence.
12. `NOT_APPLICABLE` tasks are excluded from the applicable maximum score.
13. `PENDING` and `IN_PROGRESS` must not automatically be treated as `FAILED`.
14. Exercise statuses and Assessment Result statuses are separate.
15. V1 should remain simple and extensible.
16. Do not introduce unnecessary technologies or architectural complexity.
17. Do not silently change the project's documented architecture.

---

# 22. Definition of Done

A task is considered complete when:

- The implementation follows the documented architecture.
- Security rules are respected.
- API behavior matches the API contract.
- Database changes match the database design.
- Relevant validation is implemented.
- Relevant tests are added or updated.
- Tests pass.
- No secrets are introduced.
- No unrelated files are modified unnecessarily.
- Documentation is updated when a documented contract changes.
- The final diff has been reviewed.

The goal is not simply to make the code work.

The goal is to make the code **consistent with the Cyber Range architecture, secure, testable, maintainable, and compatible with the work of the other developer.**