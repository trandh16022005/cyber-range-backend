# 05. API Contract

## 1. Mục đích

Tài liệu này định nghĩa API Contract của hệ thống **Cyber Range and Automated Defensive Skills Assessment System**.

API Contract là giao diện thống nhất giữa Web Frontend, Spring Boot Backend, Event Collector và các thành phần liên quan trong Cyber Range.

Tài liệu nhằm:

* Chuẩn hóa endpoint và HTTP method.
* Xác định request và response cơ bản.
* Xác định authentication và authorization.
* Phân biệt System Role và Exercise Role.
* Quy định cách API tương tác với Scenario, Exercise, Event, Evidence, Assessment và Scoring.
* Làm cơ sở để hai thành viên phát triển Backend và Frontend theo cùng một contract.
* Làm cơ sở cho Claude Code sinh và chỉnh sửa code mà không tự suy diễn API khác với thiết kế chung.

---

# 2. API Architecture

API được xây dựng theo mô hình REST API trên Spring Boot.

```text
Web Frontend
     |
     | HTTPS / REST API
     v
Spring Boot Backend
     |
     +-- Authentication
     +-- Authorization
     +-- User Management
     +-- Team Management
     +-- Scenario Management
     +-- Cyber Range Management
     +-- Exercise Management
     +-- Event Management
     +-- Assessment Engine
     +-- Scoring Engine
     +-- Incident Report
     +-- Audit
     |
     v
PostgreSQL
```

Event Collector kết nối với Backend để gửi các sự kiện phát sinh từ Cyber Range:

```text
Cyber Range
     |
     | Logs / Security Events / System State
     v
Event Collector
     |
     | Authenticated API
     v
Spring Boot Backend
     |
     v
Event
     |
     v
Evidence
     |
     v
Assessment Engine
     |
     v
Scoring Engine
```

---

# 3. Base URL

Trong môi trường phát triển:

```text
http://localhost:8080/api
```

Trong môi trường production:

```text
https://<domain>/api
```

API versioning có thể được bổ sung khi cần. V1 hiện sử dụng:

```text
/api/...
```

---

# 4. Authentication

API sử dụng JWT Bearer Token.

Sau khi đăng nhập thành công:

```http
Authorization: Bearer <access_token>
```

Client phải gửi JWT đối với các endpoint yêu cầu authentication.

Ví dụ:

```http
GET /api/auth/me
Authorization: Bearer eyJ...
```

Các endpoint public trong V1:

```text
POST /api/auth/login
```

Các endpoint còn lại yêu cầu authentication trừ khi được quy định khác.

---

# 5. Authorization Model

Hệ thống sử dụng hai tầng quyền:

```text
System Role
    |
    +-- ADMIN
    +-- INSTRUCTOR
    +-- STUDENT

Exercise Role
    |
    +-- RED
    +-- BLUE
    +-- WHITE
```

## 5.1. System Role

System Role xác định quyền ở cấp hệ thống.

### ADMIN

Quản trị hệ thống:

* Quản lý người dùng.
* Quản lý system role.
* Quản lý các tài nguyên hệ thống.
* Xem audit log.
* Thực hiện các thao tác quản trị được hệ thống cho phép.

### INSTRUCTOR

Quản lý hoạt động đào tạo:

* Quản lý Scenario.
* Quản lý Defensive Task.
* Quản lý Assessment Criteria.
* Quản lý Cyber Range trong phạm vi được cấp quyền.
* Tạo và quản lý Exercise.
* Theo dõi kết quả.
* Xem Assessment và Report.

### STUDENT

Người học:

* Tham gia Exercise.
* Thực hiện nhiệm vụ theo Exercise Role.
* Xem dữ liệu được phép trong Exercise.
* Gửi Incident Report khi được giao nhiệm vụ.

---

# 6. Exercise Role

Exercise Role chỉ có hiệu lực trong một Exercise cụ thể.

```text
RED
BLUE
WHITE
```

## RED

Thực hiện các hoạt động tấn công trong phạm vi Exercise.

## BLUE

Thực hiện các nhiệm vụ phòng thủ:

* Phân tích log.
* Phát hiện tấn công.
* Xác định máy bị ảnh hưởng.
* Cô lập máy.
* Khôi phục dịch vụ.
* Lập Incident Report.

## WHITE

Quản lý, giám sát và điều khiển Exercise:

* Theo dõi Exercise.
* Start / Pause / Resume / Stop / Reset.
* Theo dõi Event và Evidence.
* Xem Assessment.
* Review Incident Report.
* Theo dõi kết quả Exercise.

---

# 7. Nguyên tắc Authorization

API không tạo các namespace riêng:

```text
/api/red-team/...
/api/blue-team/...
/api/white-team/...
```

Thay vào đó, sử dụng resource chung:

```text
/api/exercises/...
```

Backend xác định quyền dựa trên:

