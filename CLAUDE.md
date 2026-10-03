# CLAUDE.md

## 1. Project Context

This repository contains the backend of a **Cyber Range and Automated Defensive Skills Assessment System**.

The system is designed for cybersecurity training and defensive-skill assessment in a controlled Cyber Range environment.

The backend is responsible for:

* User and system-role management
* Team management
* Scenario management
* Cyber Range and asset management
* Exercise management
* Event and evidence management
* Automated assessment
* Automated scoring
* Incident report management
* Audit logging
* REST API for the frontend

The system supports Red Team, Blue Team, and White Team roles within an exercise.

### Exercise Roles

* `RED` — performs attack activities defined by the exercise
* `BLUE` — performs defensive activities
* `WHITE` — administers, monitors, controls, and reviews the exercise

White Team is an **exercise role**, not a system-level RBAC role.

### System Roles

* `ADMIN`
* `INSTRUCTOR`
* `STUDENT`

System roles and exercise roles are separate concepts.

---

# 2. Source of Truth

Before modifying code, read the relevant documentation in `docs/`.

The following documents are the project's primary source of truth:

```text
docs/
├── 01-overview.md
├── 02-architecture.md
├── 03-database.md
├── 04-security.md
├── 05-api.md
├── 06-assessment-and-scoring.md
└── 07-project-structure-coding-convention.md
```

### Priority

When implementing a feature:

1. Follow the existing project documentation.
2. Follow the architecture and database design.
3. Follow the API contract.
4. Follow the security rules.
5. Follow the assessment/scoring design.
6. Follow the coding conventions.

Do **not** invent a new architecture, database entity, API contract, security model, or assessment model when an existing design already defines it.

If requirements conflict with the documentation, stop and ask for clarification instead of silently changing the architecture.

---

# 3. Technology Stack

The backend uses:

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* JWT
* Bean Validation
* PostgreSQL
* Maven
* JUnit

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

### Layer responsibilities

#### Controller

Responsible for:

* HTTP endpoints
* Request parameters
* Request/response DTOs
* HTTP status codes

Controllers should not contain complex business logic.

#### Service

Responsible for:

* Business logic
* Authorization decisions that depend on business context
* Transaction boundaries
* Coordination between repositories and other services

#### Repository

Responsible for:

* Database access
* Query methods
* Persistence operations

Do not put business logic in repositories.

#### Entity

Responsible for:

* Database persistence mapping
* Entity relationships
* Persistent state

Do not expose JPA entities directly as public API contracts when DTOs are appropriate.

#### DTO

Use DTOs for API requests and responses.

Do not allow API clients to directly control internal fields such as:

* assessment status
* awarded score
* total score
* audit information
* system-controlled timestamps

#### Mapper

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

### Important relationships

* Users belong to teams through `team_members`.
* Scenarios define reusable exercise templates.
* Exercises are actual executions of scenarios.
* Exercise participants connect users to exercises and define exercise roles.
* Events represent observed/raw events.
* Evidence represents assessment-relevant evidence.
* Assessment results represent task completion.
* Assessment scores represent awarded points for criteria.
* Incident reports represent Blue Team reporting.
* Audit logs record security-sensitive system actions.

---

# 6. Security Rules

Security rules defined in `docs/04-security.md` must be enforced server-side.

### Authentication

Use JWT-based authentication for the V1 API.

Passwords must:

* never be stored in plaintext
* never be logged
* never be returned through API responses

### Authorization

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

### Exercise isolation

A user must only access exercises and resources they are authorized to access.

Do not expose another exercise's:

* events
* evidence
* assessment results
* scores
* incident reports
* participants

### Server-controlled assessment

Clients must not be allowed to directly submit:

* final score
* awarded score
* assessment result
* task completion status
* detection time
* response time

These values must be calculated or validated by the backend.

### Audit

Sensitive actions should be auditable.

Examples include:

* login/security events
* exercise state changes
* participant changes
* assessment operations
* score-related operations
* administrative operations

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

### Important distinction

Do not confuse:

```text
Raw Event
Evidence
Assessment Result
Score
```

They are different concepts.

### Assessment Engine

The Assessment Engine determines whether a defensive task has been completed based on observable evidence.

It must not depend on a value submitted by the client.

### Scoring Engine

The Scoring Engine calculates points after assessment results are determined.

The client must never be trusted to calculate or submit the final score.

### V1 task scoring

Current V1 scoring:

| Criterion                 |  Points |
| ------------------------- | ------: |
| Detect port scan          |      10 |
| Detect brute force        |      10 |
| Detect web attack         |      15 |
| Identify compromised host |      15 |
| Isolate attacked host     |      20 |
| Recover service           |      20 |
| Incident report           |      10 |
| **Total**                 | **100** |

Scoring rules should remain configurable and extensible.

Do not hard-code task IDs as the only way to determine scoring.

### Time metrics

Detection time:

```text
Detection Time = T_detect - T_attack
```

Response time:

```text
Response Time = T_response - T_detect
```

Store timestamps and calculated durations according to the database design.

---

# 8. API Rules

Follow the API contract defined in:

```text
docs/05-api.md
```

Use REST-style resource endpoints.

Examples:

```text
/api/auth
/api/users
/api/teams
/api/scenarios
/api/cyber-ranges
/api/exercises
/api/events
/api/assessment
/api/incident-reports
/api/audit-logs
```

