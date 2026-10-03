# Assessment and Scoring Specification

## 1. Overview

Tài liệu này mô tả cơ chế **tự động đánh giá kỹ năng phòng thủ và tính điểm** trong hệ thống Cyber Range.

Hệ thống gồm hai thành phần chính:

* **Assessment Engine:** xác định mức độ hoàn thành các nhiệm vụ phòng thủ của Blue Team dựa trên Event, Evidence, trạng thái hệ thống và các tiêu chí đánh giá.
* **Scoring Engine:** tính điểm dựa trên kết quả đánh giá và các tiêu chí được định nghĩa trước.

Mục tiêu của thiết kế là giảm sự phụ thuộc vào việc chấm điểm thủ công, đồng thời đảm bảo kết quả đánh giá có thể truy xuất về Evidence làm cơ sở.

---

# 2. Scope

Phạm vi V1 của Assessment Engine và Scoring Engine bao gồm:

* Tự động đánh giá nhiệm vụ phòng thủ.
* Xác định trạng thái hoàn thành của từng nhiệm vụ.
* Ghi nhận Evidence phục vụ đánh giá.
* Tính thời gian phát hiện.
* Tính thời gian phản ứng.
* Tính điểm theo các tiêu chí đã định nghĩa.
* Tổng hợp điểm của Exercise.
* Sinh dữ liệu phục vụ báo cáo kết quả.
* Cho phép White Team kiểm tra và review kết quả.

V1 sử dụng cơ chế **Rule-based Assessment** và **Rule-based Scoring**.

Các cơ chế AI/ML hoặc đánh giá hành vi phức tạp không thuộc phạm vi V1.

---

# 3. Assessment and Scoring Architecture

Quy trình tổng thể:

```text
                    CYBER RANGE
                         |
                         v
                       EVENT
                         |
                         v
                     EVIDENCE
                         |
                         v
              +----------------------+
              |   ASSESSMENT ENGINE  |
              |                      |
              | Evidence Evaluation  |
              | Criteria Evaluation  |
              | Task Evaluation      |
              | Time Calculation     |
              +----------+-----------+
                         |
                         v
                 ASSESSMENT RESULT
                         |
                         v
              +----------------------+
              |    SCORING ENGINE    |
              |                      |
              | Criterion Score      |
              | Task Score           |
              | Total Score          |
              +----------+-----------+
                         |
                         v
                    FINAL REPORT
```

Assessment và Scoring được thiết kế thành hai trách nhiệm riêng biệt.

```text
Assessment Engine
    "Blue Team đã hoàn thành nhiệm vụ chưa?"

Scoring Engine
    "Nếu nhiệm vụ đạt thì được bao nhiêu điểm?"
```

---

# 4. Assessment Model

## 4.1. Assessment

**Assessment** là quá trình xác định kết quả thực hiện các nhiệm vụ phòng thủ của Blue Team trong một Exercise.

Một Assessment thuộc về một Exercise.

```text
Exercise
    |
    v
Assessment
    |
    +-- Assessment Result
    +-- Assessment Result
    +-- Assessment Result
```

Trong V1, một Exercise có một Assessment chính.

---

## 4.2. Assessment Result

**Assessment Result** biểu diễn kết quả đánh giá của một Defensive Task.

Một Assessment có nhiều Assessment Result.

```text
Assessment
    |
    +-- Detect Port Scan
    |
    +-- Detect Brute Force
    |
    +-- Identify Compromised Host
    |
    +-- Isolate Attacked Host
    |
    +-- Recover Service
    |
    +-- Incident Report
```

Mỗi Assessment Result có thể lưu:

* Defensive Task.
* Status.
* Detection Time.
* Response Time.
* Detection Timestamp.
* Response Timestamp.
* Completion Timestamp.
* Reason.

---

# 5. Assessment Status

V1 sử dụng các trạng thái:

| Status          | Meaning                                                   |
| --------------- | --------------------------------------------------------- |
| `PENDING`       | Nhiệm vụ chưa được đánh giá hoặc chưa có Evidence phù hợp |
| `IN_PROGRESS`   | Nhiệm vụ đang được thực hiện                              |
| `COMPLETED`     | Nhiệm vụ đã được xác định là hoàn thành                   |
| `FAILED`        | Nhiệm vụ không đạt điều kiện đánh giá                     |
| `NOT_COMPLETED` | Exercise kết thúc nhưng nhiệm vụ chưa hoàn thành          |