```text
JWT
  |
  v
User
  |
  v
System Role
  |
  v
Exercise Participant
  |
  v
Exercise Role
  |
  v
Resource Access
```

Exercise Role phải được kiểm tra từ dữ liệu `exercise_participants`, không lấy trực tiếp từ JWT.

---

# 8. HTTP Status Codes

API sử dụng các HTTP status code chính:

| Status                      | Ý nghĩa                                     |
| --------------------------- | ------------------------------------------- |
| `200 OK`                    | Request thành công                          |
| `201 Created`               | Tạo resource thành công                     |
| `204 No Content`            | Request thành công, không có response body  |
| `400 Bad Request`           | Request không hợp lệ                        |
| `401 Unauthorized`          | Chưa authentication hoặc token không hợp lệ |
| `403 Forbidden`             | Đã authentication nhưng không có quyền      |
| `404 Not Found`             | Resource không tồn tại                      |
| `409 Conflict`              | Xung đột dữ liệu hoặc trạng thái            |
| `422 Unprocessable Entity`  | Dữ liệu không vượt qua validation nghiệp vụ |
| `500 Internal Server Error` | Lỗi server không mong muốn                  |

---

# 9. Response Format

API nên sử dụng response JSON thống nhất.

## 9.1. Success

Ví dụ:

```json
{
  "data": {
    "id": 1,
    "name": "SSH Brute Force"
  }
}
```

Danh sách:

```json
{
  "data": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## 9.2. Error

```json
{
  "timestamp": "2026-10-03T13:00:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Invalid request",
  "path": "/api/exercises"
}
```

Chi tiết validation:

```json
{
  "timestamp": "2026-10-03T13:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/users",
  "errors": {
    "username": "Username is required",
    "email": "Invalid email"
  }
}
```

---

# 10. Authentication API

## 10.1. Login

```http
POST /api/auth/login
```

Authentication:

```text
Public
```

Request:

```json
{
  "username": "student01",
  "password": "********"
}
```

Response:

```json
{
  "data": {
    "accessToken": "<JWT>",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "student01",
      "fullName": "Student 01",
      "role": "STUDENT"
    }
  }
}
```

Possible status:

```text
200 OK
400 Bad Request
401 Unauthorized
423 Locked
```

---

## 10.2. Current User

```http
GET /api/auth/me
```

Authentication:

```text
Authenticated User
```

Response:

```json
{
  "data": {
    "id": 1,
    "username": "student01",
    "email": "student01@example.com",
    "fullName": "Student 01",
    "status": "ACTIVE",
    "role": "STUDENT"
  }
}
```

---

# 11. User API

Resource:

```text
/api/users
```

## 11.1. List Users

```http
GET /api/users
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Query parameters:

```text
page
size
status
role
search
```

---

## 11.2. Get User

```http
GET /api/users/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 11.3. Create User

```http
POST /api/users
```

Roles:

```text
ADMIN
```

Request:

```json
{
  "username": "student01",
  "email": "student01@example.com",
  "password": "********",
  "fullName": "Student 01",
  "roleId": 3
}
```

Password phải được hash ở Backend trước khi lưu.

---

## 11.4. Update User

```http
PATCH /api/users/{id}
```

Roles:

```text
ADMIN
```

---

## 11.5. Change User Status

```http
PATCH /api/users/{id}/status
```

Roles:

```text
ADMIN
```

Request:

```json
{
  "status": "INACTIVE"
}
```

Các trạng thái V1:

```text
ACTIVE
INACTIVE
LOCKED
```

---

# 12. Team API

Resource:

```text
/api/teams
```

## 12.1. List Teams

```http
GET /api/teams
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 12.2. Get Team

```http
GET /api/teams/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 12.3. Create Team

```http
POST /api/teams
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "name": "Blue Team 01",
  "description": "Defensive training team"
}
```

---

## 12.4. Update Team

```http
PATCH /api/teams/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 12.5. List Team Members

```http
GET /api/teams/{id}/members
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 12.6. Add Team Member

```http
POST /api/teams/{id}/members
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "userId": 10
}
```

---

## 12.7. Remove Team Member

```http
DELETE /api/teams/{id}/members/{userId}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

# 13. Scenario API

Scenario là template có thể được sử dụng để tạo nhiều Exercise.

Resource:

```text
/api/scenarios
```

## 13.1. List Scenarios

```http
GET /api/scenarios
```

Authenticated users.

---

## 13.2. Get Scenario

```http
GET /api/scenarios/{id}
```

Authenticated users có quyền truy cập Scenario.

---

## 13.3. Create Scenario

```http
POST /api/scenarios
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "name": "SSH Brute Force",
  "description": "Scenario for defensive detection training",
  "type": "SSH_BRUTE_FORCE",
  "difficulty": "MEDIUM"
}
```

---

## 13.4. Update Scenario

