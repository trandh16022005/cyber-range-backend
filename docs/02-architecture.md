# Kiến trúc hệ thống

## 1. Tổng quan

Hệ thống **Cyber Range và hệ thống tự động đánh giá kỹ năng phòng thủ** được xây dựng theo mô hình gồm hai phần chính:

* **Cyber Range**: môi trường mô phỏng hệ thống máy chủ và dịch vụ để thực hiện các hoạt động tấn công và phòng thủ.
* **Web Platform**: nền tảng Web quản lý người dùng, Scenario, quá trình thực hành, thu thập dữ liệu và tự động đánh giá kết quả.

Backend của Web Platform được xây dựng bằng **Java Spring Boot**.

Kiến trúc được thiết kế theo hướng module hóa để có thể mở rộng thêm Scenario, loại máy chủ, phương thức thu thập Evidence và các tiêu chí đánh giá trong tương lai.

---

## 2. Mục tiêu kiến trúc

Kiến trúc hệ thống phải đáp ứng các mục tiêu:

1. Tách biệt môi trường Cyber Range và Web Platform.
2. Cho phép quản lý nhiều Scenario khác nhau.
3. Cho phép Red Team thực hiện các hoạt động tấn công trong môi trường được kiểm soát.
4. Cho phép Blue Team thực hiện các hoạt động phòng thủ.
5. Thu thập được Logs, Events và System State phục vụ đánh giá.
6. Assessment Engine có thể tự động xác định trạng thái hoàn thành của các nhiệm vụ phòng thủ.
7. Scoring Engine có thể tính điểm dựa trên các Assessment Criteria.
8. Có thể lưu lại kết quả của từng lần thực hành.
9. Có thể mở rộng thêm Scenario và Assessment Criteria mà không phải thay đổi toàn bộ hệ thống.
10. Có ranh giới rõ ràng giữa các module Backend.

---

# 3. System Context

Ở mức tổng quan, hệ thống có các thành phần:

```text
                         ┌──────────────────────┐
                         │       User           │
                         │                      │
                         │ Red Team / Blue Team │
                         │ Admin / Instructor   │
                         └──────────┬───────────┘
                                    │
                                    │ HTTPS
                                    ▼
                         ┌──────────────────────┐
                         │    Web Platform      │
                         │                      │
                         │    Spring Boot       │
                         └──────────┬───────────┘
                                    │
                ┌───────────────────┼───────────────────┐
                │                   │                   │
                ▼                   ▼                   ▼
        ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
        │ Assessment   │    │   Scoring    │    │  Scenario    │
        │    Engine    │    │    Engine    │    │  Management   │
        └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
               │                   │                   │
               └───────────────────┼───────────────────┘
                                   │
                                   ▼
                         ┌──────────────────────┐
                         │      Database        │
                         └──────────────────────┘

                                   │
                                   │ Events / Logs /
                                   │ System State
                                   ▼
                         ┌──────────────────────┐
                         │     Cyber Range      │
                         │                      │
                         │ Web Server           │
                         │ Linux Server         │
                         │ Windows Server       │
                         │ Database / SSH / AD  │
                         └──────────────────────┘
```

Đây là **kiến trúc khái niệm**, chưa phải kiến trúc triển khai cuối cùng.

---

# 4. High-Level Architecture

Hệ thống được chia thành các lớp chính:

```text
┌─────────────────────────────────────────────────────┐
│                    Client Layer                     │
│                                                     │
│              Web Browser / Web UI                   │
└────────────────────────┬────────────────────────────┘
                         │ HTTPS / REST API
                         ▼
┌─────────────────────────────────────────────────────┐
│                 Application Layer                   │
│                                                     │
│  Authentication    Scenario Management              │
│  User Management   Team Management                  │
│  Event Management  Assessment Engine                │
│  Scoring Engine    Report Management                │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│                    Domain Layer                      │
│                                                     │
│  User / Team / Scenario / Task / Event              │
│  Evidence / Assessment / Score / Report             │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│               Infrastructure Layer                  │
│                                                     │
│  Database                                           │
│  Cyber Range Integration                            │
│  Log/Event Collector                                │
│  External Services                                  │
└─────────────────────────────────────────────────────┘
```

Backend sử dụng kiến trúc module hóa thay vì đưa toàn bộ logic vào Controller.

---

# 5. Backend Architecture

