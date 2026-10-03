# Project Structure and Coding Convention

## 1. Purpose

Tài liệu này quy định cấu trúc mã nguồn, coding convention và các nguyên tắc phát triển Backend cho dự án Cyber Range.

Mục tiêu:

* Thống nhất cách tổ chức source code.
* Thống nhất cách đặt tên.
* Phân tách rõ trách nhiệm giữa các layer.
* Giảm xung đột khi nhiều thành viên cùng phát triển.
* Đảm bảo code dễ đọc, dễ test và dễ mở rộng.
* Đảm bảo code được triển khai phù hợp với Architecture, Database, Security, API và Assessment/Scoring Specification.
* Cung cấp quy tắc làm việc thống nhất cho Claude Code.

Tài liệu này là **coding guideline của project**.

Các quyết định về nghiệp vụ và kiến trúc phải tham chiếu các tài liệu:

```text
docs/01-overview.md
docs/02-architecture.md
docs/03-database.md
docs/04-security.md
docs/05-api.md
docs/06-assessment-and-scoring.md
```

Nếu coding convention mâu thuẫn với các tài liệu nghiệp vụ/kiến trúc trên, phải ưu tiên các tài liệu đặc tả và xác nhận lại trước khi thay đổi.

---

# 2. Technology Baseline

Backend sử dụng các công nghệ chính:

| Technology      | Purpose                          |
| --------------- | -------------------------------- |
| Java 21         | Programming language             |
| Spring Boot     | Backend framework                |
| Spring Web      | REST API                         |
| Spring Data JPA | Persistence                      |
| Spring Security | Authentication and Authorization |
| JWT             | Stateless API authentication     |
| Bean Validation | Request validation               |
| PostgreSQL      | Database                         |
| Maven           | Build and dependency management  |
| JUnit           | Testing                          |

Các dependency khác chỉ được thêm khi có yêu cầu kỹ thuật rõ ràng.

Không tự ý thêm dependency chỉ vì một thư viện có thể làm cho việc triển khai ngắn hơn.

---

# 3. Project Structure

Cấu trúc tổng thể:

```text
cyber-range-backend/
├── .mvn/
├── docs/
│   ├── 01-overview.md
│   ├── 02-architecture.md
│   ├── 03-database.md
│   ├── 04-security.md
│   ├── 05-api.md
│   ├── 06-assessment-and-scoring.md
│   ├── 07-project-structure-coding-convention.md
│   ├── blue-team/
│   ├── assessment/
│   └── scenarios/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── cyberrange/
│   │   │           └── backend/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── ...
│   └── test/
│       └── java/
├── CLAUDE.md
├── README.md
├── pom.xml
├── mvnw
└── mvnw.cmd
```

---

# 4. Java Package Structure

Backend sử dụng mô hình **package-by-feature**.

Package root:

```text
com.cyberrange.backend
```

Cấu trúc:

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

## 4.1. Feature Packages

Các package chính tương ứng với các domain/module của hệ thống.

| Package      | Responsibility                                |
| ------------ | --------------------------------------------- |
| `auth`       | Authentication, JWT                           |
| `user`       | User management                               |
| `team`       | Team management                               |
| `scenario`   | Scenario, Defensive Task, Assessment Criteria |
| `lab`        | Cyber Range, Asset, Service, Exercise         |
| `event`      | Event và Evidence                             |
| `assessment` | Assessment Engine                             |
| `scoring`    | Scoring Engine                                |
| `report`     | Incident Report và Exercise Report            |
| `common`     | Thành phần dùng chung                         |
| `config`     | Application configuration                     |

Tên package có thể mở rộng khi hệ thống phát triển, nhưng phải phù hợp với domain đã được định nghĩa.

---

# 5. Structure Inside a Feature

Mỗi feature có thể sử dụng cấu trúc:

```text
feature/
├── controller/
├── dto/
├── entity/
├── repository/
├── service/
└── mapper/
```

Ví dụ:

```text
assessment/
├── controller/
│   └── AssessmentController.java
├── dto/
│   ├── AssessmentResponse.java
│   └── AssessmentResultResponse.java
├── entity/
│   ├── Assessment.java
│   └── AssessmentResult.java
├── repository/
│   ├── AssessmentRepository.java
│   └── AssessmentResultRepository.java
├── service/
│   ├── AssessmentService.java
│   └── AssessmentEvaluationService.java
└── mapper/
    └── AssessmentMapper.java
```

