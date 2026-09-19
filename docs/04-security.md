# Security Design

## 1. Overview

Security layer của Cyber Range Backend chịu trách nhiệm:

* Authentication — xác thực người dùng.
* Authorization — kiểm soát quyền truy cập.
* System Role — phân quyền ở cấp hệ thống.
* Exercise Role — phân vai trong từng Exercise.
* Password Security — bảo vệ thông tin xác thực.
* JWT-based API authentication.
* Resource-level authorization.
* Exercise isolation.
* Audit logging.
* Input validation.
* Protection of sensitive operations.

Security được thiết kế theo nguyên tắc:

```text
Authentication
      ↓
System Authorization
      ↓
Exercise Authorization
      ↓
Resource Authorization
      ↓
Business Logic
```

Không được coi việc đăng nhập thành công là đủ để cho phép người dùng thực hiện mọi thao tác.

---

# 2. Security Model

Hệ thống sử dụng hai lớp Role:

```text
                    USER
                     │
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
   SYSTEM ROLE             EXERCISE ROLE
          │                     │
    ADMIN/INSTRUCTOR/      RED/BLUE/WHITE
       STUDENT
```

Hai loại Role phục vụ hai mục đích khác nhau.

---

# 3. System Role

System Role xác định quyền của User đối với toàn bộ Web Platform.

V1 sử dụng ba System Role:

```text
ADMIN
INSTRUCTOR
STUDENT
```

## 3.1. ADMIN

ADMIN quản lý hệ thống ở cấp platform.

Các quyền chính:

* Quản lý User.
* Quản lý System Role.
* Quản lý Team.
* Quản lý Cyber Range.
* Quản lý Scenario.
* Quản lý cấu hình hệ thống.
* Xem Audit Log.
* Quản lý các tài nguyên hệ thống.

ADMIN không mặc nhiên được coi là RED, BLUE hoặc WHITE trong một Exercise.

Nếu ADMIN tham gia một Exercise, Exercise Role phải được xác định riêng.

Ví dụ:

```text
User:
    system_role = ADMIN

Exercise #001:
    exercise_role = WHITE
```

---

# 4. INSTRUCTOR

INSTRUCTOR quản lý hoạt động đào tạo và đánh giá.

Các quyền chính:

* Tạo Scenario.
* Chỉnh sửa Scenario.
* Publish Scenario.
* Tạo Exercise.
* Cấu hình Exercise.
* Phân công người tham gia.
* Gán RED / BLUE / WHITE.
* Theo dõi Exercise.
* Xem kết quả Assessment.
* Xem Score.
* Review Incident Report.
* Xem Report.
* Quản lý các nội dung phục vụ đào tạo.

INSTRUCTOR không mặc nhiên có quyền ADMIN.

Ví dụ:

```text
INSTRUCTOR
    ├── Scenario Management
    ├── Exercise Management
    ├── Participant Management
    ├── Assessment Review
    └── Report Review
```

---

# 5. STUDENT

STUDENT là người tham gia các hoạt động thực hành.

Quyền của STUDENT phụ thuộc vào Exercise mà User tham gia.

Một Student có thể có Exercise Role khác nhau ở các Exercise khác nhau.

Ví dụ:

```text
Exercise #001
Student A → RED

Exercise #002
Student A → BLUE

Exercise #003
Student A → WHITE
```

System Role không thay đổi.

Exercise Role thay đổi theo Exercise.

---

# 6. Exercise Role

Exercise Role được lưu tại:

```text
exercise_participants.role
```

Các giá trị V1:

```text
RED
BLUE
WHITE
```

Không lưu các giá trị này trong `roles`.

---

# 7. RED Team Permissions

RED Team đại diện cho bên thực hiện hoạt động tấn công trong Cyber Range.

Ở mức hệ thống Web Platform, RED Team có thể:

* Xem Exercise mà mình được phân công.
* Xem các thông tin Scenario được phép cung cấp.
* Xem trạng thái Exercise theo quyền được cấp.
* Thực hiện các hoạt động Red Team nằm trong phạm vi Exercise.
* Xem trạng thái hoạt động của chính mình nếu hệ thống hỗ trợ.