Trạng thái được xác định bởi Backend và Assessment Engine.

Client không được tự gửi trạng thái `COMPLETED` để yêu cầu hệ thống cộng điểm.

---

# 6. Event, Evidence, Assessment Result and Score

Các khái niệm này phải được phân biệt rõ ràng.

```text
EVENT
  |
  | Raw / observed information
  v
EVIDENCE
  |
  | Assessment-relevant information
  v
ASSESSMENT RESULT
  |
  | Result of evaluating a task/criterion
  v
SCORE
```

## 6.1. Event

Event là sự kiện được thu thập từ Cyber Range.

Ví dụ:

```text
SSH authentication failure
Web security event
Host state change
Service state change
Network security event
```

Event phản ánh thông tin quan sát được từ môi trường.

---

## 6.2. Evidence

Evidence là thông tin được xác định là có giá trị cho quá trình đánh giá.

Ví dụ:

```text
Event:
Multiple SSH authentication failures

        ↓

Evidence:
Possible SSH brute-force activity
```

Evidence được sử dụng làm cơ sở để Assessment Engine đánh giá Criteria và Defensive Tasks.

---

## 6.3. Assessment Result

Assessment Result là kết quả sau khi Assessment Engine kiểm tra Evidence và các điều kiện của nhiệm vụ.

Ví dụ:

```text
Evidence:
SSH brute-force activity detected

        ↓

Assessment:

Detect Brute Force
= COMPLETED
```

---

## 6.4. Score

Score là kết quả tính điểm sau khi Assessment Result và Assessment Criteria đã được xác định.

```text
Assessment Result:
COMPLETED

        ↓

Scoring Engine

        ↓

Awarded Score:
10 / 10
```

---

# 7. Assessment Criteria

**Assessment Criteria** định nghĩa điều kiện được sử dụng để đánh giá một Defensive Task.

Quan hệ:

```text
Scenario
    |
    +-- Defensive Task
            |
            +-- Assessment Criterion
            +-- Assessment Criterion
```

Ví dụ:

| Defensive Task            | Assessment Criterion                |
| ------------------------- | ----------------------------------- |
| Detect Port Scan          | Port scanning activity detected     |
| Detect Brute Force        | SSH brute-force activity detected   |
| Detect Web Attack         | Web attack activity detected        |
| Identify Compromised Host | Correct compromised host identified |
| Isolate Attacked Host     | Attacked host isolated              |
| Recover Service           | Target service restored             |
| Incident Report           | Incident report submitted           |

Assessment Criterion xác định điều kiện đánh giá, không phải kết quả cuối cùng.

---

# 8. Rule-based Assessment

V1 sử dụng Rule-based Assessment.

Một Rule có thể kiểm tra:

* Sự xuất hiện của Event.
* Sự tồn tại của Evidence.
* Mối quan hệ giữa các Event.
* Trạng thái của Asset.
* Trạng thái của Service.
* Hành động phòng thủ của Blue Team.
* Nội dung/trạng thái của Incident Report.

Mô hình tổng quát:

```text
IF
    required evidence exists
AND
    assessment conditions are satisfied
THEN
    criterion = SATISFIED
```

Ví dụ:

```text
Criterion:
Detect Brute Force

Condition:
Matching SSH authentication evidence exists

Result:
Criterion = SATISFIED
```

Các điều kiện chi tiết của từng Scenario được định nghĩa trong tài liệu Scenario tương ứng.

---

# 9. Defensive Task Assessment

Một Defensive Task có thể được đánh giá thông qua một hoặc nhiều Criteria.

Ví dụ:

```text
Defensive Task:
Isolate Attacked Host

        |
        +-- Criterion:
            Target host is isolated
```

Assessment Engine kiểm tra các Criteria.

Nếu điều kiện cần thiết được thỏa mãn:

```text
Criterion = SATISFIED
        |
        v
Task = COMPLETED
```

Nếu Exercise kết thúc nhưng điều kiện chưa được đáp ứng:

```text
Task = NOT_COMPLETED
```

---

# 10. Detection Time

Detection Time là thời gian từ khi sự kiện/tình huống tấn công được xác định bắt đầu cho đến khi Blue Team hoàn thành điều kiện phát hiện tương ứng.

