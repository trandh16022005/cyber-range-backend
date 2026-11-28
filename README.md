11/24/2026

# Cyber Range Backend

Backend system for a **Cyber Range and Automated Defensive Skills Assessment Platform**.

The project provides backend services for managing cybersecurity training exercises, collecting events and evidence, evaluating Blue Team defensive activities, and calculating assessment scores.

---

## 1. Project Overview

### 1.1 Introduction

A Cyber Range is a controlled environment designed for cybersecurity training, simulation, and skill assessment.

This project focuses on building a backend system that supports defensive cybersecurity training and automated evaluation.

The system allows instructors to organize exercises, assign participants, manage scenarios, collect security-related events, and evaluate defensive performance.

The platform supports three exercise roles:

- **Red Team:** Performs attack activities defined by the exercise.
- **Blue Team:** Performs defensive activities, detects suspicious events, and responds to incidents.
- **White Team:** Administers, monitors, controls, and reviews exercises.

The backend provides REST APIs for frontend applications and manages the system's business logic, security, data persistence, assessment, and scoring.

### 1.2 Project Objectives

- Manage users, teams, and exercise participants.
- Manage cybersecurity scenarios and Cyber Range environments.
- Manage exercises and their lifecycle.
- Collect events and assessment-related evidence.
- Evaluate Blue Team defensive tasks.
- Automatically calculate assessment scores.
- Measure detection and response performance.
- Manage incident reports.
- Record security-sensitive activities through audit logs.

### 1.3 Main Features

| Module | Description |
|---|---|
| Authentication | User authentication and JWT-based security |
| User Management | Manage users and system roles |
| Team Management | Manage teams and team members |
| Scenario Management | Manage reusable training scenarios |
| Cyber Range Management | Manage Cyber Range environments, assets, and services |
| Exercise Management | Create and control training exercises |
| Event Management | Store observed security events |
| Evidence Management | Store evidence used for assessment |
| Assessment | Evaluate defensive task completion |
| Scoring | Calculate scores based on assessment results |
| Incident Reports | Manage Blue Team incident reports |
| Audit Logging | Record security-sensitive system activities |

---

## 2. Technology Stack

The backend uses the following technologies:

| Technology | Purpose |
|---|---|
| Java 21 | Main programming language |
| Spring Boot | Backend application framework |
| Spring Web | REST API development |
| Spring Data JPA | Database persistence |
| Spring Security | Authentication and authorization |
| JWT | Token-based authentication |
| Bean Validation | Request data validation |
| PostgreSQL | Relational database |
| Maven | Dependency and build management |
| JUnit | Automated testing |

Use the dependencies and versions configured in `pom.xml` as the source of truth.

---

## 3. System Architecture

The backend follows a **layered architecture** and a **package-by-feature** organization.

### 3.1 Application Flow

```text
Frontend Application
        |
        | HTTP / REST API
        v
+---------------------------+
|       Controllers         |
+---------------------------+
        |
        v
+---------------------------+
|         Services          |
|                           |
| Business Logic            |
| Authorization             |
| Assessment                |
| Scoring                   |
+---------------------------+
        |
        v
+---------------------------+
|       Repositories        |
+---------------------------+
        |
        v
+---------------------------+
|       PostgreSQL          |
+---------------------------+
```

### 3.2 Assessment and Scoring Flow

```text
Cyber Range
     |
     v
  Events
     |
     v
  Evidence
     |
     v
Assessment Engine
     |
     v
Assessment Results
     |
     v
 Scoring Engine
     |
     v
Assessment Scores
     |
     v
 Reports
```

The Assessment Engine evaluates defensive task completion based on observable evidence and configured criteria.

The Scoring Engine calculates points based on the resulting assessment statuses and configured scoring rules.

These components have separate responsibilities.

---

## 4. Project Structure

The repository is organized as follows:

```text
cyber-range-backend/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cyberrange/backend/
│   │   │       ├── auth/
│   │   │       ├── user/
│   │   │       ├── team/
│   │   │       ├── scenario/
│   │   │       ├── lab/
│   │   │       ├── event/
│   │   │       ├── assessment/
│   │   │       ├── scoring/
│   │   │       ├── report/
│   │   │       ├── common/
│   │   │       └── config/
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/
│           └── com/cyberrange/backend/
│
├── docs/
│   ├── 01-overview.md
│   ├── 02-architecture.md
│   ├── 03-database.md
│   ├── 04-security.md
│   ├── 05-api.md
│   ├── 06-assessment-and-scoring.md
│   ├── 07-project-structure-coding-convention.md
│   │
│   ├── blue-team/
│   │   ├── overview.md
│   │   ├── detection.md
│   │   ├── incident-response.md
│   │   └── assessment.md
│   │
│   ├── assessment/
│   │   ├── assessment-engine.md
│   │   ├── scoring.md
│   │   └── metrics.md
│   │
│   └── scenarios/
│       ├── overview.md
│       ├── port-scanning.md
│       ├── ssh-brute-force.md
│       └── web-attack.md
│
├── .gitignore
├── .mvn/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── CLAUDE.md
└── README.md
```