Use action endpoints only when an operation represents a state transition.

Examples:

```text
POST /api/exercises/{id}/start
POST /api/exercises/{id}/pause
POST /api/exercises/{id}/resume
POST /api/exercises/{id}/stop
POST /api/exercises/{id}/reset
```

Do not create unnecessary endpoints.

Before adding a new endpoint, check whether the existing API contract already covers the requirement.

---

# 9. Validation and Error Handling

Use Bean Validation for API input validation.

Examples:

java
@NotBlank
@Email
@Size
@NotNull

Use centralized exception handling with:

```text
@RestControllerAdvice
```

API errors should have a consistent structure.

Do not expose:

* stack traces
* internal exception details
* database credentials
* sensitive configuration
* security-sensitive implementation details

to API clients.

---

# 10. Transaction Rules

Use transactions where multiple database operations must succeed or fail together.

Examples:

* creating an exercise and its participants
* changing exercise state with related records
* creating assessment results and scores
* updating related assessment information

Avoid unnecessarily large transactions.

Keep transaction boundaries primarily in the service layer.

---

# 11. Logging Rules

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

# 12. Testing Rules

Every new business feature should have appropriate tests.

Prioritize tests for:

1. Service business logic
2. Security and authorization
3. Assessment Engine
4. Scoring Engine
5. API validation
6. Repository behavior when necessary

Assessment and scoring logic should have strong automated test coverage because incorrect scoring directly affects exercise results.

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

# 13. Code Quality Rules

Follow the conventions in:

```text
docs/07-project-structure-coding-convention.md
```

Use:

* clear names
* small focused methods
* meaningful DTOs
* clear service boundaries
* constructor injection
* consistent exception handling
* consistent API responses

Avoid:

* unnecessary abstractions
* duplicated business logic
* giant service classes
* giant controllers
* magic numbers
* hard-coded business rules where configuration is appropriate
* premature microservices
* unnecessary design patterns

Prefer the simplest implementation that satisfies the documented architecture.

---

# 14. Scope Control

Work only on the requested task.

Do not modify unrelated modules unless necessary.

For example, if implementing the Team module, do not redesign:

* Assessment
* Scoring
* Authentication
* Database architecture
* API architecture

without a clear requirement.

If a task requires a change outside the current module:

1. Identify the dependency.
2. Check the relevant documentation.
3. Make the smallest necessary change.
4. Explain the cross-module change.

Do not perform large refactors while implementing an unrelated feature.

---

# 15. Documentation Consistency

When implementation changes a documented contract, check whether the documentation must also be updated.

Important documents:

```text
docs/01-overview.md
docs/02-architecture.md
docs/03-database.md
docs/04-security.md
docs/05-api.md
docs/06-assessment-and-scoring.md
docs/07-project-structure-coding-convention.md
```

Do not silently change the documented design.

If the implementation requires a design change, identify the affected document before making the change.

---

# 16. Git and Team Collaboration

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

### Commit principles

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

---

# 17. Claude Code Workflow

When starting a task:

### Step 1 — Understand

Read:

```text
CLAUDE.md
```

Then read only the relevant documents from:

```text
docs/01-overview.md
docs/02-architecture.md
docs/03-database.md
docs/04-security.md
docs/05-api.md
docs/06-assessment-and-scoring.md
docs/07-project-structure-coding-convention.md
```

### Step 2 — Inspect

Inspect the existing code before creating new files.

Do not assume that a class, endpoint, entity, or service does not already exist.

### Step 3 — Plan

Identify:

* files that need to change
* existing code that can be reused
* dependencies with other modules
* tests that should be added

### Step 4 — Implement

Implement the smallest complete solution consistent with the project documentation.

### Step 5 — Test

Run relevant tests.

For backend changes, normally run:

```powershell
.\mvnw.cmd test
```

### Step 6 — Review

Review:

```bash
git diff
git status
```

Check for:

* unintended changes
* security issues
* missing validation
* broken API contracts
* duplicated logic
* missing tests

### Step 7 — Report

Clearly report:

* what was changed
* which files were changed
* tests executed
* any remaining issues
* any documentation that may need updating

---

# 18. Rules for Ambiguous Requirements

When a requirement is ambiguous, do not invent important architectural behavior.

Ask for clarification when ambiguity affects:

* database schema
* API contract
* authentication
* authorization
* assessment logic
* scoring rules
* exercise roles
* system roles
* architecture
* data ownership

For minor implementation details, choose the simplest solution consistent with the existing documentation.

---

# 19. Important Project Constraints

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
12. V1 should remain simple and extensible.
13. Do not introduce unnecessary technologies or architectural complexity.
14. Do not silently change the project's documented architecture.

---

# 20. Definition of Done

A task is considered complete when:

* The implementation follows the documented architecture.
* Security rules are respected.
* API behavior matches the API contract.
* Database changes match the database design.
* Relevant validation is implemented.
* Relevant tests are added or updated.
* Tests pass.
* No secrets are introduced.
* No unrelated files are modified unnecessarily.
* Documentation is updated when a documented contract changes.
* The final diff has been reviewed.

The goal is not simply to make the code work.

The goal is to make the code **consistent with the Cyber Range architecture, secure, testable, maintainable, and compatible with the work of the other developer.**