```text
T_attack
    |
    | Attack/Event begins
    |
    v
T_detect
    |
    | Detection criterion satisfied
```

Công thức:

```text
Detection Time = T_detect - T_attack
```

Ví dụ:

```text
Attack starts:
10:00:00

Blue Team detection:
10:03:10

Detection Time:
190 seconds
```

Detection Time được lưu phục vụ đánh giá và báo cáo.

---

# 11. Response Time

Response Time là thời gian từ khi Blue Team phát hiện sự cố đến khi hoàn thành hành động phản ứng tương ứng.

```text
T_detect
    |
    | Detection completed
    |
    v
T_response
    |
    | Defensive response completed
```

Công thức:

```text
Response Time = T_response - T_detect
```

Ví dụ:

```text
Detection:
10:03:10

Host isolation:
10:06:00

Response Time:
170 seconds
```

Detection Time và Response Time là hai chỉ số khác nhau.

```text
Detection Time
    ≠
Response Time
```

---

# 12. Assessment Workflow

Quy trình đánh giá:

```text
1. Exercise starts
        |
        v
2. Cyber Range generates events
        |
        v
3. Events are collected
        |
        v
4. Evidence is generated/identified
        |
        v
5. Assessment Engine evaluates criteria
        |
        v
6. Assessment Result is updated
        |
        v
7. Detection / Response time is calculated
        |
        v
8. Exercise ends
        |
        v
9. Scoring Engine calculates scores
        |
        v
10. Final result is generated
```

---

# 13. Scoring Engine

Scoring Engine chịu trách nhiệm tính điểm dựa trên kết quả Assessment.

Scoring Engine không trực tiếp phát hiện tấn công và không tự xác định Blue Team đã hoàn thành nhiệm vụ.

```text
Assessment Engine
        |
        | Result
        v
Scoring Engine
        |
        v
Assessment Score
```

---

# 14. V1 Scoring Model

Tổng điểm V1 là **100 điểm**.

| Criterion                 | Maximum Score |
| ------------------------- | ------------: |
| Detect Port Scan          |            10 |
| Detect Brute Force        |            10 |
| Detect Web Attack         |            15 |
| Identify Compromised Host |            15 |
| Isolate Attacked Host     |            20 |
| Recover Service           |            20 |
| Incident Report           |            10 |
| **Total**                 |       **100** |

Điểm được tính bằng tổng điểm của các Criteria đạt yêu cầu.

```text
Total Score = Σ Awarded Score
```

---

# 15. Criterion Scoring

Mỗi Assessment Criterion có:

* Maximum Score.
* Awarded Score.
* Reason.

Ví dụ:

```text
Criterion:
Detect Brute Force

Maximum Score:
10

Assessment:
SATISFIED

Awarded Score:
10
```

Nếu Criterion không đạt:

```text
Maximum Score:
10

Assessment:
NOT SATISFIED

Awarded Score:
0
```

V1 sử dụng mô hình đạt/không đạt ở mức Criterion.

Cơ chế partial scoring có thể được bổ sung trong tương lai nếu yêu cầu đánh giá cần chi tiết hơn.

---

# 16. Scoring Rules

## 16.1. Server-side Scoring

Điểm phải được tính tại Backend.

Client không được gửi tổng điểm để Backend lưu trực tiếp.

```text
Client
   |
   | Assessment evidence / user action
   v
Backend
   |
   v
Assessment Engine
   |
   v
Scoring Engine
   |
   v
Score
```

---

## 16.2. Evidence-based Scoring

Mỗi điểm được trao phải có cơ sở từ Assessment Result và Evidence tương ứng.

Không được cộng điểm chỉ dựa trên dữ liệu do Client tự khai báo.

---

## 16.3. Maximum Score

Điểm được trao không được vượt quá điểm tối đa của Criterion.

```text
Awarded Score <= Maximum Score
```

---

## 16.4. No Duplicate Scoring

Một Criterion không được cộng điểm nhiều lần ngoài phạm vi của Rule đã định nghĩa.

Ví dụ:

```text
Detect Brute Force = 10 points
```

Không được cộng thêm 10 điểm chỉ vì hệ thống phát hiện nhiều Event khác nhau cùng thuộc một Criterion.

---

## 16.5. Assessment Before Scoring

Assessment phải được thực hiện trước khi tính điểm.