```http
PATCH /api/scenarios/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 13.5. Publish Scenario

```http
POST /api/scenarios/{id}/publish
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Scenario chỉ nên được sử dụng để tạo Exercise khi ở trạng thái phù hợp.

---

# 14. Defensive Task API

Resource:

```text
/api/tasks
```

Task thuộc về một Scenario.

## 14.1. List Tasks of Scenario

```http
GET /api/scenarios/{id}/tasks
```

Authenticated users có quyền truy cập Scenario.

---

## 14.2. Create Task

```http
POST /api/scenarios/{id}/tasks
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "name": "Detect Brute Force",
  "description": "Detect SSH brute force activity",
  "taskType": "DETECTION",
  "sequenceOrder": 2,
  "maxScore": 10,
  "required": true
}
```

---

## 14.3. Update Task

```http
PATCH /api/tasks/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

# 15. Assessment Criteria API

Resource:

```text
/api/criteria
```

Criteria định nghĩa điều kiện để đánh giá Task.

## 15.1. List Criteria

```http
GET /api/tasks/{id}/criteria
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 15.2. Create Criterion

```http
POST /api/tasks/{id}/criteria
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "name": "Detect Brute Force",
  "description": "Blue Team identifies SSH brute force activity",
  "criterionType": "EVENT_MATCH",
  "maxScore": 10
}
```

---

## 15.3. Update Criterion

```http
PATCH /api/criteria/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

# 16. Cyber Range API

Resource:

```text
/api/cyber-ranges
```

## 16.1. List Cyber Ranges

```http
GET /api/cyber-ranges
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 16.2. Get Cyber Range

```http
GET /api/cyber-ranges/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
```

White chỉ được xem Cyber Range được gán cho Exercise của mình.

---

## 16.3. Create Cyber Range

```http
POST /api/cyber-ranges
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

## 16.4. Update Cyber Range

```http
PATCH /api/cyber-ranges/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

# 17. Asset API

Asset đại diện cho máy hoặc hệ thống trong Cyber Range.

Ví dụ:

```text
Linux Server
Windows Server
Web Server
Database Server
```

Resource:

```text
/api/assets
```

## 17.1. List Assets

```http
GET /api/cyber-ranges/{id}/assets
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
```

---

## 17.2. Create Asset

```http
POST /api/cyber-ranges/{id}/assets
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "name": "Linux Server 01",
  "assetType": "LINUX_SERVER",
  "hostname": "linux01",
  "ipAddress": "192.168.10.10",
  "os": "Ubuntu",
  "status": "RUNNING"
}
```

---

## 17.3. Get Asset

```http
GET /api/assets/{id}
```

Authenticated users có quyền truy cập Exercise/Cyber Range tương ứng.

---

## 17.4. Update Asset

```http
PATCH /api/assets/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

# 18. Service API

Service thuộc về Asset.

Ví dụ:

```text
SSH : 22
HTTP : 80
HTTPS : 443
```

## 18.1. List Services

```http
GET /api/assets/{id}/services
```

---

## 18.2. Create Service

```http
POST /api/assets/{id}/services
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "name": "SSH",
  "serviceType": "SSH",
  "port": 22,
  "protocol": "TCP",
  "status": "RUNNING"
}
```

---

## 18.3. Update Service

```http
PATCH /api/services/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
```

---

# 19. Exercise API

Exercise là một lần thực thi Scenario.

```text
Scenario
    |
    | create
    v