RED Team không được:

* Thay đổi Assessment.
* Thay đổi Score.
* Review Incident Report.
* Thay đổi kết quả của Blue Team.
* Điều khiển Exercise nếu không được cấp White Team/Instructor permission.
* Truy cập dữ liệu nội bộ của Assessment Engine ngoài phạm vi được cho phép.

---

# 8. BLUE Team Permissions

BLUE Team đại diện cho bên phòng thủ.

Blue Team có thể:

* Xem Exercise được phân công.
* Xem các thông tin cần thiết cho hoạt động phòng thủ.
* Phân tích các Event/Log được cung cấp.
* Thực hiện defensive tasks.
* Xác định Incident.
* Thực hiện các hoạt động phòng thủ được hệ thống hỗ trợ.
* Submit Incident Report.
* Xem trạng thái Task của mình.

Blue Team không được:

* Thay đổi Assessment Result.
* Tự thay đổi Score.
* Review hoặc approve kết quả của chính mình.
* Thay đổi Scenario.
* Thay đổi Participant Role.
* Điều khiển Exercise nếu không có quyền White Team/Instructor.

---

# 9. WHITE Team Permissions

White Team là Exercise Administration / Control / Monitoring / Review role.

White Team không phải một loại System Role riêng.

White Team có thể:

* Theo dõi Exercise.
* Theo dõi trạng thái Cyber Range.
* Theo dõi trạng thái Asset/Service.
* Start Exercise.
* Pause Exercise.
* Resume Exercise.
* Stop Exercise.
* Reset Exercise nếu được cấp quyền.
* Theo dõi Event/Timeline.
* Review Incident Report.
* Review Assessment.
* Theo dõi kết quả Scoring.
* Thực hiện các hoạt động điều hành Exercise được hệ thống hỗ trợ.

White Team không nên được mặc định có:

* Quyền quản trị User.
* Quyền quản lý toàn bộ System.
* Quyền thay đổi System Role.

Các quyền này thuộc System Role như ADMIN hoặc INSTRUCTOR.

---

# 10. System Role vs Exercise Role

Đây là nguyên tắc quan trọng của hệ thống.

Không được thiết kế:

```text
ADMIN
RED_TEAM
BLUE_TEAM
WHITE_TEAM
```

thành một hệ thống Role duy nhất.

Thay vào đó:

```text
System Role
├── ADMIN
├── INSTRUCTOR
└── STUDENT

Exercise Role
├── RED
├── BLUE
└── WHITE
```

Ví dụ:

```text
User A
System Role:
    STUDENT

Exercise #001:
    BLUE

Exercise #002:
    RED
```

User A vẫn là STUDENT ở cấp hệ thống nhưng có vai trò khác nhau trong từng Exercise.

---

# 11. Authentication

Authentication dùng để xác định User.

Luồng đăng nhập:

```text
Client
   │
   │ username + password
   ▼
POST /api/auth/login
   │
   ▼
Authentication Service
   │
   ├── Find User
   ├── Verify password
   ├── Check account status
   │
   ▼
JWT Token
   │
   ▼
Client
```

Các API cần authentication sẽ yêu cầu JWT.

---

# 12. Password Security

Password không được lưu dưới dạng plaintext.

Database chỉ lưu:

```text
users.password_hash
```

Không lưu:

```text
password
plain_password
```

Password phải được hash bằng password hashing algorithm phù hợp.

Application không được log:

* Password.
* Password hash.
* Authentication secret.
* JWT secret.
* Refresh token nếu hệ thống sử dụng refresh token.

---

# 13. JWT Authentication

V1 sử dụng JWT để xác thực API.

Client gửi token thông qua:

```text
Authorization: Bearer <token>
```

Luồng:

```text
Client
   │
   │ Authorization: Bearer JWT
   ▼
Spring Security
   │
   ├── Validate token
   ├── Extract user identity
   ├── Load authorities
   │
   ▼
Controller
   │
   ▼
Service
```

JWT được sử dụng để xác định:

* User identity.
* System authorities.
* Token validity.

