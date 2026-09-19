# Database Design

## 1. Overview

Database của hệ thống Cyber Range được thiết kế để quản lý:

* Người dùng và phân quyền hệ thống.
* Team và thành viên.
* Scenario và các nhiệm vụ phòng thủ.
* Tiêu chí đánh giá và chấm điểm.
* Cyber Range và các tài sản trong môi trường.
* Exercise và người tham gia.
* Event và Evidence được thu thập trong quá trình thực hành.
* Assessment và kết quả đánh giá.
* Điểm số chi tiết.
* Incident Report.
* Audit Log.

Database sử dụng **PostgreSQL**.

Thiết kế tập trung vào mô hình:

```text
Scenario
    ↓
Exercise
    ↓
Event / Evidence
    ↓
Assessment
    ↓
Assessment Result
    ↓
Assessment Score
    ↓
Report
```

Database được thiết kế theo hướng có thể mở rộng thêm scenario, task và assessment criteria mà không cần thay đổi cấu trúc database cho từng tình huống cụ thể.

---

# 2. Database Principles

## 2.1. Scenario và Exercise được tách riêng

`Scenario` là template định nghĩa một bài thực hành.

`Exercise` là một lần thực thi cụ thể của Scenario.

Ví dụ:

```text
Scenario: SSH Brute Force
        │
        ├── Exercise #001
        ├── Exercise #002
        └── Exercise #003
```

Do đó:

```text
scenarios 1 ─── N exercises
```

---

## 2.2. Event và Evidence được tách riêng

`Event` là dữ liệu sự kiện quan sát được từ Cyber Range.

`Evidence` là dữ liệu đã được Assessment Engine xác định là có giá trị đối với việc đánh giá.

Luồng:

```text
Cyber Range
    ↓
Event
    ↓
Assessment Engine
    ↓
Evidence
```

Không sử dụng Event trực tiếp như kết quả đánh giá.

---

## 2.3. Assessment và Scoring được tách riêng

Assessment xác định trạng thái hoàn thành của Defensive Task.

Scoring xác định số điểm đạt được dựa trên Assessment Criteria.

```text
Defensive Task
      ↓
Assessment Result
      ↓
Assessment Score
      ↓
Total Score
```

Điều này cho phép thay đổi cách chấm điểm mà không cần thay đổi logic xác định task completion.

---

## 2.4. Red Team, Blue Team và White Team

Không tạo các bảng riêng:

```text
red_teams
blue_teams
white_teams
```

Thay vào đó, vai trò trong một Exercise được lưu trong:

```text
exercise_participants.role
```

Các giá trị:

```text
RED
BLUE
WHITE
```

Điều này cho phép cùng một User tham gia các Exercise khác nhau với vai trò khác nhau.

Ví dụ:

```text
Exercise #001
    User A → RED
    User B → BLUE
    User C → WHITE

Exercise #002
    User A → BLUE
    User B → WHITE
```

System role và Exercise role là hai khái niệm khác nhau.

### System Role

```text
ADMIN
INSTRUCTOR
STUDENT
```

### Exercise Role

```text
RED
BLUE
WHITE
```

---

# 3. Entity List

Database V1 gồm 19 bảng:

| #  | Table                   | Purpose                         |
| -- | ----------------------- | ------------------------------- |
| 1  | `roles`                 | System-level roles              |
| 2  | `users`                 | User accounts                   |
| 3  | `teams`                 | User groups                     |
| 4  | `team_members`          | User-Team relationship          |
| 5  | `scenarios`             | Scenario templates              |
| 6  | `defensive_tasks`       | Blue Team tasks                 |
| 7  | `assessment_criteria`   | Assessment/scoring criteria     |
| 8  | `cyber_ranges`          | Cyber Range environments        |
| 9  | `assets`                | Machines and other assets       |
| 10 | `services`              | Services running on assets      |
| 11 | `exercises`             | Scenario execution              |
| 12 | `exercise_participants` | Exercise participants and roles |
| 13 | `events`                | Raw/observed events             |
| 14 | `evidence`              | Assessment-relevant evidence    |
| 15 | `assessments`           | Exercise assessment             |
| 16 | `assessment_results`    | Result per defensive task       |
| 17 | `assessment_scores`     | Score per criterion             |
| 18 | `incident_reports`      | Blue Team reports               |
| 19 | `audit_logs`            | System audit trail              |

---

# 4. Entity Relationships

## 4.1. User and Role

```text
roles 1 ───── N users
```

Foreign Key:

```text
users.role_id → roles.id
```

Một Role có thể được gán cho nhiều User.

Một User thuộc một System Role.

---

## 4.2. User and Team

User và Team có quan hệ N-N.

```text
users N ───── N teams
```

Quan hệ được triển khai thông qua:

```text
team_members
```

Chi tiết:

```text
users 1 ───── N team_members N ───── 1 teams
```

Foreign Keys:

```text
team_members.user_id → users.id
team_members.team_id → teams.id
```

---

## 4.3. Scenario and Defensive Task

```text
scenarios 1 ───── N defensive_tasks
```

Foreign Key:

```text
defensive_tasks.scenario_id → scenarios.id
```

Một Scenario có nhiều Defensive Task.

Ví dụ:

```text
SSH Brute Force
    ├── Detect Brute Force
    ├── Identify Compromised Host
    ├── Isolate Host
    ├── Recover Service
    └── Incident Report
```

---

## 4.4. Defensive Task and Assessment Criteria

```text
defensive_tasks 1 ───── N assessment_criteria
```

Foreign Key:

```text
assessment_criteria.defensive_task_id
    → defensive_tasks.id
```

Một Defensive Task có thể có một hoặc nhiều Criteria.

---

## 4.5. Cyber Range and Asset

```text
cyber_ranges 1 ───── N assets
```

Foreign Key:

```text
assets.cyber_range_id → cyber_ranges.id
```

Một Cyber Range có nhiều Asset.

Asset có thể là:

```text
WEB_SERVER
LINUX_SERVER
WINDOWS_SERVER
DATABASE_SERVER
NETWORK_DEVICE
```

Thiết kế sử dụng một bảng `assets` thay vì tạo các bảng riêng cho từng loại server.

---

## 4.6. Asset and Service

```text
assets 1 ───── N services
```

Foreign Key:

```text
services.asset_id → assets.id
```

Một Asset có thể chạy nhiều Service.

Ví dụ:

```text
Linux Server
    ├── SSH :22
    ├── HTTP :80
    └── HTTPS :443
```

---

## 4.7. Scenario and Exercise

```text
scenarios 1 ───── N exercises
```

Foreign Key:

```text
exercises.scenario_id → scenarios.id
```

Một Scenario có thể được thực hiện nhiều lần.

---

## 4.8. Cyber Range and Exercise

```text
cyber_ranges 1 ───── N exercises
```

Foreign Key:

```text
exercises.cyber_range_id → cyber_ranges.id
```

Một Cyber Range có thể được sử dụng cho nhiều Exercise.

---

## 4.9. Exercise and Participant

```text
exercises 1 ───── N exercise_participants
users     1 ───── N exercise_participants
teams     1 ───── N exercise_participants
```

Foreign Keys:

```text
exercise_participants.exercise_id → exercises.id
exercise_participants.user_id     → users.id
exercise_participants.team_id     → teams.id
```

Quan hệ nghiệp vụ:

```text
users N ───── N exercises
```

được triển khai thông qua:

```text
exercise_participants
```

`team_id` có thể `NULL`, ví dụ White Team participant không nhất thiết thuộc một Team.

---

# 5. Event and Evidence Relationships

## 5.1. Exercise and Event

```text
exercises 1 ───── N events
```

Foreign Key:

```text
events.exercise_id → exercises.id
```

Mỗi Exercise có thể tạo ra nhiều Event.

---

## 5.2. Asset and Event

```text
assets 1 ───── N events
```

Foreign Key:

```text
events.asset_id → assets.id
```

Event có thể liên quan tới một Asset.

`asset_id` có thể `NULL` nếu event không xác định được Asset cụ thể.

---

## 5.3. Service and Event

```text
services 1 ───── N events
```

Foreign Key:

```text
events.service_id → services.id
```

`service_id` có thể `NULL` vì không phải Event nào cũng thuộc một Service cụ thể.

---

## 5.4. Exercise and Evidence

```text
exercises 1 ───── N evidence
```

Foreign Key:

```text
evidence.exercise_id → exercises.id
```

Evidence thuộc về một Exercise cụ thể.

---

## 5.5. Event and Evidence

```text
events 1 ───── N evidence
```

Foreign Key:

```text
evidence.event_id → events.id
```

Trong V1, Evidence có thể tham chiếu Event làm nguồn.

`event_id` có thể `NULL` để cho phép Evidence được tạo từ nhiều nguồn hoặc logic tổng hợp trong tương lai.

---

# 6. Assessment Relationships

## 6.1. Exercise and Assessment

V1 sử dụng quan hệ:

```text
exercises 1 ───── 1 assessments
```

Foreign Key:

```text
assessments.exercise_id → exercises.id
```

`assessments.exercise_id` được đặt `UNIQUE`.

Điều này đảm bảo một Exercise chỉ có một Assessment chính trong V1.

---

## 6.2. Assessment and Assessment Result

```text
assessments 1 ───── N assessment_results
```

Foreign Key:

```text
assessment_results.assessment_id
    → assessments.id
```