Exercise
```

Resource:

```text
/api/exercises
```

## 19.1. List Exercises

```http
GET /api/exercises
```

Kết quả phải được lọc theo quyền truy cập của User.

---

## 19.2. Get Exercise

```http
GET /api/exercises/{id}
```

User chỉ được xem Exercise mà mình có quyền truy cập.

---

## 19.3. Create Exercise

```http
POST /api/exercises
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "scenarioId": 1,
  "cyberRangeId": 1,
  "name": "SSH Brute Force Exercise 001"
}
```

Backend tạo một Exercise mới dựa trên Scenario và Cyber Range.

---

## 19.4. Update Exercise

```http
PATCH /api/exercises/{id}
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
```

Authorization phải kiểm tra White Team có được phân công cho Exercise hay không.

---

# 20. Exercise Participant API

Participant liên kết:

```text
Exercise
User
Team
Exercise Role
```

Exercise Role:

```text
RED
BLUE
WHITE
```

## 20.1. List Participants

```http
GET /api/exercises/{id}/participants
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
```

---

## 20.2. Add Participant

```http
POST /api/exercises/{id}/participants
```

Roles:

```text
ADMIN
INSTRUCTOR
```

Request:

```json
{
  "userId": 10,
  "teamId": 2,
  "role": "BLUE"
}
```

Backend phải kiểm tra:

* User tồn tại.
* Team tồn tại nếu được chỉ định.
* Exercise tồn tại.
* User chưa có participant record không hợp lệ trong Exercise.
* Exercise Role hợp lệ.

---

## 20.3. Remove Participant

```http
DELETE /api/exercises/{id}/participants/{participantId}
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
```

White chỉ được thực hiện trong Exercise mà mình được phân công.

---

# 21. Exercise Control API

Exercise Control phục vụ White Team.

## 21.1. Start

```http
POST /api/exercises/{id}/start
```

Roles:

```text
INSTRUCTOR
WHITE
```

Điều kiện:

```text
Exercise = READY
```

---

## 21.2. Pause

```http
POST /api/exercises/{id}/pause
```

Roles:

```text
INSTRUCTOR
WHITE
```

Điều kiện:

```text
Exercise = RUNNING
```

---

## 21.3. Resume

```http
POST /api/exercises/{id}/resume
```

Roles:

```text
INSTRUCTOR
WHITE
```

Điều kiện:

```text
Exercise = PAUSED
```

---

## 21.4. Stop

```http
POST /api/exercises/{id}/stop
```

Roles:

```text
INSTRUCTOR
WHITE
```

---

## 21.5. Reset

```http
POST /api/exercises/{id}/reset
```

Roles:

```text
INSTRUCTOR
WHITE
```

Reset phải được kiểm soát chặt vì có thể ảnh hưởng đến trạng thái Cyber Range.

---

# 22. Exercise State

V1 sử dụng các trạng thái:

```text
CREATED
READY
RUNNING
PAUSED
STOPPED
COMPLETED
```

Việc chuyển trạng thái phải được thực hiện ở Backend.

Client không được tự gửi:

```json
{
  "status": "COMPLETED"
}
```

để thay đổi trạng thái Exercise nếu không thông qua business operation tương ứng.

---

# 23. Event API

Event là dữ liệu quan sát được từ Cyber Range.

Ví dụ:

```text
Authentication Failure
Port Scan Event
Web Security Event
Service State Change
Host State Change
```

## 23.1. Ingest Event

```http
POST /api/events
```

Endpoint dành cho Event Collector hoặc trusted service.

Không dành cho Student.

Request:

```json
{
  "exerciseId": 1,
  "assetId": 2,
  "serviceId": 5,
  "eventType": "AUTH_FAILURE",
  "severity": "HIGH",
  "timestamp": "2026-10-03T13:30:00Z",
  "source": "SSH",
  "data": {}
}
```

Backend phải kiểm tra:

* Exercise tồn tại.
* Asset thuộc Cyber Range của Exercise.
* Service thuộc Asset.
* Event có timestamp hợp lệ.
* Collector/service có authentication hợp lệ.

---

## 23.2. List Exercise Events

```http
GET /api/exercises/{id}/events
```

Roles:

```text
INSTRUCTOR
WHITE
```

Blue có thể được cấp quyền xem các event cần thiết cho nhiệm vụ của mình theo Scenario.

Red không mặc định được xem toàn bộ event của hệ thống.

---

## 23.3. Get Event

```http
GET /api/exercises/{id}/events/{eventId}
```

Authorization phải kiểm tra Event thuộc Exercise tương ứng.

---

# 24. Evidence API

Evidence là bằng chứng được hệ thống sử dụng để đánh giá Task.

Quan hệ:

```text
Event
  |
  v
Evidence
  |
  v
Assessment
```

## 24.1. List Evidence

```http
GET /api/exercises/{id}/evidence
```

Roles:

```text
INSTRUCTOR
WHITE
```

Blue chỉ được xem Evidence mà thiết kế Scenario cho phép.

---

## 24.2. Get Evidence

```http
GET /api/exercises/{id}/evidence/{evidenceId}
```

Backend phải kiểm tra:

```text
Evidence.exercise_id == Exercise.id
```

Evidence không được tạo tùy ý bởi Student.

---

# 25. Assessment API

Assessment Engine xác định kết quả của các Defensive Task dựa trên Evidence.

Luồng:

```text
Event
  |
  v
Evidence
  |
  v
Assessment Engine
  |
  v
Assessment Result
```

## 25.1. Get Exercise Assessment

```http
GET /api/exercises/{id}/assessment
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
BLUE
```

Dữ liệu trả về phải được giới hạn theo quyền truy cập.

---

## 25.2. Get Assessment Results

```http
GET /api/exercises/{id}/assessment/results
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
BLUE
```

---

## 25.3. Get Assessment Result

```http
GET /api/exercises/{id}/assessment/results/{resultId}
```

---

## 25.4. Run Assessment

```http
POST /api/exercises/{id}/assessment/run
```

Roles:

```text
INSTRUCTOR
WHITE
```

Endpoint này yêu cầu Backend chạy Assessment Engine.

Client không gửi kết quả đánh giá.

---

# 26. Assessment Result

Assessment Result đại diện cho kết quả của một Defensive Task.

Ví dụ:

```json
{
  "data": {
    "id": 15,
    "taskId": 2,
    "taskName": "Detect Brute Force",
    "status": "COMPLETED",
    "detectedAt": "2026-10-03T13:35:10Z",
    "respondedAt": null,
    "completedAt": "2026-10-03T13:35:10Z",
    "detectionTime": 310,
    "responseTime": null,
    "reason": "Matching security events were detected"
  }
}
```

Các trạng thái V1:

```text
PENDING
IN_PROGRESS
COMPLETED
FAILED
NOT_COMPLETED
```

---

# 27. Scoring API

Scoring Engine tính điểm dựa trên Assessment Result và Assessment Criteria.

Luồng:

```text
Assessment Result
       |
       v