JWT không được sử dụng để thay thế kiểm tra Exercise Role.

---

# 14. JWT Claims

JWT có thể chứa các thông tin tối thiểu:

```json
{
  "sub": "user-id",
  "username": "student01",
  "role": "STUDENT",
  "iat": "...",
  "exp": "..."
}
```

Không nên đưa dữ liệu nhạy cảm hoặc dữ liệu lớn vào JWT.

Exercise Role không nên được coi là quyền cố định trong JWT vì:

```text
User có thể:
    Exercise #001 → BLUE
    Exercise #002 → RED
```

Exercise Role nên được kiểm tra từ database theo:

```text
user_id
+
exercise_id
```

---

# 15. Authorization Layers

Authorization được thực hiện ở nhiều tầng.

```text
Layer 1
Authentication
    ↓
User đã đăng nhập?

Layer 2
System Role
    ↓
User có quyền thực hiện loại operation này?

Layer 3
Exercise Role
    ↓
User có tham gia Exercise này không?
User là RED/BLUE/WHITE?

Layer 4
Resource Authorization
    ↓
User có quyền truy cập resource cụ thể không?

Layer 5
Business Rule
    ↓
Operation có hợp lệ trong trạng thái hiện tại không?
```

Ví dụ:

```text
POST /api/exercises/{id}/stop
```

Không chỉ kiểm tra:

```text
authenticated = true
```

mà cần kiểm tra:

```text
User authenticated
        ↓
User has permission to control exercise
        ↓
User participates in this exercise
        ↓
User is WHITE
        ↓
Exercise status allows STOP
        ↓
STOP operation
```

---

# 16. Resource-Level Authorization

Không được chỉ dựa vào ID trong URL.

Ví dụ:

```text
GET /api/exercises/100
```

Việc User biết `100` không có nghĩa User được phép xem Exercise #100.

Backend phải kiểm tra:

```text
User
  ↓
Exercise #100
  ↓
Is user authorized?
```

Ví dụ:

```text
Student A
    ↓
Exercise #001
    ↓
Participant?
    ↓
YES
    ↓
Allow
```

Nhưng:

```text
Student A
    ↓
Exercise #999
    ↓
Participant?
    ↓
NO
    ↓
Deny
```

---

# 17. Exercise Isolation

Dữ liệu giữa các Exercise phải được phân tách.

Ví dụ:

```text
Exercise #001
    ├── Events
    ├── Evidence
    ├── Assessment
    └── Reports

Exercise #002
    ├── Events
    ├── Evidence
    ├── Assessment
    └── Reports
```

User của Exercise #001 không được mặc định truy cập dữ liệu của Exercise #002.

Các bảng cần đặc biệt kiểm tra Exercise-level authorization:

```text
events
evidence
assessments
assessment_results
assessment_scores
incident_reports
exercise_participants
```

---

# 18. Assessment Security

Assessment là dữ liệu quan trọng vì nó quyết định kết quả đánh giá.

RED và BLUE không được trực tiếp thay đổi:

```text
assessments.total_score
assessment_results.status
assessment_scores.awarded_score
```

Các giá trị này phải được tạo hoặc cập nhật bởi Assessment/Scoring logic.

White Team có thể review kết quả theo quyền được cấp.

INSTRUCTOR có thể review kết quả.

ADMIN có quyền quản trị hệ thống nhưng không nên bypass business logic một cách tùy tiện.

---

# 19. Score Integrity

Score phải được bảo vệ khỏi việc User tự sửa điểm.

Không cho phép client gửi trực tiếp:

```json
{
  "totalScore": 100
}
```

để thay đổi kết quả.

Thay vào đó:

```text
Event / Evidence
       ↓
Assessment Engine
       ↓
Assessment Result
       ↓
Scoring Engine
       ↓
Assessment Score
       ↓
Total Score
```

Server là nguồn tin cậy của Score.

---

# 20. Incident Report Security

Blue Team có quyền tạo Incident Report.

Ví dụ:

```text
BLUE
   ↓
Submit Incident Report
```

Sau khi submit:

```text
Incident Report
    status = SUBMITTED
```

White Team hoặc người có quyền review có thể:

```text
SUBMITTED
    ↓
REVIEWED
```

Blue Team không được tự review hoặc thay đổi trạng thái review của report của chính mình.

---

# 21. Exercise Control Security

Các operation điều khiển Exercise cần được bảo vệ.

Các operation quan trọng:

```text
START
PAUSE
RESUME
STOP
RESET
```

Không phải User nào đăng nhập cũng được thực hiện.

Ví dụ:

```text
                    START
                      │
             ┌────────┴────────┐
             │                 │
          INSTRUCTOR         WHITE
             │                 │
             └────────┬────────┘
                      ▼
                  Exercise
```

Các thao tác này cũng phải được ghi Audit Log.

---

# 22. Audit Logging

Các security-sensitive operations phải được ghi vào:

```text
audit_logs
```

Ví dụ:

```text
LOGIN
LOGIN_FAILED
LOGOUT

CREATE_USER
UPDATE_USER
DISABLE_USER

CREATE_SCENARIO
UPDATE_SCENARIO
PUBLISH_SCENARIO

CREATE_EXERCISE
UPDATE_EXERCISE

START_EXERCISE
PAUSE_EXERCISE
RESUME_EXERCISE
STOP_EXERCISE
RESET_EXERCISE

ASSIGN_PARTICIPANT

REVIEW_REPORT
REVIEW_ASSESSMENT
```

Audit Log nên lưu:

```text
user_id
action
entity_type
entity_id
created_at
metadata
```

---

# 23. Input Validation

Tất cả dữ liệu từ client phải được validate.

Các nhóm cần validate:

* Username.
* Email.
* Password.
* Scenario.
* Exercise.
* Participant.
* Event data.
* Incident Report.
* Assessment data.

Validation phải được thực hiện ở backend.

Client-side validation không được coi là security control.

Spring Boot sẽ sử dụng Bean Validation cho request DTO.

Ví dụ:

```text
@NotBlank
@Email
@Size
@Min
@Max
```

---

# 24. API Security

API phải tuân thủ các nguyên tắc:

* Authentication cho protected endpoints.
* Authorization theo System Role.
* Authorization theo Exercise Role.
* Resource ownership/access check.
* Input validation.
* Không trả về sensitive information.
* Không expose internal stack trace cho client.
* Không expose password hash.
* Không expose authentication secrets.

Response lỗi nên sử dụng format thống nhất.

Ví dụ:

```json
{
  "code": "FORBIDDEN",
  "message": "You do not have permission to access this resource."
}
```

Không trả về thông tin nội bộ như:

```text
SQL query
Java stack trace
database credentials
JWT secret
internal exception details
```

---

# 25. HTTP Security

Production deployment phải sử dụng HTTPS.

```text
Client
   │
 HTTPS
   ▼
Spring Boot
```

Không gửi password hoặc JWT qua HTTP plaintext trong production.

Development có thể chạy HTTP local, nhưng deployment thực tế phải sử dụng TLS.

---

# 26. CORS

CORS phải được cấu hình theo frontend origin được phép.

Không sử dụng cấu hình production kiểu:

```text
Access-Control-Allow-Origin: *
```

cho các API authenticated nếu không cần thiết.

Development có thể cho phép frontend local, ví dụ:

```text
http://localhost:3000
```

Production phải sử dụng danh sách origin cụ thể.

---

# 27. CSRF

V1 sử dụng JWT-based stateless API.

Nếu JWT được gửi qua:

```text
Authorization: Bearer <token>
```

thì API không dựa vào browser session cookie để authentication.

CSRF protection phải được cấu hình phù hợp với cách lưu và gửi token.

Nếu sau này hệ thống chuyển sang authentication bằng cookie/session, CSRF protection phải được xem xét và bật phù hợp.

---

# 28. Session and Token Policy

V1 ưu tiên stateless API authentication bằng JWT.

Không lưu authentication state trên server theo mô hình session cho API chính.

JWT cần có expiration.

Ví dụ concept:

```text
Access Token
    ↓
Short-lived
    ↓
Expiration
```

Nếu sau này cần refresh token:

```text
Access Token
+
Refresh Token
```