The structure above describes the intended organization. Check the actual repository when locating a specific class or resource.

### Main Package Responsibilities

| Package | Responsibility |
|---|---|
| `auth` | Authentication and JWT handling |
| `user` | User management |
| `team` | Team management |
| `scenario` | Scenario management |
| `lab` | Cyber Range, asset, and exercise-related functionality |
| `event` | Event and evidence management |
| `assessment` | Assessment evaluation |
| `scoring` | Score calculation |
| `report` | Incident reports and assessment reports |
| `common` | Shared components and utilities |
| `config` | Application and security configuration |

---

## 5. Requirements

Before running the backend, install the following:

| Requirement | Version |
|---|---|
| Java JDK | 21 |
| PostgreSQL | Compatible with the configured database driver |
| Maven | Maven Wrapper is included |
| Git | Recent version |
| IDE | IntelliJ IDEA recommended |

Verify the Java installation:

```powershell
java -version
```

The output should indicate Java 21.

Verify Git:

```powershell
git --version
```

The project includes Maven Wrapper, so a separate Maven installation is not required.

---

## 6. Getting Started

### 6.1 Clone the Repository

```bash
git clone https://github.com/trandh16022005/cyber-range-backend.git
```

Navigate into the project directory:

```bash
cd cyber-range-backend
```

### 6.2 Configure PostgreSQL

Create the PostgreSQL database according to the database specification:

```sql
CREATE DATABASE cyber_range_db;
```

The database structure must follow:

```text
docs/03-database.md
```

Use the existing project SQL schema and database documentation when initializing the tables.

Do not independently redesign the database schema.

### 6.3 Configure Application Properties

Open:

```text
src/main/resources/application.properties
```

Configure the database connection using your local PostgreSQL settings.

Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cyber_range_db
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