Một Assessment có kết quả cho nhiều Defensive Task.

---

## 6.3. Defensive Task and Assessment Result

```text
defensive_tasks 1 ───── N assessment_results
```

Foreign Key:

```text
assessment_results.defensive_task_id
    → defensive_tasks.id
```

Một Defensive Task có thể xuất hiện trong kết quả của nhiều Exercise/Assessment khác nhau.

---

## 6.4. Assessment Result and Assessment Score

```text
assessment_results 1 ───── N assessment_scores
```

Foreign Key:

```text
assessment_scores.assessment_result_id
    → assessment_results.id
```

Một Assessment Result có thể có nhiều Score Detail.

---

## 6.5. Assessment Criteria and Assessment Score

```text
assessment_criteria 1 ───── N assessment_scores
```

Foreign Key:

```text
assessment_scores.criterion_id
    → assessment_criteria.id
```

Một Criterion có thể được sử dụng để chấm điểm trong nhiều Assessment.

---

# 7. Incident Report Relationships

## 7.1. Exercise and Incident Report

```text
exercises 1 ───── N incident_reports
```

Foreign Key:

```text
incident_reports.exercise_id → exercises.id
```

Một Exercise có thể có một hoặc nhiều Incident Report.

---

## 7.2. User and Incident Report

User có hai vai trò khác nhau đối với Incident Report.

### Submitter

```text
users 1 ───── N incident_reports
```

Foreign Key:

```text
incident_reports.submitted_by → users.id
```

### Reviewer

```text
users 1 ───── N incident_reports
```

Foreign Key:

```text
incident_reports.reviewed_by → users.id
```

`reviewed_by` có thể `NULL` cho tới khi White Team hoặc người có quyền review báo cáo.

---

# 8. Audit Log Relationship

```text
users 1 ───── N audit_logs
```

Foreign Key:

```text
audit_logs.user_id → users.id
```

Audit Log ghi lại các thao tác quan trọng của người dùng.

Ví dụ:

```text
START_EXERCISE
PAUSE_EXERCISE
RESUME_EXERCISE
STOP_EXERCISE
RESET_EXERCISE
REVIEW_REPORT
UPDATE_SCENARIO
```

`user_id` có thể `NULL` nếu cần ghi lại system-generated event.

---

# 9. Foreign Key Summary

| Table                   | Foreign Key            | References               |
| ----------------------- | ---------------------- | ------------------------ |
| `users`                 | `role_id`              | `roles.id`               |
| `team_members`          | `user_id`              | `users.id`               |
| `team_members`          | `team_id`              | `teams.id`               |
| `defensive_tasks`       | `scenario_id`          | `scenarios.id`           |
| `assessment_criteria`   | `defensive_task_id`    | `defensive_tasks.id`     |
| `assets`                | `cyber_range_id`       | `cyber_ranges.id`        |
| `services`              | `asset_id`             | `assets.id`              |
| `exercises`             | `scenario_id`          | `scenarios.id`           |
| `exercises`             | `cyber_range_id`       | `cyber_ranges.id`        |
| `exercises`             | `created_by`           | `users.id`               |
| `exercise_participants` | `exercise_id`          | `exercises.id`           |
| `exercise_participants` | `user_id`              | `users.id`               |
| `exercise_participants` | `team_id`              | `teams.id`               |
| `events`                | `exercise_id`          | `exercises.id`           |
| `events`                | `asset_id`             | `assets.id`              |
| `events`                | `service_id`           | `services.id`            |
| `evidence`              | `exercise_id`          | `exercises.id`           |
| `evidence`              | `event_id`             | `events.id`              |
| `assessments`           | `exercise_id`          | `exercises.id`           |
| `assessments`           | `reviewed_by`          | `users.id`               |
| `assessment_results`    | `assessment_id`        | `assessments.id`         |
| `assessment_results`    | `defensive_task_id`    | `defensive_tasks.id`     |
| `assessment_scores`     | `assessment_result_id` | `assessment_results.id`  |
| `assessment_scores`     | `criterion_id`         | `assessment_criteria.id` |
| `incident_reports`      | `exercise_id`          | `exercises.id`           |
| `incident_reports`      | `submitted_by`         | `users.id`               |
| `incident_reports`      | `reviewed_by`          | `users.id`               |
| `audit_logs`            | `user_id`              | `users.id`               |

---

# 10. Cardinality Summary

## One-to-Many