Assessment Criteria
       |
       v
Scoring Engine
       |
       v
Assessment Score
```

## 27.1. Get Exercise Score

```http
GET /api/exercises/{id}/score
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
BLUE
```

---

## 27.2. Get Score Details

```http
GET /api/exercises/{id}/score/details
```

Response:

```json
{
  "data": {
    "totalScore": 85,
    "maxScore": 100,
    "details": [
      {
        "criterionId": 1,
        "name": "Detect Port Scan",
        "awardedScore": 10,
        "maxScore": 10,
        "reason": "Criterion satisfied"
      }
    ]
  }
}
```

Không có endpoint cho Client tự gửi điểm.

---

# 28. V1 Scoring Criteria

Các tiêu chí ban đầu:

| Criterion                 | Max Score |
| ------------------------- | --------: |
| Detect port scan          |        10 |
| Detect brute force        |        10 |
| Detect web attack         |        15 |
| Identify compromised host |        15 |
| Isolate attacked host     |        20 |
| Recover service           |        20 |
| Incident report           |        10 |
| **Total**                 |   **100** |

Scoring Engine chịu trách nhiệm tính điểm.

Client chỉ đọc kết quả.

---

# 29. Incident Report API

Blue Team sử dụng API này để gửi báo cáo sự cố.

## 29.1. Create Incident Report

```http
POST /api/exercises/{id}/incident-reports
```

Exercise Role:

```text
BLUE
```

Request:

```json
{
  "title": "SSH Brute Force Incident",
  "description": "Multiple authentication failures were detected.",
  "severity": "HIGH"
}
```

---

## 29.2. List Incident Reports

```http
GET /api/exercises/{id}/incident-reports
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
BLUE
```

---

## 29.3. Get Incident Report

```http
GET /api/incident-reports/{id}
```

Authorization phải kiểm tra Exercise ownership/access.

---

## 29.4. Update Incident Report

```http
PATCH /api/incident-reports/{id}
```

Exercise Role:

```text
BLUE
```

Chỉ người gửi hoặc Blue Team participant có quyền tương ứng mới được chỉnh sửa trong thời gian cho phép.

---

# 30. Incident Report Review API

White Team hoặc Instructor review Incident Report.

```http
POST /api/incident-reports/{id}/review
```

Roles:

```text
INSTRUCTOR
WHITE
```

Request:

```json
{
  "status": "REVIEWED",
  "reviewComment": "Incident report reviewed."
}
```

Review phải được ghi nhận trong:

```text
incident_reports.reviewed_by
incident_reports.reviewed_at
incident_reports.review_comment
```

---

# 31. Report API

Report là dữ liệu tổng hợp từ nhiều thành phần:

```text
Exercise
+
Participants
+
Assessment
+
Assessment Results
+
Scores
+
Metrics
+
Incident Reports
```

## 31.1. Get Exercise Report

```http
GET /api/exercises/{id}/report
```

Roles:

```text
ADMIN
INSTRUCTOR
WHITE
BLUE
```

Response mẫu:

```json
{
  "data": {
    "exercise": {
      "id": 1,
      "name": "SSH Brute Force Exercise 001",
      "status": "COMPLETED"
    },
    "score": {
      "total": 85,
      "max": 100
    },
    "metrics": {
      "detectionTime": 310,
      "responseTime": 540
    },
    "tasks": [],
    "incidentReports": []
  }
}
```

---

# 32. Audit Log API

Audit Log ghi nhận các hành động quan trọng.

## 32.1. List Audit Logs

```http
GET /api/audit-logs
```

Roles:

```text
ADMIN
```

Có thể hỗ trợ filter:

```text
userId
action
entityType
entityId
from
to
```

---

## 32.2. Get Audit Log

```http
GET /api/audit-logs/{id}
```

Roles:

```text
ADMIN
```

---

# 33. Authorization Matrix

Ma trận dưới đây mô tả quyền ở mức API/business operation. Resource-level authorization vẫn phải được kiểm tra ở Backend.

| Resource / Operation   |  ADMIN  | INSTRUCTOR | STUDENT | RED |   BLUE  | WHITE |
| ---------------------- | :-----: | :--------: | :-----: | :-: | :-----: | :---: |
| Login                  |    ✓    |      ✓     |    ✓    |  -  |    -    |   -   |
| View own profile       |    ✓    |      ✓     |    ✓    |  -  |    -    |   -   |
| Manage users           |    ✓    |      -     |    -    |  -  |    -    |   -   |
| Manage teams           |    ✓    |      ✓     |    -    |  -  |    -    |   -   |
| Manage scenarios       |    ✓    |      ✓     |    -    |  -  |    -    |   -   |
| Manage tasks           |    ✓    |      ✓     |    -    |  -  |    -    |   -   |
| Manage criteria        |    ✓    |      ✓     |    -    |  -  |    -    |   -   |
| Manage Cyber Range     |    ✓    |      ✓     |    -    |  -  |    -    |   -   |
| Create Exercise        |    ✓    |      ✓     |    -    |  -  |    -    |   -   |
| View assigned Exercise |    ✓    |      ✓     |    ✓    |  ✓  |    ✓    |   ✓   |
| Manage participants    |    ✓    |      ✓     |    -    |  -  |    -    |   ✓*  |
| Start Exercise         |    ✓    |      ✓     |    -    |  -  |    -    |   ✓   |
| Pause Exercise         |    ✓    |      ✓     |    -    |  -  |    -    |   ✓   |
| Resume Exercise        |    ✓    |      ✓     |    -    |  -  |    -    |   ✓   |
| Stop Exercise          |    ✓    |      ✓     |    -    |  -  |    -    |   ✓   |
| Reset Exercise         |    ✓    |      ✓     |    -    |  -  |    -    |   ✓   |
| Ingest Event           | Service |   Service  |    -    |  -  |    -    |   -   |
| View Exercise Events   |    ✓    |      ✓     |    -    |  -  | Limited |   ✓   |
| View Evidence          |    ✓    |      ✓     |    -    |  -  | Limited |   ✓   |
| View Assessment        |    ✓    |      ✓     |    -    |  -  |    ✓    |   ✓   |
| Run Assessment         |    ✓    |      ✓     |    -    |  -  |    -    |   ✓   |
| View Score             |    ✓    |      ✓     |    ✓    |  ✓  |    ✓    |   ✓   |
| Submit Incident Report |    -    |      -     |    -    |  -  |    ✓    |   -   |
| Review Incident Report |    ✓    |      ✓     |    -    |  -  |    -    |   ✓   |
| View Final Report      |    ✓    |      ✓     |    ✓    |  ✓  |    ✓    |   ✓   |
| View Audit Logs        |    ✓    |   Limited  |    -    |  -  |    -    |   -   |

`✓*` = chỉ khi White Team được phân công và Exercise Policy cho phép.

Các quyền `Limited` phải được triển khai bằng resource-level authorization, không chỉ dựa trên System Role.

---

# 34. Resource Isolation

Backend phải bảo đảm User không thể truy cập resource của Exercise khác chỉ bằng cách thay đổi ID trên URL.

Ví dụ:

```http
GET /api/exercises/100
```

Không có nghĩa là User có quyền xem Exercise `100`.

Backend phải kiểm tra:

```text
Current User
      |
      v