Không sử dụng trực tiếp:

```text
Event → Score
```

Mà sử dụng:

```text
Event
  ↓
Evidence
  ↓
Assessment
  ↓
Assessment Result
  ↓
Score
```

---

# 17. Assessment Score Data

Kết quả điểm được lưu trong `assessment_scores`.

Các thông tin chính:

| Field                  | Description                        |
| ---------------------- | ---------------------------------- |
| `assessment_result_id` | Assessment Result tương ứng        |
| `criterion_id`         | Criterion được đánh giá            |
| `awarded_score`        | Điểm thực tế được trao             |
| `max_score`            | Điểm tối đa tại thời điểm đánh giá |
| `reason`               | Lý do/giải thích kết quả           |

`max_score` được lưu cùng kết quả để giữ lại thông tin của Rule tại thời điểm Assessment được thực hiện.

---

# 18. Assessment Result Data

`assessment_results` lưu kết quả đánh giá của từng Defensive Task.

Các thông tin chính:

| Field               | Description          |
| ------------------- | -------------------- |
| `assessment_id`     | Assessment           |
| `defensive_task_id` | Defensive Task       |
| `status`            | Trạng thái nhiệm vụ  |
| `detected_at`       | Thời điểm phát hiện  |
| `responded_at`      | Thời điểm phản ứng   |
| `completed_at`      | Thời điểm hoàn thành |
| `detection_time`    | Thời gian phát hiện  |
| `response_time`     | Thời gian phản ứng   |
| `reason`            | Lý do kết quả        |

`detection_time` và `response_time` được lưu dưới dạng khoảng thời gian, ví dụ số giây.

---

# 19. Example: SSH Brute Force

Scenario:

```text
SSH Brute Force
```

Blue Team Task:

```text
Detect Brute Force
```

Trong quá trình Exercise:

```text
Cyber Range
    |
    v
SSH authentication events
    |
    v
Event Collection
    |
    v
Evidence
    |
    v
Assessment Engine
```

Assessment Engine kiểm tra Criterion:

```text
SSH brute-force activity detected
```

Nếu điều kiện được đáp ứng:

```text
Assessment Result:
COMPLETED
```

Scoring Engine:

```text
Maximum Score = 10
Awarded Score = 10
```

---

# 20. Example: Host Isolation

Defensive Task:

```text
Isolate Attacked Host
```

Assessment Engine kiểm tra trạng thái của target host.

Nếu Evidence xác nhận host đã được cô lập:

```text
Criterion:
Target host isolated

        ↓

SATISFIED

        ↓

Task:
COMPLETED
```

Scoring Engine:

```text
Maximum Score = 20
Awarded Score = 20
```

---

# 21. Example: Service Recovery

Defensive Task:

```text
Recover Service
```

Assessment Engine kiểm tra trạng thái Service trước và sau hành động phục hồi.

Ví dụ:

```text
Service:
UNAVAILABLE
       |
       | Blue Team recovery
       v
Service:
AVAILABLE
```

Nếu trạng thái cuối phù hợp với điều kiện của Criterion:

```text
Recover Service
= COMPLETED
```

Scoring Engine:

```text
Awarded Score = 20
```

---

# 22. Incident Report Assessment

Incident Report là một trong các tiêu chí đánh giá của V1.

Blue Team gửi báo cáo sự cố thông qua Web Platform.

Quy trình:

```text
Blue Team
    |
    v
Incident Report
    |
    v
Backend
    |
    v
Assessment Engine
    |
    v
Incident Report Criterion
```

Criterion có thể kiểm tra việc báo cáo đã được gửi theo yêu cầu của Exercise.

White Team có thể review nội dung báo cáo.

---

# 23. White Team and Assessment

Assessment Engine không thay thế White Team.

White Team vẫn có vai trò:

* Theo dõi Exercise.
* Kiểm tra tiến trình.
* Xem Event và Evidence.
* Xem Assessment Result.
* Review Incident Report.
* Kiểm tra kết quả đánh giá.
* Phục vụ quá trình tổng kết Exercise.

Mô hình:

```text
Cyber Range
      |
      v
Events / Evidence
      |
      v
Assessment Engine
      |
      v
Scoring Engine
      |
      v
White Team Review
      |
      v
Final Report
```

Assessment Engine giúp giảm công việc đánh giá thủ công nhưng vẫn giữ khả năng kiểm tra và review của White Team.