```text
roles               1 ─── N users

scenarios           1 ─── N defensive_tasks
defensive_tasks     1 ─── N assessment_criteria

cyber_ranges        1 ─── N assets
assets              1 ─── N services

scenarios           1 ─── N exercises
cyber_ranges        1 ─── N exercises

users               1 ─── N exercise_participants
teams               1 ─── N exercise_participants
exercises           1 ─── N exercise_participants

exercises           1 ─── N events
assets              1 ─── N events
services            1 ─── N events

exercises           1 ─── N evidence
events              1 ─── N evidence

assessments         1 ─── N assessment_results
defensive_tasks     1 ─── N assessment_results

assessment_results  1 ─── N assessment_scores
assessment_criteria 1 ─── N assessment_scores

exercises           1 ─── N incident_reports
users               1 ─── N incident_reports
users               1 ─── N audit_logs
```

## One-to-One

```text
exercises 1 ─── 1 assessments
```

## Many-to-Many

```text
users N ─── N teams
```

thông qua:

```text
team_members
```

và:

```text
users N ─── N exercises
```

thông qua:

```text
exercise_participants
```

---

# 11. Table Details

## 11.1. `roles`

Purpose: System-level authorization roles.

| Column        | Type        | Constraint       | Description      |
| ------------- | ----------- | ---------------- | ---------------- |
| `id`          | BIGSERIAL   | PK               | Role ID          |
| `name`        | VARCHAR(50) | UNIQUE, NOT NULL | Role name        |
| `description` | TEXT        |                  | Role description |
| `created_at`  | TIMESTAMPTZ | NOT NULL         | Creation time    |

Initial roles:

```text
ADMIN
INSTRUCTOR
STUDENT
```

Exercise roles such as `RED`, `BLUE`, `WHITE` are not stored here.

---

## 11.2. `users`

Purpose: User accounts.

| Column          | Type         | Constraint       | Description    |
| --------------- | ------------ | ---------------- | -------------- |
| `id`            | BIGSERIAL    | PK               | User ID        |
| `role_id`       | BIGINT       | FK, NOT NULL     | System role    |
| `username`      | VARCHAR(100) | UNIQUE, NOT NULL | Login username |
| `email`         | VARCHAR(255) | UNIQUE, NOT NULL | Email          |
| `password_hash` | VARCHAR(255) | NOT NULL         | Password hash  |
| `full_name`     | VARCHAR(255) | NOT NULL         | Display name   |
| `status`        | VARCHAR(30)  | NOT NULL         | Account status |
| `created_at`    | TIMESTAMPTZ  | NOT NULL         | Creation time  |
| `updated_at`    | TIMESTAMPTZ  | NOT NULL         | Last update    |

---

## 11.3. `teams`

Purpose: Groups of users.

| Column        | Type         | Constraint       | Description      |
| ------------- | ------------ | ---------------- | ---------------- |
| `id`          | BIGSERIAL    | PK               | Team ID          |
| `name`        | VARCHAR(100) | UNIQUE, NOT NULL | Team name        |
| `description` | TEXT         |                  | Team description |
| `created_at`  | TIMESTAMPTZ  | NOT NULL         | Creation time    |

---

## 11.4. `team_members`

Purpose: N-N relationship between users and teams.

| Column      | Type        | Constraint |
| ----------- | ----------- | ---------- |
| `team_id`   | BIGINT      | PK, FK     |
| `user_id`   | BIGINT      | PK, FK     |
| `joined_at` | TIMESTAMPTZ | NOT NULL   |

Composite Primary Key:

```text
(team_id, user_id)
```

---

## 11.5. `scenarios`

Purpose: Reusable Cyber Range exercise templates.

| Column        | Type         | Constraint | Description          |
| ------------- | ------------ | ---------- | -------------------- |
| `id`          | BIGSERIAL    | PK         | Scenario ID          |
| `name`        | VARCHAR(150) | NOT NULL   | Scenario name        |
| `description` | TEXT         |            | Scenario description |
| `type`        | VARCHAR(50)  | NOT NULL   | Scenario type        |
| `difficulty`  | VARCHAR(30)  |            | Difficulty           |
| `status`      | VARCHAR(30)  | NOT NULL   | Scenario lifecycle   |
| `created_by`  | BIGINT       | FK         | Creator              |
| `created_at`  | TIMESTAMPTZ  | NOT NULL   | Creation time        |
| `updated_at`  | TIMESTAMPTZ  | NOT NULL   | Last update          |

Initial scenario types:

```text
PORT_SCANNING
SSH_BRUTE_FORCE
WEB_ATTACK
```

---

## 11.6. `defensive_tasks`

Purpose: Blue Team tasks belonging to a Scenario.

| Column           | Type         | Constraint   |
| ---------------- | ------------ | ------------ |
| `id`             | BIGSERIAL    | PK           |
| `scenario_id`    | BIGINT       | FK, NOT NULL |
| `name`           | VARCHAR(150) | NOT NULL     |
| `description`    | TEXT         |              |
| `task_type`      | VARCHAR(50)  | NOT NULL     |
| `sequence_order` | INTEGER      | NOT NULL     |
| `max_score`      | NUMERIC(5,2) | NOT NULL     |
| `required`       | BOOLEAN      | NOT NULL     |