spring.jpa.hibernate.ddl-auto=none
```

Set `DATABASE_USERNAME` and `DATABASE_PASSWORD` in your local environment or use the Windows configuration below.

Important:

- Use your own local database credentials.
- Never commit database passwords or JWT secrets.
- Do not use production credentials in development.
- The managed Windows runtime validates the existing schema at startup.
- Do not use `create` or `update` to automatically modify the existing schema.

Check the project's existing `application.properties` before replacing configuration. Preserve other required settings, such as JWT configuration and application-specific properties.

### 6.4 Build the Project

On Windows PowerShell:

```powershell
.\mvnw.cmd clean install
```

On Linux or macOS:

```bash
./mvnw clean install
```

### 6.5 Run the Backend

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

Alternatively, open the project in IntelliJ IDEA and run the main Spring Boot application class:

```text
com.cyberrange.backend.CyberRangeBackendApplication
```

If the application starts successfully, Spring Boot will display its startup logs in the terminal or IDE console.

By default, Spring Boot uses port `8080` unless the project configuration specifies another port.

The local API base URL is therefore normally:

```text
http://localhost:8080/api/v1
```

### 6.6 Khởi động cả sản phẩm trên Windows

Chuẩn bị Java 21, Python 3, Node.js, Docker Desktop với Linux containers và PostgreSQL. `python.exe`, `node.exe` và `docker.exe` cần có trong `PATH` của PowerShell và IntelliJ. Cài dependencies Frontend một lần với `npm.cmd --prefix frontend ci` từ thư mục gốc. Dùng database hiện có `cyber_range_db`, schema `public`, tại `127.0.0.1:5432`.

Cấp `DATABASE_PASSWORD` và `JWT_SECRET` một lần qua biến môi trường Windows hoặc trong tệp `%LOCALAPPDATA%\CyberRange\application.properties` của tài khoản đang chạy. Với tệp properties, điền giá trị riêng của bạn:

```properties
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<mat-khau-PostgreSQL>
JWT_SECRET=<khoa-ky-JWT-hien-co>
```

Tạo thư mục `%LOCALAPPDATA%\CyberRange` nếu chưa có. Tệp dùng định dạng Java properties và lưu giá trị dưới dạng văn bản. Trong **Properties → Security**, giới hạn quyền đọc cho tài khoản Windows chạy sản phẩm. Giữ tệp ngoài repository. Nếu mật khẩu có dấu gạch chéo ngược, dùng `\\` trong tệp hoặc truyền giá trị gốc qua biến môi trường. Biến môi trường cùng tên ghi đè giá trị trong tệp. Nếu Windows đã có `JWT_SECRET`, chỉ cần bổ sung mật khẩu PostgreSQL vào tệp. Giữ nguyên khóa hiện có, với tối thiểu 32 byte sau giải mã Base64. Nếu mới thay `PATH` hoặc biến môi trường Windows, mở lại IntelliJ để nhận giá trị mới. Backend tự tạo hai Service Token và cấp chúng cho Runner mỗi lần khởi động.

Nếu khởi động báo `Managed Cyber Range configuration is invalid`, xử lý tất cả mục được liệt kê rồi chạy lại. Thông báo chỉ nêu tên cấu hình và điều kiện hợp lệ. Khi PostgreSQL từ chối đăng nhập, kiểm tra username và mật khẩu của `cyber_range_db` trong nguồn cấu hình đã chọn.

Bật PostgreSQL và Docker Desktop. Trong PowerShell 5.1 tại thư mục gốc, chạy:

```powershell
.\mvnw.cmd spring-boot:run
```

Trong IntelliJ, có thể chạy `CyberRangeBackendApplication` với **Working directory** là thư mục gốc và JDK 21. Khi log hiện `Runner authenticated with Backend` và `Frontend ready`, mở [Cyber Range](http://127.0.0.1:5173). Backend lắng nghe trên `127.0.0.1:8080`. Log Runner và Vite nằm trong `target/runtime-logs/`, đường dẫn cụ thể xuất hiện khi khởi động. Tiến trình cũ khởi động bằng hai tab hoặc launcher PowerShell cần được dừng trước khi chuyển sang quy trình này. Đặt `CYBER_RANGE_MANAGED=false` trong môi trường của tiến trình Java để chạy Backend riêng theo mục trước.

Để dừng bình thường, nhấn `Ctrl+C` hoặc dùng thao tác dừng mềm trong IntelliJ rồi đợi `Managed Runner and Frontend process tree closed`. Nếu Lab đang chạy, Backend yêu cầu Runner dừng đúng phiên và đợi `STOPPED`. Nên dừng Lab trong giao diện trước nếu IntelliJ chỉ có thao tác buộc dừng. Khi JVM bị buộc dừng, Windows thu dọn Runner và Vite, còn lần chạy kế tiếp tiếp tục xử lý trạng thái Lab đã lưu. Docker volume và lịch sử vẫn được giữ.

| Triệu chứng | Cách xử lý |
|---|---|
| Cổng `8080` hoặc `5173` bị chiếm | Chạy `Get-NetTCPConnection -State Listen -LocalPort 8080,5173`. Đối chiếu `OwningProcess` và dừng đúng tiến trình đã khởi động. |
| Thiếu `python.exe`, `node.exe`, `docker.exe` hoặc Vite | Kiểm tra `PATH` và chạy `npm.cmd --prefix frontend ci` nếu thiếu dependencies. |
| Docker preflight thất bại | Bật Docker Desktop, chọn context local và kiểm tra Docker đang dùng Linux containers. |
| Hibernate báo thiếu bảng/cột | Kiểm tra đúng database và schema. Đối chiếu [migration SSH Lab](docs/ssh-defensive-workflow.md) và thực hiện theo quy trình sao lưu, phê duyệt. |
| Runner thoát hoặc báo `403` | Mở `runner.log`, kiểm tra còn Runner thủ công hay không, sau đó khởi động lại cả sản phẩm. |
| `ERROR · Đang dừng` | Kiểm tra Runner đang chạy và xem log. Làm mới **Lab Workflow** và đợi `STOPPED`. Khi chạy phiên mới, `READY` xác nhận heartbeat và các probe dịch vụ đã đạt. |

---

## 7. Running Tests

Run all automated tests:

On Windows:

```powershell
.\mvnw.cmd test
```

On Linux or macOS:

```bash
./mvnw test
```

Run a specific test class:

```powershell
.\mvnw.cmd test -Dtest=CyberRangeBackendApplicationTests
```

Assessment and scoring logic should have dedicated tests covering:

- Assessment Result status transitions.
- Evidence-based task evaluation.
- `COMPLETED` and `FAILED` scoring.
- `NOT_APPLICABLE` exclusion from applicable maximum points.
- Provisional and finalized assessment results.
- Score normalization.
- Invalid scoring conditions.

---

## 8. API Documentation

The backend exposes REST APIs under:

```text
/api/v1
```

Main API resource groups include:

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

The API contract is documented in:

[docs/05-api.md](docs/05-api.md)

Use that document as the source of truth for endpoint paths, request formats, response formats, authorization, and HTTP status codes.

---

## 9. Assessment and Scoring

The V1 assessment model evaluates seven main defensive tasks:

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

Assessment Result statuses:

```text
PENDING
IN_PROGRESS
COMPLETED
FAILED
NOT_APPLICABLE
```

Tasks marked `NOT_APPLICABLE` are excluded from both earned points and applicable maximum points.

The final score is normalized:

```text
Final Score =
    (Total Earned Points / Applicable Maximum Points) × 100