Exercise Participant
      |
      v
Exercise #100
```

Tương tự đối với:

```text
/events
/evidence
/assessment
/score
/incident-reports
/report
```

---

# 35. Business Rules

## 35.1. Scenario

Scenario là template.

Không lưu kết quả của một lần thực hành trực tiếp trong Scenario.

```text
Scenario != Exercise
```

---

## 35.2. Exercise

Exercise là một lần chạy cụ thể của Scenario.

Exercise phải tham chiếu:

```text
scenario_id
cyber_range_id
```

---

## 35.3. Participant

Mỗi User tham gia Exercise phải có Exercise Role:

```text
RED
BLUE
WHITE
```

System Role và Exercise Role không thay thế cho nhau.

---

## 35.4. Event

Event là dữ liệu quan sát được từ Cyber Range.

Client không được tự gửi Event với tư cách Student.

---

## 35.5. Evidence

Evidence được sử dụng làm cơ sở cho Assessment.

Client không được tự tạo Evidence để nhận điểm.

---

## 35.6. Assessment

Assessment Engine xác định Task có hoàn thành hay không.

Client không được tự gửi:

```text
COMPLETED
```

để thay đổi Assessment Result.

---

## 35.7. Score

Score được tính ở Backend.

Client không được gửi:

```json
{
  "totalScore": 100
}
```

để cập nhật điểm.

---

## 35.8. Report

Report được tổng hợp từ dữ liệu hệ thống.

Không tạo một endpoint cho Client tự gửi toàn bộ kết quả cuối cùng.

---

# 36. Error Handling

Các lỗi nghiệp vụ phải trả về HTTP status phù hợp.

Ví dụ:

### Exercise không tồn tại

```http
404 Not Found
```

### User không tham gia Exercise

```http
403 Forbidden
```

### Exercise đang PAUSED nhưng gọi RESUME không hợp lệ

```http
409 Conflict
```

### Exercise đang CREATED nhưng gọi STOP

```http
409 Conflict
```

### Request thiếu trường bắt buộc

```http
400 Bad Request
```

### JWT không hợp lệ

```http
401 Unauthorized
```

---

# 37. Pagination

Các endpoint trả về danh sách lớn nên hỗ trợ:

```text
page
size
sort
```

Ví dụ:

```http
GET /api/exercises?page=0&size=20
```

Response:

```json
{
  "data": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

# 38. Filtering

Các API list có thể hỗ trợ filter tùy resource.

Ví dụ:

```http
GET /api/exercises?status=RUNNING
```

```http
GET /api/events?severity=HIGH
```

```http
GET /api/audit-logs?action=LOGIN
```

Filter cụ thể sẽ được triển khai theo từng module.

---

# 39. API Security Requirements

API phải tuân thủ các nguyên tắc trong `04-security.md`.

## Authentication

```text
JWT Bearer Token
```

## Password

```text
Password
   ↓
Hash
   ↓
Database
```

Không lưu plaintext password.

## Authorization

Kiểm tra:

```text
System Role
+
Exercise Role
+
Resource Ownership
+
Exercise Isolation
```

## Validation

Request DTO phải sử dụng Bean Validation khi phù hợp.

Ví dụ:

java
@NotBlank
@Email
@Size


## Audit

Các thao tác nhạy cảm phải tạo Audit Log.

## HTTPS

Production sử dụng HTTPS.

## CORS

Production chỉ cho phép origin được cấu hình.

---

# 40. API Module Mapping

API được ánh xạ với các Backend module:

```text
src/main/java/com/cyberrange/backend/

├── auth/
│   └── /api/auth

├── user/
│   └── /api/users

├── team/
│   └── /api/teams

├── scenario/
│   ├── /api/scenarios
│   ├── /api/tasks
│   └── /api/criteria

├── lab/
│   ├── /api/cyber-ranges
│   ├── /api/assets
│   └── /api/services

├── exercise/
│   └── /api/exercises

├── event/
│   ├── /api/events
│   └── /api/evidence

├── assessment/
│   └── /api/exercises/{id}/assessment

├── scoring/
│   └── /api/exercises/{id}/score

├── report/
│   ├── /api/exercises/{id}/report
│   └── /api/exercises/{id}/incident-reports

└── audit/
    └── /api/audit-logs
```

---

# 41. Complete API Endpoint Summary

```text
AUTH
POST   /api/auth/login
GET    /api/auth/me

USERS
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PATCH  /api/users/{id}
PATCH  /api/users/{id}/status

TEAMS
GET    /api/teams
GET    /api/teams/{id}
POST   /api/teams
PATCH  /api/teams/{id}
GET    /api/teams/{id}/members
POST   /api/teams/{id}/members
DELETE /api/teams/{id}/members/{userId}

SCENARIOS
GET    /api/scenarios
GET    /api/scenarios/{id}
POST   /api/scenarios
PATCH  /api/scenarios/{id}
POST   /api/scenarios/{id}/publish
GET    /api/scenarios/{id}/tasks

TASKS
POST   /api/scenarios/{id}/tasks
PATCH  /api/tasks/{id}

CRITERIA
GET    /api/tasks/{id}/criteria
POST   /api/tasks/{id}/criteria
PATCH  /api/criteria/{id}

CYBER RANGES
GET    /api/cyber-ranges
GET    /api/cyber-ranges/{id}
POST   /api/cyber-ranges
PATCH  /api/cyber-ranges/{id}

ASSETS
GET    /api/cyber-ranges/{id}/assets
POST   /api/cyber-ranges/{id}/assets
GET    /api/assets/{id}
PATCH  /api/assets/{id}

SERVICES
GET    /api/assets/{id}/services
POST   /api/assets/{id}/services
PATCH  /api/services/{id}

EXERCISES
GET    /api/exercises
GET    /api/exercises/{id}
POST   /api/exercises
PATCH  /api/exercises/{id}

PARTICIPANTS
GET    /api/exercises/{id}/participants
POST   /api/exercises/{id}/participants
DELETE /api/exercises/{id}/participants/{participantId}

EXERCISE CONTROL
POST   /api/exercises/{id}/start
POST   /api/exercises/{id}/pause
POST   /api/exercises/{id}/resume
POST   /api/exercises/{id}/stop
POST   /api/exercises/{id}/reset

EVENTS
POST   /api/events
GET    /api/exercises/{id}/events
GET    /api/exercises/{id}/events/{eventId}

EVIDENCE
GET    /api/exercises/{id}/evidence
GET    /api/exercises/{id}/evidence/{evidenceId}

ASSESSMENT
GET    /api/exercises/{id}/assessment
GET    /api/exercises/{id}/assessment/results
GET    /api/exercises/{id}/assessment/results/{resultId}
POST   /api/exercises/{id}/assessment/run

SCORING
GET    /api/exercises/{id}/score
GET    /api/exercises/{id}/score/details

INCIDENT REPORT
POST   /api/exercises/{id}/incident-reports
GET    /api/exercises/{id}/incident-reports
GET    /api/incident-reports/{id}
PATCH  /api/incident-reports/{id}
POST   /api/incident-reports/{id}/review

REPORT
GET    /api/exercises/{id}/report

AUDIT
GET    /api/audit-logs
GET    /api/audit-logs/{id}
```

---

# 42. End-to-End Exercise Flow

Luồng API hoàn chỉnh của một Exercise:

```text
                 INSTRUCTOR
                     |
                     | Create Scenario
                     v
                SCENARIO
                     |
                     | Create Exercise
                     v
                EXERCISE
                     |
          +----------+----------+
          |          |          |
         RED        BLUE       WHITE
          |          |          |
       Attack      Defend     Control
          |          |          |
          +----------+----------+
                     |
                     v
                  EVENT
                     |
                     v
                 EVIDENCE
                     |
                     v
            ASSESSMENT ENGINE
                     |
                     v
             ASSESSMENT RESULT
                     |
                     v
              SCORING ENGINE
                     |
                     v
               SCORE / 100
                     |
                     v
             INCIDENT REPORT
                     |
                     v
                WHITE REVIEW
                     |
                     v
                 REPORT
```

---

# 43. V1 API Scope

API V1 tập trung vào các chức năng cần thiết cho đề tài:

* Authentication.
* User Management.
* Team Management.
* Scenario Management.
* Defensive Task Management.
* Assessment Criteria Management.
* Cyber Range Management.
* Asset và Service Management.
* Exercise Management.
* Red/Blue/White Exercise Participants.
* Exercise Control.
* Event Collection.
* Evidence.
* Automated Assessment.
* Automated Scoring.
* Incident Report.
* Exercise Report.
* Audit Log.

Các chức năng sau chưa bắt buộc trong V1:

```text
OAuth2 / SSO
MFA
Refresh Token nâng cao
Privilege Escalation Scenario
Persistence Scenario
Advanced Windows / Active Directory API
AI/ML Assessment
Automated Incident Response
Real-time WebSocket API
Multi-Cyber-Range Orchestration
```

Các chức năng trên có thể được bổ sung ở các phiên bản sau mà không thay đổi nguyên tắc API cốt lõi.

---

# 44. API Design Principles

API V1 tuân thủ các nguyên tắc:

1. REST-oriented.
2. Resource-oriented endpoint.
3. Business operation dùng HTTP POST khi làm thay đổi state.
4. Authentication bằng JWT.
5. Authorization server-side.
6. System Role và Exercise Role tách biệt.
7. Exercise isolation.
8. Assessment và Score do server kiểm soát.
9. Event Collector được authentication riêng.
10. Không để Client tự quyết định kết quả đánh giá.
11. Không để Client tự quyết định điểm.
12. Response và Error format thống nhất.
13. Validation ở Backend.
14. Sensitive operations được audit.
15. API phải tương thích với Database Design và Security Design.
16. API được thiết kế để mở rộng thêm Scenario, Task và Assessment Criteria.

---

# 45. Final API Architecture

```text
                           CLIENT
                             |
                             | HTTPS
                             v
                    ┌─────────────────┐
                    │   REST API      │
                    │ Spring Boot     │
                    └────────┬────────┘
                             |
             ┌───────────────┴───────────────┐
             |                               |
       Authentication                  Authorization
             |                               |
           JWT                    System Role + Exercise Role
             |                               |
             └───────────────┬───────────────┘
                             |
        ┌────────────────────┼────────────────────┐
        |                    |                    |
    MANAGEMENT           EXERCISE             REPORTING
        |                    |                    |
    User/Team           Exercise Control      Assessment
    Scenario             Participants          Score
    Cyber Range           Events               Report
    Asset/Service         Evidence             Audit
        |                    |
        └────────────────────┼────────────────────┘
                             |
                             v
                       BUSINESS LOGIC
                             |
             ┌───────────────┼───────────────┐
             |               |               |
       Assessment       Scoring          Validation
         Engine          Engine
             |               |
             └───────┬───────┘
                     |
                     v
                 PostgreSQL
```

**API Contract V1 được xem là lớp giao tiếp chính giữa Frontend, Backend và Event Collector. Mọi implementation sau này phải tuân thủ contract này; nếu cần thay đổi API phải cập nhật tài liệu trước khi triển khai.**