Backend Spring Boot được tổ chức theo các module chức năng.

```text
Spring Boot Backend
│
├── Authentication & Authorization
│
├── User Management
│
├── Team Management
│
├── Scenario Management
│
├── Lab / Session Management
│
├── Event Management
│
├── Evidence Management
│
├── Assessment Engine
│
├── Scoring Engine
│
└── Report Management
```

Mỗi module chịu trách nhiệm cho một nhóm chức năng cụ thể.

Các module không được phụ thuộc lẫn nhau một cách tùy tiện.

---

# 6. Core Modules

## 6.1 Authentication & Authorization

Module chịu trách nhiệm:

* Đăng nhập.
* Xác thực người dùng.
* Kiểm tra quyền truy cập.
* Quản lý Role.
* Bảo vệ các API yêu cầu quyền.

Các Role dự kiến:

* `ADMIN`
* `INSTRUCTOR`
* `RED_TEAM`
* `BLUE_TEAM`

Chi tiết về Authentication, Authorization và RBAC sẽ được định nghĩa trong:

```text
docs/04-security.md
```

---

## 6.2 User Management

Quản lý thông tin người dùng trong hệ thống.

Chức năng chính:

* Tạo User.
* Cập nhật User.
* Xem User.
* Quản lý trạng thái User.
* Gán Role.

---

## 6.3 Team Management

Quản lý Team tham gia Cyber Range.

Team có thể được sử dụng để:

* Xác định thành viên Red Team.
* Xác định thành viên Blue Team.
* Theo dõi kết quả của Team.
* Phân biệt Individual Assessment và Team Assessment trong tương lai.

---

## 6.4 Scenario Management

Scenario mô tả một bài thực hành Cyber Range.

Một Scenario có thể chứa:

* Tên Scenario.
* Mô tả.
* Mục tiêu.
* Cyber Range Environment.
* Red Team Tasks.
* Blue Team Tasks.
* Assessment Criteria.
* Scoring Rules.

Ví dụ Scenario V1:

```text
Scenario: Web + SSH Attack

Red Team:
    ├── Port Scanning
    ├── SSH Brute Force
    └── Web Attack

Blue Team:
    ├── Detect Port Scan
    ├── Detect Brute Force
    ├── Detect Web Attack
    ├── Identify Compromised Host
    ├── Isolate Host
    ├── Recover Service
    └── Submit Incident Report
```

---

# 7. Lab / Session Management

Một **Lab Session** đại diện cho một lần thực hành Scenario.

Luồng cơ bản:

```text
Create Scenario
      ↓
Start Lab Session
      ↓
Red Team Attack
      ↓
Blue Team Defense
      ↓
End Lab Session
      ↓
Assessment
      ↓
Scoring
      ↓
Report
```

Lab Session cần lưu các thông tin cần thiết để xác định:

* Scenario nào được sử dụng.
* Ai tham gia.
* Team nào tham gia.
* Thời gian bắt đầu.
* Thời gian kết thúc.
* Trạng thái Session.
* Kết quả Assessment.

---

# 8. Cyber Range Integration

Cyber Range là môi trường thực thi các hệ thống và dịch vụ được sử dụng trong Scenario.

Các thành phần mục tiêu gồm:

```text
Cyber Range
│
├── Web Server
│
├── Linux Server
│
├── Windows Server
│
├── Database
│
├── SSH
│
└── Active Directory
```

Trong V1, hệ thống ưu tiên hỗ trợ các thành phần cần thiết cho:

* Port Scanning.
* SSH Brute Force.
* Web Attack.
* Log Analysis.
* Detection.
* Host Isolation.
* Service Recovery.

Windows Server và Active Directory có thể được mở rộng ở các phiên bản sau nếu chưa cần triển khai đầy đủ trong V1.

---

# 9. Event & Log Flow

Các hoạt động xảy ra trong Cyber Range tạo ra Logs và Events.

Luồng dữ liệu tổng quát:

```text
Red Team Activity
        │
        ▼
Cyber Range
        │
        ▼
Logs / Security Events / System State
        │
        ▼
Event Collector
        │
        ▼
Backend
        │
        ▼
Event Storage
        │
        ├───────────────┐
        ▼               ▼
Assessment Engine   Report / Metrics
```

Backend không chỉ dựa vào hành động mà User khai báo.