---

# 24. Database Mapping

Assessment và Scoring sử dụng các Entity đã được định nghĩa trong Database Design.

```text
scenarios
    |
    v
defensive_tasks
    |
    v
assessment_criteria
```

Trong Exercise:

```text
exercises
    |
    +-- events
    |
    +-- evidence
```

Assessment:

```text
assessments
    |
    v
assessment_results
    |
    v
assessment_scores
```

Incident Report:

```text
exercises
    |
    v
incident_reports
```

Audit:

```text
audit_logs
```

---

# 25. Assessment and Scoring Flow

Toàn bộ quy trình V1:

```text
                    SCENARIO
                       |
                       v
                    EXERCISE
                       |
             +---------+---------+
             |                   |
             v                   v
         RED TEAM            BLUE TEAM
             |                   |
             |              Defensive Tasks
             |                   |
             +---------+---------+
                       |
                       v
                     EVENTS
                       |
                       v
                    EVIDENCE
                       |
                       v
             +----------------------+
             |   ASSESSMENT ENGINE  |
             +----------+-----------+
                        |
                        v
               ASSESSMENT RESULT
                        |
              +---------+---------+
              |                   |
              v                   v
       Detection Time       Response Time
              |                   |
              +---------+---------+
                        |
                        v
             +----------------------+
             |    SCORING ENGINE    |
             +----------+-----------+
                        |
                        v
                ASSESSMENT SCORE
                        |
                        v
                  TOTAL SCORE
                        |
                        v
                     REPORT
```

---

# 26. Example Final Result

Một Exercise có thể tạo kết quả:

```text
Exercise: SSH Brute Force Training

Detect Port Scan          10 / 10
Detect Brute Force        10 / 10
Detect Web Attack          0 / 15
Identify Compromised Host 15 / 15
Isolate Attacked Host     20 / 20
Recover Service           20 / 20
Incident Report           10 / 10
-----------------------------------
Total                     85 / 100
```

Ngoài điểm số, hệ thống có thể hiển thị:

```text
Detection Time
Response Time
Task Status
Evidence
Assessment Reason
Incident Report
```

---

# 27. Explainability

Một kết quả Assessment phải có khả năng giải thích.

Ví dụ:

```text
Task:
Detect Brute Force

Status:
COMPLETED

Reason:
Matching evidence was found.

Evidence:
SSH authentication events

Detection Time:
190 seconds

Score:
10 / 10
```

Mục đích là giúp Blue Team và White Team hiểu tại sao hệ thống xác định nhiệm vụ đạt hoặc không đạt.

---

# 28. V1 Scenario Mapping

Các Scenario V1:

```text
Port Scanning
SSH Brute Force
Web Attack
```

Các nhóm Defensive Task:

```text
Log Analysis
Detection
Identify Compromised Host
Isolation
Recovery
Incident Report
```

Assessment Engine sử dụng cùng một pipeline:

```text
Scenario
    ↓
Defensive Task
    ↓
Assessment Criteria
    ↓
Evidence
    ↓
Assessment Result
    ↓
Score
```

Chi tiết Event Pattern, Evidence Pattern và Rule của từng Scenario được định nghĩa riêng trong:

```text
docs/scenarios/
├── overview.md
├── port-scanning.md
├── ssh-brute-force.md
└── web-attack.md
```

Không hard-code toàn bộ Scenario Rule vào tài liệu Assessment chung.

---

# 29. V1 Limitations

V1 có các giới hạn:

* Assessment sử dụng Rule-based Assessment.
* Scoring sử dụng Rule-based Scoring.
* Chưa sử dụng AI/ML.
* Chưa có Behavior-based Assessment phức tạp.
* Chưa có Adaptive Scoring.
* Chưa có Automated Incident Response.
* Chưa đánh giá toàn diện kỹ năng Blue Team ngoài các Criteria đã định nghĩa.
* Chi tiết Event/Evidence Rule phụ thuộc vào từng Scenario.

Các giới hạn này phù hợp với phạm vi Cyber Range quy mô nhỏ của V1.

---

# 30. Extensibility

Kiến trúc phải cho phép bổ sung Scenario và Assessment Criteria mà không phải thay đổi toàn bộ Assessment Engine.

Mô hình:

```text
New Scenario
      |
      v
New Defensive Tasks
      |
      v
New Assessment Criteria
      |
      v
New Evidence Rules
      |
      v
Existing Assessment Pipeline
      |
      v
Existing Scoring Pipeline
```

Các hướng mở rộng:

* Privilege Escalation.
* Persistence.
* Windows Server.
* Active Directory.
* Additional Web Attacks.
* Additional Defensive Tasks.
* Individual Assessment.
* Team Assessment.
* Automated Response.
* AI/ML-based Assessment.

---

# 31. Separation of Responsibilities

| Component           | Responsibility                              |
| ------------------- | ------------------------------------------- |
| Cyber Range         | Cung cấp môi trường thực hành               |
| Event Collector     | Thu thập Event                              |
| Evidence Processing | Xử lý/chuẩn hóa Evidence                    |
| Assessment Engine   | Xác định nhiệm vụ/criteria có đạt hay không |
| Scoring Engine      | Tính điểm                                   |
| Web Platform        | Hiển thị và quản lý dữ liệu                 |
| White Team          | Giám sát và review                          |
| Report Module       | Tổng hợp kết quả                            |

Các component không được thay thế trách nhiệm của nhau.

---

# 32. Design Principles

1. Assessment và Scoring là hai trách nhiệm riêng biệt.
2. Assessment phải dựa trên Evidence có thể quan sát được.
3. Event không đồng nghĩa với Evidence.
4. Evidence không đồng nghĩa với Assessment Result.
5. Assessment Result không đồng nghĩa với Score.
6. Điểm phải được tính ở Backend.
7. Client không được tự quyết định Assessment Result.
8. Client không được tự quyết định Score.
9. Detection Time và Response Time phải được định nghĩa rõ ràng.
10. V1 sử dụng Rule-based Assessment.
11. V1 sử dụng Rule-based Scoring.
12. White Team vẫn giữ vai trò giám sát và review.
13. Một Criterion không được cộng điểm trùng ngoài Rule cho phép.
14. Awarded Score không được vượt Maximum Score.
15. Assessment Result phải có khả năng truy xuất Evidence làm cơ sở.
16. Các Scenario mới phải có khả năng sử dụng lại Assessment/Scoring Pipeline.
17. Assessment và Scoring phải phù hợp với Database Design.
18. Assessment và Scoring phải phù hợp với API Contract.
19. Chi tiết Rule của từng Scenario được định nghĩa tại tài liệu Scenario tương ứng.

---

# 33. Final Architecture

```text
                         CYBER RANGE
                              |
                              v
                            EVENT
                              |
                              v
                          EVIDENCE
                              |
                              v
                 +----------------------+
                 |   ASSESSMENT ENGINE  |
                 |                      |
                 | Evidence Evaluation  |
                 | Criteria Evaluation  |
                 | Task Evaluation      |
                 | Time Calculation     |
                 +----------+-----------+
                            |
                            v
                    ASSESSMENT RESULT
                            |
             +--------------+--------------+
             |                             |
             v                             v
      Detection Time                Response Time
             |                             |
             +--------------+--------------+
                            |
                            v
                 +----------------------+
                 |    SCORING ENGINE     |
                 |                      |
                 | Criterion Score       |
                 | Task Score            |
                 | Total Score           |
                 +----------+-----------+
                            |
                            v
                       FINAL RESULT
                            |
              +-------------+-------------+
              |                           |
              v                           v
          REPORT                   WHITE TEAM REVIEW
```

---

# 34. Final V1 Definition

Assessment Engine của hệ thống có nhiệm vụ:

> **Tự động xác định mức độ hoàn thành các nhiệm vụ phòng thủ của Blue Team dựa trên Event, Evidence, trạng thái hệ thống và Assessment Criteria.**

Scoring Engine có nhiệm vụ:

> **Tính điểm dựa trên Assessment Result và các Scoring Criteria đã được định nghĩa.**

Pipeline chính của hệ thống:

```text
EVENT
  ↓
EVIDENCE
  ↓
ASSESSMENT
  ↓
ASSESSMENT RESULT
  ↓
SCORING
  ↓
ASSESSMENT SCORE
  ↓
TOTAL SCORE
  ↓
REPORT
```

Đây là pipeline chuẩn được sử dụng làm cơ sở cho Database, API, Backend Implementation và các Scenario của Cyber Range.