Không bắt buộc mọi feature phải có đầy đủ các package con.

Nếu một feature chưa cần `mapper` hoặc `dto` riêng thì không tạo folder chỉ để làm cho cấu trúc đẹp hơn.

---

# 6. Layer Responsibilities

Luồng xử lý chính:

```text
HTTP Request
     |
     v
Controller
     |
     v
Service
     |
     v
Repository
     |
     v
Database
```

Mỗi layer có trách nhiệm riêng.

| Layer      | Responsibility                         |
| ---------- | -------------------------------------- |
| Controller | Nhận HTTP request và trả HTTP response |
| DTO        | Đại diện dữ liệu API Request/Response  |
| Service    | Business logic                         |
| Repository | Database access                        |
| Entity     | Persistence model                      |
| Mapper     | Chuyển đổi Entity ↔ DTO                |
| Config     | Application/security configuration     |
| Common     | Thành phần dùng chung                  |

---

# 7. Controller Convention

Controller chịu trách nhiệm:

* Nhận HTTP request.
* Validate request thông qua DTO.
* Gọi Service.
* Trả HTTP response.
* Xác định HTTP status phù hợp.

Controller không được chứa business logic phức tạp.

Ví dụ:

```java
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/{id}")
    public ExerciseResponse getExercise(@PathVariable Long id) {
        return exerciseService.getExercise(id);
    }
}
```

Không đặt logic như:

```text
calculate score
evaluate assessment
check complex exercise state
direct database query
```

trực tiếp trong Controller.

---

# 8. DTO Convention

API sử dụng DTO để tách API Contract khỏi Entity.

Có thể sử dụng:

```text
CreateUserRequest
UpdateUserRequest
UserResponse

CreateExerciseRequest
UpdateExerciseRequest
ExerciseResponse

AssessmentResponse
AssessmentResultResponse
```

## 8.1. Request DTO

Request DTO đại diện dữ liệu Client gửi lên.

Ví dụ:

```java
public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        @Email String email
) {
}
```

## 8.2. Response DTO

Response DTO đại diện dữ liệu Backend trả về.

```java
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String status
) {
}
```

Không trả Entity trực tiếp ra API.

---

# 9. Entity Convention

Entity đại diện cho persistence model.

Ví dụ:

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ...
}
```

Các nguyên tắc:

* Entity phải tương ứng với Database Design.
* Tên table sử dụng `snake_case`.
* Không dùng Entity trực tiếp làm API response.
* Không đưa password/hash/token vào Response DTO.
* Quan hệ giữa Entity phải được định nghĩa rõ.
* Không sử dụng `CascadeType.ALL` nếu chưa có lý do rõ ràng.
* Không sử dụng `FetchType.EAGER` tùy tiện.
* Không đưa business logic phức tạp vào Entity nếu logic đó thuộc Service/Domain processing.

---

# 10. Repository Convention

Repository chịu trách nhiệm truy cập Database.

Ví dụ:

```java
public interface AssessmentRepository
        extends JpaRepository<Assessment, Long> {
}
```

Repository có thể chứa:

* CRUD operations.
* Derived query.
* Custom query khi cần.

Repository không chịu trách nhiệm:

* Authorization.
* Assessment logic.
* Score calculation.
* Exercise state management.
* Business workflow.

Ví dụ không nên đặt trong Repository:

```text
if user is allowed
if exercise is completed
calculate score
evaluate defensive task
```

Các logic trên thuộc Service hoặc Assessment/Scoring processing.

---

# 11. Service Convention

Service là nơi xử lý business logic.

Ví dụ:

```java
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;

    public AssessmentResponse getAssessment(Long exerciseId) {
        // Business logic
    }
}
```

Service có thể chịu trách nhiệm:

* Kiểm tra business rules.
* Kiểm tra resource access.
* Thực hiện workflow.
* Gọi Repository.
* Gọi các Service khác.
* Tạo/cập nhật domain data.
* Thực hiện Assessment/Scoring logic.

---

# 12. Assessment and Scoring Code Organization

Assessment và Scoring là core functionality của project.

Không đặt Assessment/Scoring logic trong Controller.

Cấu trúc đề xuất:

```text
assessment/
├── controller/
├── dto/
├── entity/
├── repository/
├── service/
│   ├── AssessmentService.java
│   └── AssessmentEvaluationService.java
└── mapper/