thì Refresh Token phải có lifecycle và security policy riêng.

Refresh Token không nên được xử lý giống Access Token.

---

# 29. Account Security

User account có trạng thái:

```text
ACTIVE
INACTIVE
LOCKED
```

Chỉ `ACTIVE` user mới có thể đăng nhập.

Ví dụ:

```text
ACTIVE
   ↓
Authentication allowed

INACTIVE
   ↓
Authentication denied

LOCKED
   ↓
Authentication denied
```

Các thay đổi trạng thái account phải được audit.

---

# 30. Authorization Matrix

## System-level permissions

| Operation           | ADMIN | INSTRUCTOR | STUDENT |
| ------------------- | ----: | ---------: | ------: |
| Manage Users        |     ✓ |          — |       — |
| Manage System Roles |     ✓ |          — |       — |
| Manage Teams        |     ✓ |          ✓ |       — |
| Manage Cyber Range  |     ✓ |          ✓ |       — |
| Create Scenario     |     ✓ |          ✓ |       — |
| Edit Scenario       |     ✓ |          ✓ |       — |
| Publish Scenario    |     ✓ |          ✓ |       — |
| Create Exercise     |     ✓ |          ✓ |       — |
| Configure Exercise  |     ✓ |          ✓ |       — |
| View Own Exercise   |     ✓ |          ✓ |      ✓* |
| View Assessment     |     ✓ |          ✓ |      ✓* |
| Review Assessment   |     ✓ |          ✓ |       — |
| View Audit Logs     |     ✓ |    Limited |       — |

`*` phụ thuộc vào Exercise Participant và Exercise Role.

---

# 31. Exercise-level Permission Matrix

| Operation              |     RED |    BLUE | WHITE |
| ---------------------- | ------: | ------: | ----: |
| View Exercise          |       ✓ |       ✓ |     ✓ |
| View Assigned Scenario |       ✓ |       ✓ |     ✓ |
| Perform Red Activity   |       ✓ |       — |     — |
| Perform Blue Activity  |       — |       ✓ |     — |
| Submit Incident Report |       — |       ✓ |     — |
| View Defensive Tasks   |       — |       ✓ |     ✓ |
| Monitor Exercise       | Limited | Limited |     ✓ |
| Start Exercise         |       — |       — |     ✓ |
| Pause Exercise         |       — |       — |     ✓ |
| Resume Exercise        |       — |       — |     ✓ |
| Stop Exercise          |       — |       — |     ✓ |
| Reset Exercise         |       — |       — |    ✓* |
| Review Incident Report |       — |       — |     ✓ |
| Review Assessment      |       — |       — |     ✓ |
| Modify Score           |       — |       — |   —** |

`*` phụ thuộc vào permission được cấp cho White Team.

`**` Score không nên được sửa trực tiếp bởi Exercise Participant. Nếu cần điều chỉnh kết quả, phải đi qua review/administration workflow được kiểm soát.

---

# 32. Security Boundaries

Hệ thống có các security boundary chính:

```text
Internet / Client
        │
        ▼
Authentication
        │
        ▼
API
        │
        ▼
Authorization
        │
        ├── System Role
        │
        ├── Exercise Role
        │
        └── Resource Access
        │
        ▼
Business Logic
        │
        ├── Assessment Engine
        ├── Scoring Engine
        └── Exercise Control
        │
        ▼
Database
```

Không cho phép client bypass Business Logic để thay đổi dữ liệu quan trọng.

---

# 33. Sensitive Data

Các dữ liệu cần bảo vệ:

```text
Password
Password Hash
JWT Secret
Refresh Token
Database Credentials
API Keys
Infrastructure Credentials
```

Không commit các thông tin này vào Git.

Không đặt secret trực tiếp trong:

```text
application.properties
application.yml
CLAUDE.md
README.md
```

Production secrets phải được cung cấp thông qua environment variables hoặc secret management mechanism.

---

# 34. Environment Configuration

Các thông tin nhạy cảm nên sử dụng environment variables.

Ví dụ:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

JWT_SECRET
JWT_EXPIRATION