Assessment phải ưu tiên sử dụng **Evidence có thể quan sát được** từ Cyber Range.

Ví dụ:

```text
Port Scan
    ↓
Network / Security Event
    ↓
Event Collector
    ↓
Stored Event
    ↓
Assessment Engine
    ↓
Detect Port Scan = COMPLETED
```

---

# 10. Evidence

**Evidence** là dữ liệu được sử dụng để chứng minh một Blue Team Task đã được thực hiện.

Evidence có thể đến từ:

* Security Logs.
* Application Logs.
* Authentication Logs.
* Network Events.
* System State.
* Service State.
* Host State.
* Các dữ liệu quan sát được khác trong Cyber Range.

Ví dụ:

```text
Blue Team Task:
"Isolate attacked host"

Evidence:
Host network state = isolated

Assessment:
Task completed
```

Assessment Engine phải dựa trên Evidence thay vì chỉ dựa vào một giá trị do Client gửi lên.

---

# 11. Assessment Engine

Assessment Engine là thành phần quan trọng của hệ thống.

Nhiệm vụ:

1. Nhận thông tin Lab Session.
2. Xác định các Assessment Criteria.
3. Thu thập Evidence.
4. Đối chiếu Evidence với Criteria.
5. Xác định trạng thái Task.
6. Tính thời điểm hoàn thành.
7. Tạo Assessment Result.

Luồng:

```text
Lab Session
     │
     ▼
Assessment Criteria
     │
     ▼
Evidence
     │
     ▼
Evaluation Rules
     │
     ▼
Task Status
     │
     ├── NOT_STARTED
     ├── IN_PROGRESS
     ├── COMPLETED
     └── FAILED
```

Assessment Engine phải được thiết kế theo hướng có thể thêm Criteria mới mà không phải thay đổi toàn bộ hệ thống.

---

# 12. Assessment Criteria

Assessment Criteria xác định điều kiện để một nhiệm vụ được xem là hoàn thành.

Ví dụ:

```text
Criterion:
Detect Port Scan

Condition:
Có Evidence xác định Blue Team đã phát hiện hoạt động Port Scanning.

Result:
COMPLETED
```

Một Criterion có thể chứa:

* Tên.
* Mô tả.
* Task.
* Evidence Type.
* Evaluation Rule.
* Completion Condition.
* Thời gian đánh giá.

Chi tiết dữ liệu sẽ được xác định trong `docs/03-database.md`.

---

# 13. Scoring Engine

Scoring Engine chịu trách nhiệm tính điểm dựa trên kết quả Assessment.

Luồng:

```text
Assessment Result
       │
       ▼
Scoring Rules
       │
       ▼
Calculate Score
       │
       ▼
Final Score
```

Scoring Engine không trực tiếp xác định User có hoàn thành Task hay không.

Phân chia trách nhiệm:

```text
Assessment Engine
    → Task có hoàn thành không?

Scoring Engine
    → Task đó được bao nhiêu điểm?
```

Điều này giúp tách biệt logic đánh giá và logic tính điểm.

---

# 14. V1 Scoring Model

V1 sử dụng tổng điểm 100:

| Assessment Criterion      |   Score |
| ------------------------- | ------: |
| Detect Port Scan          |      10 |
| Detect Brute Force        |      10 |
| Detect Web Attack         |      15 |
| Identify Compromised Host |      15 |
| Isolate Attacked Host     |      20 |
| Recover Service           |      20 |
| Incident Report           |      10 |
| **Total**                 | **100** |

Điểm số phải được lưu lại cùng với kết quả Assessment.

Scoring Rules cần được thiết kế theo hướng có thể mở rộng.

Không nên hard-code toàn bộ điểm số trực tiếp trong Controller hoặc Service nghiệp vụ.

---

# 15. Detection Time và Response Time

Hệ thống cần hỗ trợ các Metrics liên quan đến thời gian.

## Detection Time

Thời gian từ khi Attack Event xảy ra đến khi Blue Team hoàn thành Detection Task.

```text
Attack Event
     │
     ├─────────────── Detection Time ────────────────┐
     │                                                │
     ▼                                                ▼
Attack occurs                              Detection completed
```

## Response Time

Thời gian từ khi Detection xảy ra đến khi Blue Team hoàn thành Response Action.

```text
Detection
     │
     ├─────────────── Response Time ─────────────────┐
     │                                                │
     ▼                                                ▼
Response starts                              Response completed
```