scoring/
├── controller/
├── dto/
├── entity/
├── repository/
├── service/
│   ├── ScoringService.java
│   └── ScoreCalculationService.java
└── mapper/
```

Pipeline phải giữ nguyên:

```text
Event
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
```

Không được rút gọn thành:

```text
Event → Score
```

---

# 13. Security Convention

Security phải tuân thủ `docs/04-security.md`.

Có hai loại role:

```text
System Role
    ADMIN
    INSTRUCTOR
    STUDENT

Exercise Role
    RED
    BLUE
    WHITE
```

System Role và Exercise Role không được trộn lẫn.

Exercise Role phải được xác định dựa trên:

```text
Authenticated User
        ↓
Exercise
        ↓
Exercise Participant
        ↓
Exercise Role
```

Không tin tưởng Exercise Role do Client gửi lên.

Ví dụ không được sử dụng trực tiếp:

```json
{
  "role": "WHITE"
}
```

để quyết định authorization.

Backend phải kiểm tra role thực tế của User trong Exercise.

---

# 14. Authorization Convention

Authentication xác định User là ai.

Authorization xác định User được phép làm gì.

Các operation quan trọng phải kiểm tra:

```text
Authenticated User
        ↓
System Role
        ↓
Exercise Participation
        ↓
Exercise Role
        ↓
Resource Access
        ↓
Business Rule
```

Ví dụ:

```text
Start Exercise
```

không chỉ kiểm tra User đã đăng nhập.

Phải kiểm tra User có quyền điều khiển Exercise đó hay không.

---

# 15. Validation Convention

Validation được thực hiện ở hai mức.

## 15.1. Request Validation

Sử dụng Bean Validation:

```text
@NotNull
@NotBlank
@Email
@Size
@Min
@Max
```

Ví dụ:

```java
public record CreateTeamRequest(
        @NotBlank String name
) {
}
```

## 15.2. Business Validation

Các điều kiện nghiệp vụ nằm trong Service.

Ví dụ:

```text
Request validation:
name không được rỗng

Business validation:
team không được thêm cùng một user hai lần
```

Không cố gắng giải quyết toàn bộ business validation bằng annotation.

---

# 16. Exception Handling

Không xử lý exception riêng lẻ trong từng Controller nếu không cần thiết.

Sử dụng centralized exception handling:

```text
@RestControllerAdvice
        |
        v
GlobalExceptionHandler
```

Các nhóm HTTP error chính:

| Status | Meaning               |
| ------ | --------------------- |
| `400`  | Bad Request           |
| `401`  | Unauthorized          |
| `403`  | Forbidden             |
| `404`  | Not Found             |
| `409`  | Conflict              |
| `500`  | Internal Server Error |

Application không được trả stack trace hoặc thông tin nội bộ không cần thiết cho Client trong production.

---

# 17. API Response Convention

API phải thống nhất response structure.

Success:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Example"
  }
}
```

Error:

```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Exercise not found"
  }
}
```

Các API mới phải tuân thủ API Contract trong:

```text
docs/05-api.md
```

Không tự ý thay đổi endpoint, HTTP method hoặc request/response structure nếu chưa cập nhật API specification.

---

# 18. Pagination Convention

Các API trả về danh sách lớn nên hỗ trợ pagination khi cần.

Ví dụ:

```text
GET /api/exercises?page=0&size=20
```

Response có thể chứa:

```text
data
page
size
totalElements
totalPages
```

Không nên tải toàn bộ dữ liệu của một resource lớn chỉ bằng một request nếu không có lý do.

---

# 19. Transaction Convention

Sử dụng `@Transactional` khi một business operation cần nhiều thay đổi Database được thực hiện như một transaction.

Ví dụ:

```text
Create Exercise
    ↓
Create Participants
    ↓
Create Assessment
```

Nếu các operation phải thành công hoặc thất bại cùng nhau, chúng nên được xử lý trong cùng transaction.

Không đặt `@Transactional` tùy tiện trên mọi method.

---

# 20. Naming Convention

## 20.1. Java

Class:

```text
AssessmentService
AssessmentController
AssessmentRepository
AssessmentResult
```

Method:

```text
getAssessment()
createExercise()
calculateScore()
evaluateTask()
```

Variable:

```text
exerciseId
assessmentResult
awardedScore
```

Constant:

```text
MAX_SCORE
DEFAULT_PAGE_SIZE
```

---

# 21. Database Naming

Database sử dụng `snake_case`.

Ví dụ:

```text
users
roles
team_members
defensive_tasks
assessment_criteria
cyber_ranges
exercise_participants
assessment_results
assessment_scores
incident_reports
audit_logs
```