CORS_ALLOWED_ORIGINS
```

Repository chỉ chứa:

```text
.env.example
```

và không chứa secret thật.

---

# 35. Database Security

Application database account nên sử dụng principle of least privilege.

Application không nên sử dụng PostgreSQL superuser trong production.

Ví dụ concept:

```text
PostgreSQL
│
├── Admin / Migration User
│
└── Application User
        │
        ├── SELECT
        ├── INSERT
        ├── UPDATE
        └── DELETE
```

Database credentials không được hard-code trong source code.

---

# 36. Logging Security

Application logs không được chứa:

```text
password
password_hash
JWT secret
database password
API secret
```

Đặc biệt cần tránh log toàn bộ Authentication Request.

Ví dụ không nên log:

```text
username=student01
password=123456
```

Có thể log:

```text
LOGIN_FAILED
user=student01
timestamp=...
```

với mức độ thông tin phù hợp.

---

# 37. Error Handling

Backend không trả về internal exception details cho client.

Không nên:

```json
{
  "error": "org.postgresql.util.PSQLException: ..."
}
```

Thay vào đó:

```json
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "An internal error occurred."
}
```

Chi tiết kỹ thuật được ghi trong server-side log với mức độ phù hợp.

---

# 38. Security Rules for Cyber Range Operations

Cyber Range là môi trường thực hành có kiểm soát.

Các thao tác Red Team và Blue Team phải được giới hạn trong:

```text
Assigned Exercise
        ↓
Assigned Cyber Range
        ↓
Authorized Assets
        ↓
Authorized Services
```

Không cho phép một Exercise participant sử dụng quyền của hệ thống để truy cập tùy ý các resource ngoài Exercise.

---

# 39. Security Principles

Security implementation phải tuân thủ các nguyên tắc:

### Least Privilege

User chỉ có quyền cần thiết.

### Defense in Depth

Không phụ thuộc vào một lớp security duy nhất.

### Fail Secure

Khi authorization không xác định được, mặc định từ chối.

### Server-side Enforcement

Security rule phải được kiểm tra ở backend.

### Separation of Duties

Không để cùng một User tự tạo, tự đánh giá và tự phê duyệt kết quả khi workflow yêu cầu độc lập.

### Auditability

Các hành động quan trọng phải có Audit Log.

### Data Isolation

Dữ liệu giữa các Exercise phải được phân tách.

---

# 40. Security Flow Example

Ví dụ Blue Team truy cập Assessment:

```text
Client
  │
  │ GET /api/exercises/001/assessment
  ▼
JWT Authentication
  │
  ▼
Identify User
  │
  ▼
Check System Role
  │
  ▼
Check Exercise Participant
  │
  ▼
Check Exercise Role
  │
  ├── BLUE → allowed according to resource policy
  │
  ├── WHITE → allowed
  │
  └── RED → denied if assessment is restricted
  │
  ▼
Service Layer
  │
  ▼
Assessment Repository
  │
  ▼
Response
```

---

# 41. Security Flow for White Team Exercise Control

Ví dụ White Team Pause Exercise:

```text
Client
  │
  │ POST /api/exercises/001/pause
  ▼
JWT Authentication
  │
  ▼
User Identity
  │
  ▼
Exercise Participant Check
  │
  ▼
Exercise Role = WHITE?
  │
  ├── NO → 403 FORBIDDEN
  │
  └── YES
        │
        ▼
   Exercise State Check
        │
        ▼
   Pause Exercise
        │
        ▼
   Audit Log
        │
        ▼
   Response
```

---

# 42. Security Flow for Incident Report

```text
BLUE User
   │
   ▼
Submit Incident Report
   │
   ▼
Validate Request
   │
   ▼
Check Exercise Membership
   │
   ▼
Check Exercise Role = BLUE
   │
   ▼
Create Report
   │
   ▼
status = SUBMITTED
   │
   ▼
WHITE / INSTRUCTOR
   │
   ▼
Review Report
   │
   ▼