```

The final score must not be finalized while applicable tasks remain unresolved.

For detailed rules, see:

- [Assessment and Scoring Specification](docs/06-assessment-and-scoring.md)
- [Assessment Engine](docs/assessment/assessment-engine.md)
- [Scoring](docs/assessment/scoring.md)
- [Metrics](docs/assessment/metrics.md)

---

## 10. Blue Team and Scenario Documentation

### Blue Team

The Blue Team documentation describes defensive responsibilities, detection, incident response, and assessment.

- [Blue Team Overview](docs/blue-team/overview.md)
- [Detection](docs/blue-team/detection.md)
- [Incident Response](docs/blue-team/incident-response.md)
- [Blue Team Assessment](docs/blue-team/assessment.md)

### Scenarios

The initial V1 scenario specifications include:

- [Scenario Overview](docs/scenarios/overview.md)
- [Port Scanning](docs/scenarios/port-scanning.md)
- [SSH Brute Force](docs/scenarios/ssh-brute-force.md)
- [Web Attack](docs/scenarios/web-attack.md)

---

## 11. Development Guidelines

Before implementing a feature:

1. Read `CLAUDE.md`.
2. Read the relevant documentation in `docs/`.
3. Inspect the existing source code.
4. Identify the affected modules.
5. Implement the smallest complete solution.
6. Add or update relevant tests.
7. Review the final changes.

Follow these principles:

- Keep business logic in the service layer.
- Use DTOs for API requests and responses.
- Enforce authorization on the backend.
- Do not trust client-submitted assessment results or scores.
- Keep Assessment Engine and Scoring Engine responsibilities separate.
- Preserve the existing database design.
- Avoid unnecessary dependencies and architectural complexity.
- Keep changes within the assigned development scope.

For more detailed instructions, see:

[CLAUDE.md](CLAUDE.md)

---

## 12. Git Workflow

Check the current working tree before making changes:

```bash
git status
git branch
```

Use meaningful commit messages:

```text
feat: implement user management
fix: validate exercise access
test: add assessment scoring tests
docs: update backend documentation
refactor: improve assessment service
```

Before committing:

```bash
git diff
git status
```

Only stage intended files.

Never commit:

- Database passwords.
- JWT secrets.
- API keys.
- `.env` files containing secrets.
- Private certificates.
- Other sensitive credentials.

---

## 13. Project Documentation

For the complete project specifications, refer to the `docs/` directory.

| Document | Description |
|---|---|
| [Project Overview](docs/01-overview.md) | Project scope and objectives |
| [Architecture](docs/02-architecture.md) | System architecture |
| [Database](docs/03-database.md) | Database schema and relationships |
| [Security](docs/04-security.md) | Authentication and authorization |
| [API](docs/05-api.md) | REST API contract |
| [Assessment and Scoring](docs/06-assessment-and-scoring.md) | Assessment and scoring specification |
| [Coding Convention](docs/07-project-structure-coding-convention.md) | Code organization and conventions |

---

## 14. Project Status

This repository contains the backend implementation of the Cyber Range and Automated Defensive Skills Assessment System.

The V1 design focuses on:

- Core user, team, scenario, and exercise management.
- Cyber Range and asset management.
- Event and evidence collection.
- Rule-based Blue Team assessment.
- Automated scoring.
- Incident reporting.
- Security and audit logging.

Implementation details and completion status should be verified against the current source code and project documentation.

---

## 15. License

This project is developed for academic and educational purposes.

Add a formal license here if the project adopts one.