Java sử dụng:

```text
User
Role
TeamMember
DefensiveTask
AssessmentCriterion
CyberRange
ExerciseParticipant
AssessmentResult
AssessmentScore
IncidentReport
AuditLog
```

Mapping:

```text
assessment_results
        ↕
AssessmentResult
```

---

# 22. File and Class Naming

Tên file phải khớp với public class.

Ví dụ:

```text
AssessmentController.java
AssessmentService.java
AssessmentRepository.java
AssessmentResponse.java
```

Không sử dụng tên chung chung như:

```text
Manager.java
Helper.java
Utils.java
Data.java
Common.java
```

trừ khi trách nhiệm của class thực sự rõ ràng và được dùng chung.

---

# 23. Mapper Convention

Mapper chịu trách nhiệm chuyển đổi giữa Entity và DTO.

Ví dụ:

```text
Assessment Entity
       ↓
AssessmentMapper
       ↓
AssessmentResponse
```

Không đưa business logic vào Mapper.

Mapper chỉ thực hiện transformation.

Ví dụ:

```java
public AssessmentResponse toResponse(Assessment entity) {
    return new AssessmentResponse(
            entity.getId(),
            entity.getStatus()
    );
}
```

---

# 24. Logging Convention

Logging phục vụ:

* Debugging.
* Monitoring.
* Error diagnosis.
* Operational tracking.

Không ghi log:

```text
password
JWT
secret
database password
API secret
private credentials
```

Có thể log:

```text
Exercise started
Exercise stopped
Assessment completed
Scoring completed
Authorization denied
Important system error
```

Audit Log và Application Log là hai loại dữ liệu khác nhau.

```text
Application Log
    → phục vụ vận hành/debugging

Audit Log
    → phục vụ security/audit/history
```

---

# 25. Configuration Convention

Configuration phải được quản lý thông qua configuration files và environment variables.

Không hard-code:

```text
database password
JWT secret
API secret
production credentials
```

Ví dụ:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

Các giá trị nhạy cảm phải được externalize.

---

# 26. Testing Structure

Test code nằm trong:

```text
src/test/java/com/cyberrange/backend/
```

Nên tổ chức tương ứng với production package:

```text
src/test/java/com/cyberrange/backend/
├── auth/
├── user/
├── team/
├── scenario/
├── lab/
├── event/
├── assessment/
├── scoring/
└── report/
```

---

# 27. Testing Priority

Các khu vực cần ưu tiên test:

### High Priority

```text
Authentication
Authorization
Assessment Engine
Scoring Engine
Exercise state transitions
```

### Medium Priority

```text
User management
Team management
Scenario management
Event processing
Incident reports
```

### API/Integration

Kiểm tra:

* HTTP status.
* Request validation.
* Authorization.
* Response structure.
* Database interaction.

---

# 28. Assessment and Scoring Tests

Assessment và Scoring là chức năng cốt lõi nên phải có test riêng.

Ví dụ:

```text
AssessmentEvaluationServiceTest
ScoreCalculationServiceTest
```

Các trường hợp cần kiểm tra:

```text
Criterion satisfied
Criterion not satisfied
Task completed
Task not completed
Detection time calculation
Response time calculation
Maximum score
Zero score
Duplicate scoring prevention
Total score calculation
```

Ví dụ:

```text
Detect Brute Force
Maximum Score = 10

Satisfied:
Awarded Score = 10

Not satisfied:
Awarded Score = 0
```

---

# 29. Git Branch Convention

Không phát triển trực tiếp trên `main` đối với feature thông thường.

Branch naming:

```text
feature/<name>
fix/<name>
refactor/<name>
docs/<name>
test/<name>
```

Ví dụ:

```text
feature/auth
feature/assessment-engine
feature/scoring-engine
feature/user-management
fix/exercise-authorization
docs/api-update
test/scoring-engine
```

---

# 30. Commit Convention

Commit message sử dụng format:

```text
<type>: <description>
```

Các type chính:

| Type       | Purpose          |
| ---------- | ---------------- |
| `feat`     | Feature mới      |
| `fix`      | Bug fix          |
| `docs`     | Documentation    |
| `test`     | Test             |
| `refactor` | Refactoring      |
| `chore`    | Maintenance      |
| `build`    | Build/dependency |

Ví dụ:

```text
feat: add assessment service
feat: implement exercise management
fix: validate exercise participant
docs: update API specification
test: add scoring engine tests
refactor: simplify assessment evaluation
build: update spring dependencies
```

Commit message phải mô tả thay đổi chính.

---

# 31. Pull Request Convention

Trước khi merge vào `main`:

```text
Feature Branch
      ↓
Commit
      ↓
Push
      ↓
Pull Request
      ↓
Code Review
      ↓
Tests
      ↓
Merge
```

Pull Request nên mô tả:

* What changed?
* Why?
* Main files changed.
* API changes.
* Database changes.
* Security impact.
* Test result.

---

# 32. Documentation Synchronization

Khi thay đổi implementation làm ảnh hưởng đến specification, phải kiểm tra các tài liệu liên quan.

Ví dụ:

### API thay đổi

Kiểm tra:

```text
docs/05-api.md
```

### Database thay đổi

Kiểm tra:

```text
docs/03-database.md
```

### Security thay đổi

Kiểm tra:

```text
docs/04-security.md
```

### Assessment/Scoring thay đổi

Kiểm tra:

```text
docs/06-assessment-and-scoring.md
```

Không để code và specification trở thành hai nguồn sự thật khác nhau.

---

# 33. Claude Code Working Convention

Cả thành viên trong team đều có thể sử dụng Claude Code.

Claude Code phải tuân thủ các quy tắc sau.

## 33.1. Read Before Code

Trước khi thực hiện task, Claude Code phải đọc:

```text
CLAUDE.md
```

và các tài liệu liên quan trong:

```text
docs/
```

Tối thiểu phải kiểm tra tài liệu tương ứng với task.

Ví dụ:

```text
Assessment task
    ↓
06-assessment-and-scoring.md

API task
    ↓
05-api.md

Security task
    ↓
04-security.md

Database task
    ↓
03-database.md
```

---

## 33.2. Do Not Invent Architecture

Claude Code không được tự ý:

* Tạo architecture mới.
* Tạo Entity không có trong Database Design.
* Tạo API không có trong API Contract.
* Thay đổi authentication model.
* Thay đổi RBAC model.
* Thay đổi Assessment/Scoring pipeline.
* Thêm Team type mới.
* Thêm dependency lớn mà không có lý do.

Nếu requirement chưa rõ hoặc mâu thuẫn với documentation:

```text
STOP
  ↓
Explain conflict
  ↓
Ask for confirmation
```

Không tự đoán.

---

# 34. Claude Code Database Rules

Claude Code phải tuân thủ `docs/03-database.md`.

Không tự tạo:

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
ssh_servers
active_directory
```

nếu chưa có quyết định mới về Database Design.

Các Team Role:

```text
RED
BLUE
WHITE
```

được quản lý thông qua Exercise Participant, không tạo bảng riêng cho từng Team.

---

# 35. Claude Code API Rules

Claude Code phải tuân thủ:

```text
docs/05-api.md
```

Không tự ý:

* Đổi endpoint.
* Đổi HTTP method.
* Đổi request structure.
* Đổi response structure.
* Thêm API duplicate.
* Cho Client gửi final score.
* Cho Client quyết định Assessment Result.

Nếu cần API mới:

```text
Requirement
    ↓
API Specification
    ↓
Implementation
```

Không làm ngược lại.

---

# 36. Claude Code Security Rules

Claude Code phải tuân thủ:

```text
docs/04-security.md
```

Không được:

* Hard-code secret.
* Log password.
* Log JWT.
* Bypass authorization để "cho chạy được".
* Tin tưởng role do Client gửi.
* Cho Student truy cập dữ liệu Exercise không thuộc phạm vi.
* Cho Client tự gửi Score cuối cùng.

Security check phải được thực hiện ở Backend.

---

# 37. Claude Code Assessment and Scoring Rules

Claude Code phải tuân thủ:

```text
docs/06-assessment-and-scoring.md
```

Pipeline bắt buộc:

```text
Event
  ↓
Evidence
  ↓
Assessment
  ↓
Assessment Result
  ↓
Scoring
  ↓