status = REVIEWED
```

---

# 43. Out of Scope for V1

Các security feature sau chưa bắt buộc trong V1:

```text
Multi-Factor Authentication
OAuth2 / OpenID Connect
Single Sign-On
External Identity Provider
Advanced SIEM integration
Hardware security key
Fine-grained policy engine
Attribute-Based Access Control
Advanced secret management platform
```

Kiến trúc phải đủ linh hoạt để bổ sung các tính năng này trong tương lai.

---

# 44. Security Implementation Mapping

Security design sẽ được triển khai ở các module:

```text
src/main/java/com/cyberrange/backend/
│
├── auth/
│   ├── authentication
│   ├── jwt
│   └── security
│
├── user/
├── team/
├── exercise/
├── scenario/
├── assessment/
├── scoring/
└── audit/
```

Các security checks không nên được đặt toàn bộ trong Controller.

Business authorization nên được kiểm tra ở Service/Security layer phù hợp.

---

# 45. Security Checklist

## Authentication

* [ ] Login endpoint implemented.
* [ ] Password hashed.
* [ ] JWT generated.
* [ ] JWT expiration configured.
* [ ] JWT validation implemented.
* [ ] Disabled/locked users cannot authenticate.

## Authorization

* [ ] System Role implemented.
* [ ] Exercise Role implemented.
* [ ] Resource-level authorization implemented.
* [ ] Exercise isolation implemented.
* [ ] Protected endpoints configured.
* [ ] Unauthorized requests return `401`.
* [ ] Forbidden requests return `403`.

## Exercise Security

* [ ] RED cannot modify Blue assessment.
* [ ] BLUE cannot modify score.
* [ ] WHITE can control assigned Exercise.
* [ ] Exercise participants are verified.
* [ ] Exercise data is isolated.

## Assessment Security

* [ ] Assessment results are server-controlled.
* [ ] Score cannot be directly supplied by client.
* [ ] Assessment review is authorized.
* [ ] Score changes are auditable.

## Audit

* [ ] Sensitive actions are logged.
* [ ] Login failures are logged appropriately.
* [ ] Exercise control actions are logged.
* [ ] Report review is logged.
* [ ] Administrative actions are logged.

## Data Protection

* [ ] No plaintext passwords.
* [ ] No secrets in Git.
* [ ] No secrets in source code.
* [ ] No sensitive data in application logs.
* [ ] Production uses HTTPS.
* [ ] Database credentials are externalized.

---

# 46. Final Security Architecture

```text
                         CLIENT
                           │
                           ▼
                  ┌─────────────────┐
                  │ Authentication  │
                  │      JWT        │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │ System Role     │
                  │                 │
                  │ ADMIN           │
                  │ INSTRUCTOR      │
                  │ STUDENT         │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │ Exercise Role   │
                  │                 │
                  │ RED             │
                  │ BLUE            │
                  │ WHITE           │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │ Resource Access │
                  │ / Exercise      │
                  │ Isolation       │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │ Business Logic  │
                  ├─────────────────┤
                  │ Exercise        │
                  │ Assessment      │
                  │ Scoring         │
                  │ Reporting       │
                  └────────┬────────┘
                           │
                           ▼
                     PostgreSQL
                           │
                           ▼
                      Audit Logs
```

---

# 47. Final Security Decisions

Các quyết định security của V1:

1. PostgreSQL được sử dụng làm database.
2. Authentication sử dụng JWT.
3. Password không bao giờ được lưu plaintext.
4. System Role gồm `ADMIN`, `INSTRUCTOR`, `STUDENT`.
5. Exercise Role gồm `RED`, `BLUE`, `WHITE`.
6. System Role và Exercise Role được tách biệt.
7. Exercise Role được lưu tại `exercise_participants`.
8. Resource-level authorization là bắt buộc.
9. Exercise data phải được isolation.
10. Assessment và Score được kiểm soát phía server.
11. User không được tự thay đổi Score.
12. White Team là Exercise Role, không phải System Role.
13. Security-sensitive operations phải được audit.
14. Secrets không được commit vào repository.
15. Production API sử dụng HTTPS.
16. Security implementation được đặt chủ yếu trong Spring Security + Service layer.
17. V1 chưa yêu cầu OAuth2, MFA hoặc SSO.