---

## 11.7. `assessment_criteria`

Purpose: Defines how a Defensive Task is scored.

| Column              | Type         | Constraint   |
| ------------------- | ------------ | ------------ |
| `id`                | BIGSERIAL    | PK           |
| `defensive_task_id` | BIGINT       | FK, NOT NULL |
| `name`              | VARCHAR(150) | NOT NULL     |
| `description`       | TEXT         |              |
| `criterion_type`    | VARCHAR(50)  | NOT NULL     |
| `max_score`         | NUMERIC(5,2) | NOT NULL     |

---

## 11.8. `cyber_ranges`

Purpose: Defines a Cyber Range environment.

| Column             | Type         | Constraint       |
| ------------------ | ------------ | ---------------- |
| `id`               | BIGSERIAL    | PK               |
| `name`             | VARCHAR(150) | UNIQUE, NOT NULL |
| `description`      | TEXT         |                  |
| `environment_type` | VARCHAR(50)  |                  |
| `status`           | VARCHAR(30)  | NOT NULL         |
| `created_at`       | TIMESTAMPTZ  | NOT NULL         |

---

## 11.9. `assets`

Purpose: Represents machines and other Cyber Range assets.

| Column           | Type         | Constraint   |
| ---------------- | ------------ | ------------ |
| `id`             | BIGSERIAL    | PK           |
| `cyber_range_id` | BIGINT       | FK, NOT NULL |
| `name`           | VARCHAR(150) | NOT NULL     |
| `asset_type`     | VARCHAR(50)  | NOT NULL     |
| `hostname`       | VARCHAR(255) |              |
| `ip_address`     | INET         |              |
| `os`             | VARCHAR(100) |              |
| `status`         | VARCHAR(30)  | NOT NULL     |

---

## 11.10. `services`

Purpose: Services running on Assets.

| Column         | Type         | Constraint   |
| -------------- | ------------ | ------------ |
| `id`           | BIGSERIAL    | PK           |
| `asset_id`     | BIGINT       | FK, NOT NULL |
| `name`         | VARCHAR(150) | NOT NULL     |
| `service_type` | VARCHAR(50)  | NOT NULL     |
| `port`         | INTEGER      |              |
| `protocol`     | VARCHAR(20)  |              |
| `status`       | VARCHAR(30)  | NOT NULL     |

---

## 11.11. `exercises`

Purpose: Actual execution of a Scenario.

| Column           | Type         | Constraint   |
| ---------------- | ------------ | ------------ |
| `id`             | BIGSERIAL    | PK           |
| `scenario_id`    | BIGINT       | FK, NOT NULL |
| `cyber_range_id` | BIGINT       | FK, NOT NULL |
| `name`           | VARCHAR(150) | NOT NULL     |
| `status`         | VARCHAR(30)  | NOT NULL     |
| `started_at`     | TIMESTAMPTZ  |              |
| `ended_at`       | TIMESTAMPTZ  |              |
| `created_by`     | BIGINT       | FK           |
| `created_at`     | TIMESTAMPTZ  | NOT NULL     |

Exercise lifecycle:

```text
CREATED
   ↓
RUNNING
   ↓
PAUSED
   ↓
RUNNING
   ↓
COMPLETED
```

Alternative termination:

```text
CANCELLED
RESET
```

---

## 11.12. `exercise_participants`

Purpose: Assign users and optional teams to an Exercise.

| Column        | Type        | Constraint   |
| ------------- | ----------- | ------------ |
| `id`          | BIGSERIAL   | PK           |
| `exercise_id` | BIGINT      | FK, NOT NULL |
| `user_id`     | BIGINT      | FK, NOT NULL |
| `team_id`     | BIGINT      | FK, nullable |
| `role`        | VARCHAR(20) | NOT NULL     |
| `joined_at`   | TIMESTAMPTZ | NOT NULL     |

Allowed exercise roles:

```text
RED
BLUE
WHITE
```

---

## 11.13. `events`

Purpose: Raw/observed events from the Cyber Range.

| Column            | Type         | Constraint   |
| ----------------- | ------------ | ------------ |
| `id`              | BIGSERIAL    | PK           |
| `exercise_id`     | BIGINT       | FK, NOT NULL |
| `asset_id`        | BIGINT       | FK, nullable |
| `service_id`      | BIGINT       | FK, nullable |
| `event_type`      | VARCHAR(50)  | NOT NULL     |
| `severity`        | VARCHAR(20)  |              |
| `event_timestamp` | TIMESTAMPTZ  | NOT NULL     |
| `source`          | VARCHAR(100) |              |
| `data`            | JSONB        |              |
| `created_at`      | TIMESTAMPTZ  | NOT NULL     |