Assessment Score
```

Không viết logic:

```text
Event → Score
```

Không hard-code điểm vào Controller.

Không để Client gửi:

```text
score = 100
```

để Backend chấp nhận trực tiếp.

---

# 38. Claude Code Scope Control

Khi được giao một task, Claude Code chỉ nên thay đổi những file cần thiết.

Ví dụ task:

```text
Implement Assessment Service
```

Không tự ý đồng thời:

```text
rewrite authentication
change database schema
rewrite API response
refactor unrelated modules
```

trừ khi thay đổi đó thực sự cần thiết để hoàn thành task.

Nếu phát hiện vấn đề ngoài scope:

```text
1. Report the issue.
2. Explain the impact.
3. Do not silently rewrite unrelated code.
```

---

# 39. Definition of Done

Một task được coi là hoàn thành khi:

* [ ] Code nằm đúng package.
* [ ] Naming đúng convention.
* [ ] Không vi phạm Architecture.
* [ ] Không vi phạm Database Design.
* [ ] Không vi phạm Security Design.
* [ ] API phù hợp với API Contract.
* [ ] Validation phù hợp.
* [ ] Exception handling phù hợp.
* [ ] Business logic nằm trong Service.
* [ ] Không expose Entity trực tiếp nếu API yêu cầu DTO.
* [ ] Có test cho business logic quan trọng.
* [ ] `mvn test` thành công.
* [ ] Không có secret trong source code.
* [ ] Documentation được cập nhật nếu contract/specification thay đổi.
* [ ] Git diff được kiểm tra trước khi commit.

---

# 40. Development Workflow

Workflow chuẩn:

```text
1. Read documentation
        |
        v
2. Understand requirement
        |
        v
3. Create feature branch
        |
        v
4. Implement
        |
        v
5. Run tests
        |
        v
6. Review code/diff
        |
        v
7. Update documentation if required
        |
        v
8. Commit
        |
        v
9. Push
        |
        v
10. Pull Request
        |
        v
11. Code Review
        |
        v
12. Merge
```

---

# 41. Source of Truth

Khi phát triển Backend, các tài liệu sau là nguồn tham chiếu chính:

```text
01-overview.md
    ↓
Project scope and objectives

02-architecture.md
    ↓
System architecture

03-database.md
    ↓
Database and entities

04-security.md
    ↓
Authentication, authorization and security

05-api.md
    ↓
API contract

06-assessment-and-scoring.md
    ↓
Assessment and scoring behavior

07-project-structure-coding-convention.md
    ↓
Implementation and coding convention
```

`CLAUDE.md` sẽ cung cấp hướng dẫn trực tiếp cho Claude Code dựa trên các tài liệu trên.

---

# 42. Final Project Structure

Cấu trúc Backend mục tiêu:

```text
com.cyberrange.backend/
│
├── auth/
│   ├── controller/
│   ├── dto/
│   ├── service/
│   └── ...
│
├── user/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── team/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── scenario/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── lab/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── event/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── assessment/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── scoring/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── report/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── mapper/
│
├── common/
│   ├── exception/
│   ├── response/
│   ├── validation/
│   └── util/
│
└── config/
    ├── SecurityConfig.java
    ├── JwtConfig.java
    └── ...
```

Không nhất thiết phải tạo toàn bộ package/file ngay từ đầu.

Chỉ tạo component khi feature tương ứng thực sự được triển khai.

---

# 43. Final Principles

Các nguyên tắc bắt buộc của Backend:

1. **Package-by-feature** thay vì package-by-layer ở cấp project.
2. Controller chỉ xử lý HTTP/API.
3. Service xử lý business logic.
4. Repository chỉ xử lý persistence.
5. DTO được sử dụng để bảo vệ API contract khỏi Entity.
6. Entity đại diện persistence model.
7. Validation được thực hiện ở Request và Business layer phù hợp.
8. Exception được xử lý tập trung.
9. Authentication và Authorization phải được thực hiện server-side.
10. System Role và Exercise Role phải được tách biệt.
11. Assessment và Scoring phải là hai trách nhiệm riêng biệt.
12. Assessment phải dựa trên Evidence.
13. Scoring phải dựa trên Assessment Result.
14. Client không được quyết định Score.
15. Secret không được hard-code hoặc commit vào Git.
16. Business logic quan trọng phải có test.
17. API và Database phải đồng bộ với documentation.
18. Không tự ý thay đổi architecture hoặc contract.
19. Claude Code phải đọc documentation trước khi implement.
20. Khi requirement mâu thuẫn với specification, phải xác nhận trước khi thay đổi.
21. Feature branch được sử dụng cho development.
22. Code phải được test trước khi Pull Request.
23. Mỗi thay đổi phải có scope rõ ràng.
24. Documentation phải được cập nhật khi specification thay đổi.
25. `main` phải luôn ở trạng thái có thể build và test được.