Các Metrics này được sử dụng để đánh giá chất lượng phản ứng của Blue Team.

---

# 16. Report Management

Sau khi Assessment và Scoring hoàn thành, hệ thống tạo kết quả báo cáo.

Report có thể bao gồm:

* Scenario.
* Lab Session.
* Team / User.
* Thời gian thực hành.
* Các Task.
* Task đã hoàn thành.
* Task chưa hoàn thành.
* Evidence.
* Score từng Task.
* Total Score.
* Detection Time.
* Response Time.
* Tổng kết kết quả.

Luồng:

```text
Assessment
     +
Score
     +
Metrics
     ↓
Report
     ↓
Web Platform
```

---

# 17. Data Flow tổng thể

Luồng dữ liệu chính của hệ thống:

```text
                 ┌──────────────┐
                 │    Admin     │
                 └──────┬───────┘
                        │
                        ▼
                Create Scenario
                        │
                        ▼
                 ┌──────────────┐
                 │   Scenario   │
                 └──────┬───────┘
                        │
                        ▼
                 Start Lab Session
                        │
          ┌─────────────┴─────────────┐
          │                           │
          ▼                           ▼
    Red Team Activity          Blue Team Activity
          │                           │
          ▼                           ▼
    Cyber Range               Defensive Actions
          │                           │
          └─────────────┬─────────────┘
                        ▼
                Logs / Events
                        │
                        ▼
                 Event Collector
                        │
                        ▼
                   Database
                        │
                        ▼
               Assessment Engine
                        │
                        ▼
                Assessment Result
                        │
                        ▼
                 Scoring Engine
                        │
                        ▼
                    Score
                        │
                        ▼
                    Report
                        │
                        ▼
                  Web Platform
```

---

# 18. Module Dependency

Các module nên tuân theo dependency direction:

```text
Controller
    ↓
Service
    ↓
Domain / Repository
    ↓
Database
```

Ví dụ:

```text
ScenarioController
        ↓
ScenarioService
        ↓
ScenarioRepository
        ↓
Database
```

Assessment:

```text
AssessmentController
        ↓
AssessmentService
        ↓
AssessmentEngine
        ↓
Evidence / Event Repository
        ↓
Database
```

Scoring:

```text
ScoringController
        ↓
ScoringService
        ↓
ScoringEngine
        ↓
Assessment Result
        ↓
Database
```

Controller không được chứa business logic phức tạp.

Business logic phải nằm ở Service hoặc các component domain phù hợp.

---

# 19. Package Structure dự kiến

Backend Spring Boot dự kiến sử dụng package-by-feature:

```text
src/main/java/com/cyberrange/backend/

├── auth/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── team/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── scenario/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── lab/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── event/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── assessment/
│   ├── controller/
│   ├── service/
│   ├── engine/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── scoring/
│   ├── controller/
│   ├── service/
│   ├── engine/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
└── report/
    ├── controller/
    ├── service/
    ├── repository/
    ├── entity/
    └── dto/
```

Đây là cấu trúc dự kiến ở mức Architecture. Cấu trúc chi tiết và Coding Convention sẽ được chuẩn hóa trong:

```text
docs/07-project-structure-coding-convention.md
```

---

# 20. Deployment Architecture

Kiến trúc triển khai dự kiến:

```text
                         User
                           │
                           │ HTTPS
                           ▼
                  ┌─────────────────┐
                  │    Web Client   │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  Spring Boot   │
                  │    Backend     │
                  └───────┬─────────┘
                          │
             ┌────────────┼────────────┐
             │            │            │
             ▼            ▼            ▼
        ┌────────┐   ┌──────────┐  ┌────────────┐
        │Database│   │   Event  │  │ Assessment │
        │        │   │ Collector│  │   Engine   │
        └────────┘   └─────┬────┘  └────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │ Cyber Range  │
                    ├──────────────┤
                    │ Web Server   │
                    │ Linux Server │
                    │ Windows      │
                    └──────────────┘
```

Trong giai đoạn phát triển, các thành phần có thể chạy trên cùng một máy hoặc các máy/VM khác nhau.

Kiến trúc logic phải được giữ độc lập với cách triển khai vật lý.

---

# 21. V1 Architecture Scope

V1 tập trung vào kiến trúc tối thiểu nhưng đầy đủ cho quy trình:

```text
Scenario
   ↓
Lab Session
   ↓
Attack
   ↓
Event / Log
   ↓
Blue Team Defense
   ↓
Evidence
   ↓
Assessment
   ↓
Scoring
   ↓
Report
```

V1 ưu tiên:

* Web Server.
* Linux Server.
* SSH.
* Web Attack.
* Port Scanning.
* Brute Force.
* Log/Event Collection.
* Assessment Engine.
* Scoring Engine.
* Report.
* User / Team Management.

Các thành phần nâng cao như Windows Server, Active Directory, Privilege Escalation và Persistence có thể được triển khai ở các giai đoạn mở rộng nếu không cần thiết cho V1.

---

# 22. Future Extension

Kiến trúc phải cho phép mở rộng:

```text
V1
│
├── Port Scanning
├── SSH Brute Force
├── Web Attack
├── Detection
├── Isolation
├── Recovery
└── Incident Report
        │
        ▼
Future
│
├── Privilege Escalation
├── Persistence
├── Windows Server
├── Active Directory
├── SIEM Integration
├── Automated Response
├── Advanced Detection
├── Team Assessment
├── Individual Assessment
└── AI / ML Assistance
```

Việc mở rộng không nên yêu cầu thay đổi toàn bộ kiến trúc hiện tại.

---

# 23. Nguyên tắc kiến trúc

Hệ thống phải tuân thủ các nguyên tắc:

### 23.1 Separation of Concerns

Mỗi module có trách nhiệm riêng.

Ví dụ:

```text
Scenario Management
    ≠
Assessment
    ≠
Scoring
    ≠
Report
```

### 23.2 Evidence-based Assessment

Assessment phải dựa trên Evidence có thể quan sát được trong Cyber Range.

Không coi Client request đơn thuần là bằng chứng đủ để hoàn thành một nhiệm vụ quan trọng.

### 23.3 Configurable Assessment

Assessment Criteria và Scoring Rules phải có khả năng cấu hình và mở rộng.

### 23.4 Modular Design

Các module phải được thiết kế độc lập để có thể phát triển song song.

### 23.5 Extensibility

Kiến trúc phải hỗ trợ thêm Scenario, Server, Event Type, Assessment Criteria và Scoring Rule.

### 23.6 Security by Design

Authentication, Authorization, Input Validation và các cơ chế bảo vệ API phải được xem xét ngay từ đầu.

Chi tiết sẽ được định nghĩa trong `docs/04-security.md`.

---

# 24. Architectural Boundary

Tài liệu này chỉ xác định kiến trúc ở mức hệ thống và module.

Các nội dung sau **chưa được quyết định chi tiết tại bước này**:

* Database Schema cụ thể.
* Table/Column cụ thể.
* REST API Endpoint cụ thể.
* JWT configuration cụ thể.
* Authentication implementation cụ thể.
* Chi tiết Docker/VM deployment.
* Chi tiết Log Collector implementation.
* Chi tiết Assessment Rule implementation.

Các nội dung trên sẽ được xác định ở các tài liệu tiếp theo.

---

# 25. Tóm tắt kiến trúc

Kiến trúc hệ thống được tổ chức xoay quanh chu trình:

```text
CREATE SCENARIO
      ↓
START LAB
      ↓
RED TEAM ATTACK
      ↓
COLLECT EVENTS
      ↓
BLUE TEAM DEFENSE
      ↓
COLLECT EVIDENCE
      ↓
ASSESSMENT ENGINE
      ↓
SCORING ENGINE
      ↓
REPORT
      ↓
WEB PLATFORM
```

Trong đó:

* **Cyber Range** cung cấp môi trường thực hành.
* **Event Collector** thu thập Logs và Events.
* **Assessment Engine** xác định Blue Team đã hoàn thành nhiệm vụ hay chưa.
* **Scoring Engine** tính điểm dựa trên Assessment Result.
* **Web Platform** quản lý người dùng, Scenario, Session và hiển thị kết quả.
* **Database** lưu trữ dữ liệu của hệ thống.

Kiến trúc này là cơ sở cho các bước thiết kế tiếp theo:

```text
01-overview.md
     ↓
02-architecture.md
     ↓
03-database.md
     ↓
04-security.md
     ↓
05-api.md
     ↓
06-assessment-and-scoring.md
     ↓
07-project-structure-coding-convention.md
```