Example event types:

```text
NETWORK
AUTHENTICATION
WEB
SYSTEM
FIREWALL
SECURITY
SERVICE_STATE
```

---

## 11.14. `evidence`

Purpose: Assessment-relevant evidence.

| Column          | Type        | Constraint   |
| --------------- | ----------- | ------------ |
| `id`            | BIGSERIAL   | PK           |
| `exercise_id`   | BIGINT      | FK, NOT NULL |
| `event_id`      | BIGINT      | FK, nullable |
| `evidence_type` | VARCHAR(50) | NOT NULL     |
| `data`          | JSONB       |              |
| `created_at`    | TIMESTAMPTZ | NOT NULL     |

---

## 11.15. `assessments`

Purpose: Main assessment of an Exercise.

| Column           | Type         | Constraint   |
| ---------------- | ------------ | ------------ |
| `id`             | BIGSERIAL    | PK           |
| `exercise_id`    | BIGINT       | FK, UNIQUE   |
| `status`         | VARCHAR(30)  | NOT NULL     |
| `total_score`    | NUMERIC(7,2) | NOT NULL     |
| `started_at`     | TIMESTAMPTZ  |              |
| `completed_at`   | TIMESTAMPTZ  |              |
| `review_status`  | VARCHAR(30)  | NOT NULL     |
| `reviewed_by`    | BIGINT       | FK, nullable |
| `reviewed_at`    | TIMESTAMPTZ  |              |
| `review_comment` | TEXT         |              |
| `created_at`     | TIMESTAMPTZ  | NOT NULL     |

---

## 11.16. `assessment_results`

Purpose: Assessment result for each Defensive Task.

| Column              | Type        | Constraint   |
| ------------------- | ----------- | ------------ |
| `id`                | BIGSERIAL   | PK           |
| `assessment_id`     | BIGINT      | FK, NOT NULL |
| `defensive_task_id` | BIGINT      | FK, NOT NULL |
| `status`            | VARCHAR(30) | NOT NULL     |
| `detected_at`       | TIMESTAMPTZ |              |
| `responded_at`      | TIMESTAMPTZ |              |
| `completed_at`      | TIMESTAMPTZ |              |
| `detection_time`    | INTERVAL    |              |
| `response_time`     | INTERVAL    |              |
| `reason`            | TEXT        |              |

Unique constraint:

```text
(assessment_id, defensive_task_id)
```

Một Task chỉ có một kết quả trong một Assessment.

---

## 11.17. `assessment_scores`

Purpose: Detailed score awarded for each Assessment Criterion.

| Column                 | Type         | Constraint   |
| ---------------------- | ------------ | ------------ |
| `id`                   | BIGSERIAL    | PK           |
| `assessment_result_id` | BIGINT       | FK, NOT NULL |
| `criterion_id`         | BIGINT       | FK, NOT NULL |
| `awarded_score`        | NUMERIC(5,2) | NOT NULL     |
| `max_score`            | NUMERIC(5,2) | NOT NULL     |
| `reason`               | TEXT         |              |

Constraint:

```text
awarded_score <= max_score
```

---

## 11.18. `incident_reports`

Purpose: Incident reports submitted by Blue Team and reviewed by authorized users.

| Column           | Type         | Constraint   |
| ---------------- | ------------ | ------------ |
| `id`             | BIGSERIAL    | PK           |
| `exercise_id`    | BIGINT       | FK, NOT NULL |
| `submitted_by`   | BIGINT       | FK, NOT NULL |
| `title`          | VARCHAR(255) | NOT NULL     |
| `description`    | TEXT         | NOT NULL     |
| `severity`       | VARCHAR(20)  |              |
| `status`         | VARCHAR(30)  | NOT NULL     |
| `submitted_at`   | TIMESTAMPTZ  | NOT NULL     |
| `reviewed_by`    | BIGINT       | FK, nullable |
| `reviewed_at`    | TIMESTAMPTZ  |              |
| `review_comment` | TEXT         |              |

---

## 11.19. `audit_logs`

Purpose: System audit trail.

| Column        | Type         | Constraint   |
| ------------- | ------------ | ------------ |
| `id`          | BIGSERIAL    | PK           |
| `user_id`     | BIGINT       | FK, nullable |
| `action`      | VARCHAR(100) | NOT NULL     |
| `entity_type` | VARCHAR(100) |              |
| `entity_id`   | BIGINT       |              |
| `created_at`  | TIMESTAMPTZ  | NOT NULL     |
| `metadata`    | JSONB        |              |

---

# 12. V1 Scoring Model

V1 sử dụng tổng điểm 100:

| Defensive Task            | Max Score |
| ------------------------- | --------: |
| Detect Port Scan          |        10 |
| Detect Brute Force        |        10 |
| Detect Web Attack         |        15 |
| Identify Compromised Host |        15 |
| Isolate Attacked Host     |        20 |
| Recover Service           |        20 |
| Incident Report           |        10 |
| **Total**                 |   **100** |

Các giá trị điểm được lưu trong:

```text
assessment_criteria.max_score
```

Không hard-code tổng điểm 100 vào database schema.

Tổng điểm của một Assessment được lưu tại:

```text
assessments.total_score
```

Chi tiết điểm được lưu tại:

```text
assessment_scores
```

---

# 13. Assessment Flow

Quá trình đánh giá:

```text
Cyber Range
     │
     │ events
     ▼
  events
     │
     ▼
Assessment Engine
     │
     │ generates
     ▼
  evidence
     │
     ▼
Assessment
     │
     ├── Assessment Result
     │       │
     │       └── Assessment Score
     │
     ▼
Total Score
```

Assessment Engine sử dụng các Event/Evidence để xác định trạng thái của Defensive Task.

Ví dụ:

```text
SSH Brute Force
      ↓
Authentication Events
      ↓
Assessment Engine
      ↓
Evidence
      ↓
Detect Brute Force
      ↓
COMPLETED
      ↓
10 points
```

---

# 14. Detection Time and Response Time

Database hỗ trợ đo:

```text
Detection Time
Response Time
```

Các field chính:

```text
assessment_results.detected_at
assessment_results.responded_at
assessment_results.completed_at
assessment_results.detection_time
assessment_results.response_time
```

Khái niệm:

```text
Detection Time
= detected_at - attack/event reference time

Response Time
= responded_at - detected_at
```

Việc xác định chính xác event nào là mốc bắt đầu sẽ thuộc về Assessment Engine và scenario configuration.

---

# 15. White Team Data Model

White Team không có bảng riêng.

White Team được biểu diễn bằng:

```text
exercise_participants.role = 'WHITE'
```

White Team có thể thực hiện các hoạt động như:

```text
START_EXERCISE
PAUSE_EXERCISE
RESUME_EXERCISE
STOP_EXERCISE
RESET_EXERCISE
MONITOR_EXERCISE
REVIEW_REPORT
REVIEW_ASSESSMENT
```

Các hành động quan trọng được ghi vào:

```text
audit_logs
```

Incident Report có thể được White Team review thông qua:

```text
incident_reports.reviewed_by
incident_reports.reviewed_at
incident_reports.review_comment
```

Assessment có thể được review thông qua:

```text
assessments.reviewed_by
assessments.reviewed_at
assessments.review_comment
assessments.review_status
```

---

# 16. Data Flow Example

Ví dụ một Exercise:

```text
Scenario
SSH Brute Force
       │
       ▼
Exercise #001
       │
       ├── RED User A
       ├── BLUE User B
       └── WHITE User C
       │
       ▼
Cyber Range
       │
       └── Linux Server
               │
               └── SSH :22
       │
       ▼
Events
       │
       ├── Authentication Failed
       ├── Authentication Failed
       ├── Authentication Failed
       └── ...
       │
       ▼
Assessment Engine
       │
       ▼
Evidence
       │
       ▼
Assessment #001
       │
       ├── Detect Brute Force
       │       └── COMPLETED
       │
       ├── Identify Compromised Host
       │       └── COMPLETED
       │
       ├── Isolate Host
       │       └── COMPLETED
       │
       └── Incident Report
               └── COMPLETED
       │
       ▼
Assessment Scores
       │
       ├── 10
       ├── 15
       ├── 20
       └── 10
       │
       ▼
Total Score
```

---

# 17. Delete Rules

Một số quan hệ sử dụng `ON DELETE CASCADE` để tránh dữ liệu con không còn tham chiếu.

Các quan hệ chính:

```text
teams
    ↓
team_members
```

```text
scenarios
    ↓
defensive_tasks
    ↓
assessment_criteria
```

```text
cyber_ranges
    ↓
assets
    ↓
services
```

```text
exercises
    ↓
events
    ↓
evidence
```

```text
exercises
    ↓
assessments
    ↓
assessment_results
    ↓
assessment_scores
```

Đối với User được tham chiếu bởi các dữ liệu lịch sử, một số quan hệ sử dụng:

```text
ON DELETE SET NULL
```

để không làm mất dữ liệu lịch sử khi User bị xóa hoặc vô hiệu hóa.

---

# 18. Indexing Strategy

Các Foreign Key và các trường thường được sử dụng để tìm kiếm được index.

Các nhóm index chính:

```text
User
    └── role_id

Team Membership
    ├── user_id
    └── team_id

Scenario
    └── scenario_id

Cyber Range
    └── cyber_range_id

Exercise
    ├── scenario_id
    ├── cyber_range_id
    └── created_by

Exercise Participant
    ├── exercise_id
    ├── user_id
    └── team_id

Events
    ├── exercise_id
    ├── asset_id
    ├── service_id
    └── event_timestamp

Evidence
    ├── exercise_id
    └── event_id

Assessment
    ├── assessment_id
    └── defensive_task_id

Incident Report
    ├── exercise_id
    ├── submitted_by
    └── reviewed_by

Audit Log
    ├── user_id
    └── entity_type + entity_id
```

Đặc biệt `events.event_timestamp` được index vì Event là một trong những loại dữ liệu có thể tăng nhanh nhất trong Cyber Range.

---

# 19. Tables Intentionally Not Included

Các bảng sau **không thuộc Database V1**:

```text
red_teams
blue_teams
white_teams

attack_scenarios

scores

score_details

assessment_reviews

linux_servers
windows_servers
web_servers
database_servers
ssh_servers
active_directory
```

Lý do chính:

* Red/Blue/White được quản lý bằng `exercise_participants.role`.
* Server được quản lý thống nhất bằng `assets`.
* Service được quản lý bằng `services`.
* Score detail được quản lý bằng `assessment_scores`.
* Review information được đặt trong `assessments` và `incident_reports`.
* `attack_scenarios` chưa cần thiết khi V1 chỉ sử dụng các Scenario đơn giản.

Nếu Scenario trong tương lai trở thành multi-step/multi-phase, có thể bổ sung một entity riêng mà không cần phá vỡ thiết kế hiện tại.

---

# 20. Database Structure

Cấu trúc logic cuối cùng:

```text
IDENTITY
├── roles
├── users
├── teams
└── team_members

SCENARIO
├── scenarios
├── defensive_tasks
└── assessment_criteria

CYBER RANGE
├── cyber_ranges
├── assets
└── services

EXERCISE
├── exercises
└── exercise_participants

EVENT / EVIDENCE
├── events
└── evidence

ASSESSMENT
├── assessments
├── assessment_results
└── assessment_scores

REPORTING
├── incident_reports
└── audit_logs
```

---

# 21. Final Relationship Model

```text
                         ┌──────────┐
                         │  roles   │
                         └────┬─────┘
                              │ 1
                              │
                              N
                         ┌────▼─────┐
                         │  users   │
                         └────┬─────┘
                              │
               ┌──────────────┼──────────────┐
               │              │              │
               N              N              N
        ┌──────▼──────┐       │       ┌──────▼──────┐
        │team_members │       │       │ audit_logs  │
        └──────┬──────┘       │       └─────────────┘
               │              │
               N              │
               │              │
               1              │
        ┌──────▼──────┐       │
        │    teams    │       │
        └─────────────┘       │
                              │
                              N
                   ┌──────────▼──────────┐
                   │exercise_participants│
                   └──────────┬──────────┘
                              │
                              N
                              │
                              1
                       ┌──────▼──────┐
                       │  exercises  │
                       └──────┬──────┘
                              │
             ┌────────────────┼────────────────┐
             │                │                │
             │                │                N
             │                │                │
             │                │          ┌─────▼─────┐
             │                │          │   events  │
             │                │          └─────┬─────┘
             │                │                │
             │                │                N
             │                │          ┌─────▼─────┐
             │                │          │  evidence │
             │                │          └───────────┘
             │                │
             │                1
             │          ┌─────▼──────┐
             │          │ assessments│
             │          └─────┬──────┘
             │                │ N
             │          ┌─────▼──────────┐
             │          │assessment_     │
             │          │results         │
             │          └─────┬──────────┘
             │                │ N
             │          ┌─────▼──────────┐
             │          │assessment_     │
             │          │scores          │
             │          └────────────────┘
             │
             N
       ┌─────▼──────────┐
       │incident_reports│
       └────────────────┘


scenarios
    │ 1
    │
    N
defensive_tasks
    │ 1
    │
    N
assessment_criteria


cyber_ranges
    │ 1
    │
    N
assets
    │ 1
    │
    N
services
```

---

# 22. Implementation Status

Database V1 is considered structurally defined when the following are completed:

* [x] Entity list
* [x] Primary Keys
* [x] Foreign Keys
* [x] Relationship
* [x] Cardinality
* [x] Nullable fields
* [x] Unique constraints
* [x] Basic Check constraints
* [x] Delete behavior
* [x] Index strategy
* [x] Assessment structure
* [x] Scoring structure
* [x] White Team representation
* [x] Incident Report structure
* [x] Audit Log structure

The actual PostgreSQL schema is maintained separately in:

```text
database/database.sql
```

The application layer will map these tables to Java entities using JPA/Hibernate.
