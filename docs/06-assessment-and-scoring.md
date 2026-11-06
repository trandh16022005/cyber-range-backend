# Assessment and Scoring Specification

## 1. Overview

Tài liệu này mô tả cơ chế **tự động đánh giá kỹ năng phòng thủ và tính điểm** trong hệ thống Cyber Range.

Hệ thống gồm hai thành phần chính:

- **Assessment Engine:** Xác định mức độ hoàn thành các nhiệm vụ phòng thủ của Blue Team dựa trên Event, Evidence, trạng thái hệ thống và các tiêu chí đánh giá.
- **Scoring Engine:** Tính điểm dựa trên Assessment Result và trọng số được định nghĩa trước.

Mục tiêu của thiết kế:

- Giảm sự phụ thuộc vào việc chấm điểm thủ công.
- Đảm bảo kết quả đánh giá có cơ sở từ Evidence.
- Đảm bảo điểm số được tính thống nhất tại Backend.
- Cho phép truy xuất và giải thích kết quả đánh giá.
- Hỗ trợ White Team kiểm tra và review kết quả.

## 2. Scope

Phạm vi V1 của Assessment Engine và Scoring Engine bao gồm:

- Tự động đánh giá nhiệm vụ phòng thủ.
- Xác định trạng thái hoàn thành của từng Defensive Task.
- Ghi nhận Evidence phục vụ đánh giá.
- Tính Detection Time.
- Tính Response Time.
- Tính điểm theo trọng số đã định nghĩa.
- Tổng hợp điểm của Exercise.
- Xử lý các nhiệm vụ không áp dụng.
- Sinh dữ liệu phục vụ báo cáo kết quả.
- Cho phép White Team kiểm tra và review kết quả.

V1 sử dụng:

- Rule-based Assessment.
- Rule-based Scoring.
- Binary Task Evaluation (hoàn thành hoặc không hoàn thành đối với nhiệm vụ đã được đánh giá).

Các cơ chế AI/ML hoặc đánh giá hành vi phức tạp không thuộc phạm vi V1.

## 3. Assessment and Scoring Architecture

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
              +--------------------------+
              |     ASSESSMENT ENGINE    |
              |                          |
              | Evidence Evaluation      |
              | Criteria Evaluation      |
              | Task Evaluation          |
              | Time Calculation         |
              +------------+-------------+
                           |
                           v
                   ASSESSMENT RESULT
                           |
                           v
              +--------------------------+
              |       SCORING ENGINE     |
              |                          |
              | Task Score               |
              | Applicable Maximum Score|
              | Earned Points             |
              | Final Score               |
              +------------+-------------+
                           |
                           v
                      FINAL REPORT
```

Assessment và Scoring là hai trách nhiệm riêng biệt.

```text
Assessment Engine:
"Blue Team đã hoàn thành nhiệm vụ chưa?"

Scoring Engine:
"Nhiệm vụ đã hoàn thành được bao nhiêu điểm?"
```

Assessment Engine quyết định kết quả đánh giá.

Scoring Engine chỉ sử dụng kết quả đánh giá đã được xác định để tính điểm.

## 4. Assessment Model

### 4.1. Assessment

Assessment là quá trình xác định kết quả thực hiện các nhiệm vụ phòng thủ của Blue Team trong một Exercise.

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

### 4.2. Assessment Result

Assessment Result biểu diễn kết quả đánh giá của một Defensive Task trong một Assessment.

Một Assessment có nhiều Assessment Result.

```text
Assessment
   |
   +-- Detect Port Scan
   |
   +-- Detect Brute Force
   |
   +-- Detect Web Attack
   |
   +-- Identify Compromised Host
   |
   +-- Isolate Attacked Host
   |
   +-- Recover Service
   |
   +-- Incident Report
```

Mỗi Assessment Result có thể lưu các thông tin:

- Defensive Task.
- Status.
- Detection Time.
- Response Time.
- Detection Timestamp.
- Response Timestamp.
- Completion Timestamp.
- Reason.

Các trường dữ liệu thực tế phải phù hợp với Database Schema hiện có.

## 5. Assessment Status

V1 sử dụng chính xác 5 trạng thái Assessment Result:

| Status | Meaning |
|---|---|
| `PENDING` | Nhiệm vụ chưa bắt đầu hoặc chưa có đủ thông tin để đánh giá. |
| `IN_PROGRESS` | Nhiệm vụ đang được thực hiện. |
| `COMPLETED` | Nhiệm vụ đã đáp ứng các điều kiện đánh giá. |
| `FAILED` | Nhiệm vụ đã được đánh giá nhưng không đáp ứng các điều kiện yêu cầu. |
| `NOT_APPLICABLE` | Nhiệm vụ không áp dụng đối với Exercise hiện tại. |

`NOT_COMPLETED` không phải là trạng thái hợp lệ trong V1.

### 5.1. Status Rules

- Assessment Result được khởi tạo với trạng thái `PENDING`.
- Khi người tham gia bắt đầu thực hiện nhiệm vụ, trạng thái có thể chuyển thành `IN_PROGRESS`.
- Khi các điều kiện đánh giá được đáp ứng, trạng thái chuyển thành `COMPLETED`.
- Khi nhiệm vụ được đánh giá và không đáp ứng điều kiện, trạng thái chuyển thành `FAILED`.
- Khi nhiệm vụ không thuộc phạm vi đánh giá của Exercise, trạng thái có thể được xác định là `NOT_APPLICABLE`.

### 5.2. Status Handling

`PENDING` và `IN_PROGRESS` không được tự động chuyển thành `FAILED` chỉ vì Exercise kết thúc.

Khi Exercise kết thúc:

- Assessment Engine tiếp tục giữ nguyên trạng thái nếu chưa đủ căn cứ đánh giá.
- Những nhiệm vụ đã đáp ứng điều kiện được giữ trạng thái `COMPLETED`.
- Những nhiệm vụ đã được đánh giá không đạt được giữ trạng thái `FAILED`.
- Những nhiệm vụ không thuộc phạm vi Exercise được giữ trạng thái `NOT_APPLICABLE`.
- Những nhiệm vụ chưa có kết quả xác định vẫn có thể ở `PENDING` hoặc `IN_PROGRESS`.

Việc hoàn tất đánh giá cuối cùng cần xử lý các kết quả chưa xác định, thay vì tự động xem chúng là thất bại.

### 5.3. NOT_APPLICABLE Rules

`NOT_APPLICABLE` được sử dụng khi một Defensive Task không cần áp dụng cho Exercise hiện tại.

Ví dụ:

- Exercise không yêu cầu cô lập host.
- Exercise không có nhiệm vụ khôi phục service.
- Một nhiệm vụ không thuộc phạm vi của Scenario được cấu hình.

Quy tắc:

- Không sử dụng `NOT_APPLICABLE` chỉ vì người tham gia chưa hoàn thành nhiệm vụ.
- Không sử dụng `NOT_APPLICABLE` để thay thế `FAILED`.
- Không cho phép Client tự ý thay đổi trạng thái để tăng điểm.
- Việc xác định phải dựa trên cấu hình Exercise hoặc quyết định được Backend kiểm soát.
- Mọi thay đổi trạng thái quan trọng phải có khả năng truy xuất và audit.

Trạng thái được xác định bởi Backend và Assessment Engine.

Client không được tự gửi trạng thái `COMPLETED` để yêu cầu hệ thống cộng điểm.

## 6. Event, Evidence, Assessment Result and Score

Các khái niệm phải được phân biệt rõ ràng.

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
   | Result of evaluating a task
   v
SCORE
```

### 6.1. Event

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

### 6.2. Evidence

Evidence là thông tin được xác định là có giá trị cho quá trình đánh giá.

Ví dụ:

```text
Event:
Multiple SSH authentication failures

       |

       v

Evidence:
Possible SSH brute-force activity
```

Evidence được sử dụng làm cơ sở để Assessment Engine đánh giá Criteria và Defensive Tasks.

### 6.3. Assessment Result

Assessment Result là kết quả sau khi Assessment Engine kiểm tra Evidence và các điều kiện của nhiệm vụ.

Ví dụ:

```text
Evidence:
SSH brute-force activity detected

       |

       v

Assessment:

Detect Brute Force
= COMPLETED
```

### 6.4. Score

Score là kết quả tính điểm dựa trên Assessment Result và trọng số của Defensive Task.

Ví dụ:

```text
Assessment Result:
COMPLETED

       |

       v

Scoring Engine

       |

       v

Awarded Score:
10 / 10
```

## 7. Assessment Criteria

Assessment Criteria định nghĩa các điều kiện được sử dụng để đánh giá một Defensive Task.

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

| Defensive Task | Assessment Criterion |
|---|---|
| Detect Port Scan | Port scanning activity detected |
| Detect Brute Force | SSH brute-force activity detected |
| Detect Web Attack | Web attack activity detected |
| Identify Compromised Host | Correct compromised host identified |
| Isolate Attacked Host | Attacked host successfully isolated |
| Recover Service | Target service successfully restored |
| Incident Report | Incident report submitted with required information |

Một Defensive Task có thể được đánh giá thông qua một hoặc nhiều Criteria.

Assessment Criteria xác định điều kiện đánh giá, không phải kết quả cuối cùng.

Các Criteria bắt buộc của một Task phải được đáp ứng theo Rule đã định nghĩa để Task được đánh giá là `COMPLETED`.

## 8. Rule-based Assessment

V1 sử dụng Rule-based Assessment.

Một Rule có thể kiểm tra:

- Sự xuất hiện của Event.
- Sự tồn tại của Evidence.
- Mối quan hệ giữa các Event.
- Trạng thái của Asset.
- Trạng thái của Service.
- Hành động phòng thủ của Blue Team.
- Nội dung và trạng thái của Incident Report.

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

## 9. Defensive Task Assessment

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

Nếu toàn bộ điều kiện bắt buộc được thỏa mãn:

```text
Required Criteria = SATISFIED
           |
           v
Task = COMPLETED
```

Nếu nhiệm vụ đã được đánh giá nhưng điều kiện bắt buộc không được đáp ứng:

```text
Required Criteria = NOT SATISFIED
           |
           v
Task = FAILED
```

Nếu chưa đủ thông tin để xác định kết quả:

```text
Task = PENDING
```

Nếu nhiệm vụ đang được thực hiện:

```text
Task = IN_PROGRESS
```

Nếu nhiệm vụ không áp dụng:

```text
Task = NOT_APPLICABLE
```

Không sử dụng `NOT_COMPLETED` để biểu diễn trường hợp Exercise kết thúc nhưng Task chưa hoàn thành.

## 10. Detection Time

Detection Time là thời gian từ khi sự kiện hoặc hoạt động cần phát hiện bắt đầu đến khi Blue Team hoàn thành điều kiện phát hiện tương ứng.

```text
T_event
   |
   | Event occurs
   |
   v
T_detection
   |
   | Detection criterion satisfied
```

Công thức:

```text
Detection Time = T_detection - T_event
```

Ví dụ:

```text
Event occurs:
10:00:00

Blue Team detection:
10:03:10

Detection Time:
190 seconds
```

Detection Time được lưu phục vụ đánh giá và báo cáo.

Thời điểm xảy ra Event phải được phân biệt với thời điểm Event được thu thập hoặc xử lý.

## 11. Response Time

Response Time là thời gian từ khi Blue Team phát hiện sự cố đến khi hành động phản ứng tương ứng được xác nhận.

```text
T_detection
   |
   | Detection completed
   |
   v
T_response
   |
   | Defensive response verified
```

Công thức:

```text
Response Time = T_response - T_detection
```

Ví dụ:

```text
Detection:
10:03:10

Verified response:
10:06:00

Response Time:
170 seconds
```

Detection Time và Response Time là hai chỉ số khác nhau.

```text
Detection Time != Response Time
```

Thời điểm phản ứng phải dựa trên kết quả hành động đã được xác minh, không chỉ dựa trên thời điểm Client gửi yêu cầu.

Các chỉ số Isolation Time và Recovery Time được mô tả chi tiết trong:

`docs/assessment/metrics.md`

## 12. Assessment Workflow

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
4. Evidence is generated / identified
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
9. Assessment results are reviewed and finalized
        |
        v
10. Scoring Engine calculates final score
        |
        v
11. Final result is generated
```

Assessment Result chưa xác định không được tự động chuyển thành `FAILED` khi Exercise kết thúc.

## 13. Scoring Engine

Scoring Engine chịu trách nhiệm tính điểm dựa trên kết quả Assessment.

Scoring Engine:

- Không trực tiếp phát hiện tấn công.
- Không tự xác định Blue Team đã hoàn thành nhiệm vụ.
- Không nhận tổng điểm do Client tự gửi.
- Không cộng điểm nhiều lần cho cùng một Defensive Task.
- Không tính điểm cho Task có trạng thái `NOT_APPLICABLE`.

```text
Assessment Engine
       |
       | Assessment Result
       v
Scoring Engine
       |
       v
Assessment Score
```

Assessment Engine xác định kết quả.

Scoring Engine tính điểm từ kết quả đó.

## 14. V1 Scoring Model

Tổng trọng số tối đa của V1 là **100 điểm**, khi tất cả Defensive Tasks đều áp dụng.

| Defensive Task | Maximum Score |
|---|---:|
| Detect Port Scan | 10 |
| Detect Brute Force | 10 |
| Detect Web Attack | 15 |
| Identify Compromised Host | 15 |
| Isolate Attacked Host | 20 |
| Recover Service | 20 |
| Incident Report | 10 |
| **Total** | **100** |

Các trọng số được gán cho Defensive Task.

Assessment Criteria được sử dụng để xác định Task có đạt hay không.

Không cộng điểm riêng cho từng Event hoặc Evidence nếu chúng cùng phục vụ một Task.

## 15. Task Scoring Rules

V1 sử dụng mô hình đánh giá đạt/không đạt ở cấp Defensive Task.

| Assessment Result | Earned Points | Applicable Maximum Points |
|---|---:|---:|
| `COMPLETED` | Full task points | Full task points |
| `FAILED` | 0 | Full task points |
| `PENDING` | Not finalized | Full task points |
| `IN_PROGRESS` | Not finalized | Full task points |
| `NOT_APPLICABLE` | Excluded | Excluded |

Quy tắc:

1. Task `COMPLETED` nhận toàn bộ điểm tối đa.
2. Task `FAILED` nhận 0 điểm.
3. Task `PENDING` chưa có điểm cuối cùng.
4. Task `IN_PROGRESS` chưa có điểm cuối cùng.
5. Task `NOT_APPLICABLE` không tham gia tính điểm.
6. Một Task không được cộng điểm nhiều lần ngoài Rule đã định nghĩa.
7. Awarded Points không được vượt quá Maximum Points.
8. Không tự động xem Task chưa hoàn thành là `FAILED` chỉ vì Exercise kết thúc.

V1 không sử dụng Partial Scoring ở cấp Defensive Task.

Partial Scoring có thể được xem xét trong các phiên bản sau nếu yêu cầu đánh giá cần chi tiết hơn.

## 16. Applicable Maximum Score and Final Score

### 16.1. Total Maximum Score

Tổng Maximum Score mặc định của V1 là 100 điểm.

### 16.2. Applicable Maximum Score

Applicable Maximum Score là tổng điểm tối đa của các Defensive Task được áp dụng trong Exercise.

Các Task có trạng thái `NOT_APPLICABLE` phải được loại khỏi tổng điểm tối đa.

Công thức:

```text
Applicable Maximum Score
=
Sum of Maximum Points
for all applicable Defensive Tasks
```

### 16.3. Total Earned Points

Total Earned Points là tổng số điểm của các Task đã được đánh giá và hoàn thành.

```text
Total Earned Points
=
Sum of Earned Points
for COMPLETED Defensive Tasks
```

Task `FAILED` nhận 0 điểm.

Task `PENDING` và `IN_PROGRESS` chưa được xem là kết quả cuối cùng.

### 16.4. Final Score Formula

```text
Final Score =
(Total Earned Points / Applicable Maximum Score) * 100
```

Điểm cuối cùng được chuẩn hóa về thang điểm 100.

### 16.5. Example: NOT_APPLICABLE

Giả sử kết quả đánh giá:

| Defensive Task | Maximum Points | Status | Earned Points |
|---|---:|---|---:|
| Detect Port Scan | 10 | COMPLETED | 10 |
| Detect Brute Force | 10 | COMPLETED | 10 |
| Detect Web Attack | 15 | FAILED | 0 |
| Identify Compromised Host | 15 | COMPLETED | 15 |
| Isolate Attacked Host | 20 | NOT_APPLICABLE | Excluded |
| Recover Service | 20 | COMPLETED | 20 |
| Incident Report | 10 | FAILED | 0 |

Applicable Maximum Score:

```text
10 + 10 + 15 + 15 + 20 + 10 = 80
```

Total Earned Points:

```text
10 + 10 + 0 + 15 + 20 + 0 = 55
```

Final Score:

```text
(55 / 80) * 100 = 68.75
```

Kết quả:

```text
Final Score = 68.75 / 100
```

Task `Isolate Attacked Host` không làm giảm điểm vì nó không thuộc phạm vi đánh giá của Exercise.

### 16.6. No Applicable Tasks

Nếu tất cả Task đều có trạng thái `NOT_APPLICABLE`:

```text
Applicable Maximum Score = 0
```

Hệ thống không được thực hiện phép chia cho 0.

Final Score phải được biểu diễn là unavailable hoặc chưa có điểm, thay vì tự động gán 0.

## 17. Provisional Score and Final Score

Hệ thống phải phân biệt điểm tạm thời và điểm cuối cùng.

### 17.1. Provisional Score

Provisional Score được sử dụng khi vẫn còn Task ở trạng thái:

- `PENDING`
- `IN_PROGRESS`

Điểm này chỉ mang tính tham khảo trong quá trình Exercise.

Hệ thống không được tự động xem những Task chưa có kết quả là `FAILED`.

### 17.2. Final Score

Final Score chỉ được xác định khi tất cả Task thuộc phạm vi đánh giá đã có trạng thái cuối cùng:

- `COMPLETED`
- `FAILED`
- `NOT_APPLICABLE`

Nếu còn Task ở trạng thái `PENDING` hoặc `IN_PROGRESS`, kết quả chưa đủ điều kiện để được xem là Final Score.

White Team có thể review các kết quả theo quyền được phân công.

Mọi thay đổi kết quả sau khi đánh giá phải được Backend kiểm soát và có khả năng audit.

## 18. Scoring Rules

### 18.1. Server-side Scoring

Điểm phải được tính tại Backend.

Client không được gửi tổng điểm để Backend lưu trực tiếp.

```text
Client
   |
   | User action / submitted information
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

### 18.2. Evidence-based Scoring

Mỗi điểm được trao phải có cơ sở từ Assessment Result và Evidence tương ứng.

Không được cộng điểm chỉ dựa trên dữ liệu do Client tự khai báo.

### 18.3. Maximum Score

Điểm được trao không được vượt quá điểm tối đa của Task.

```text
Awarded Points <= Maximum Points
```

### 18.4. No Duplicate Scoring

Một Defensive Task không được cộng điểm nhiều lần ngoài phạm vi Rule đã định nghĩa.

Ví dụ:

```text
Detect Brute Force = 10 points
```

Không được cộng thêm 10 điểm chỉ vì hệ thống phát hiện nhiều Event khác nhau cùng thuộc một Task.

### 18.5. Assessment Before Scoring

Assessment phải được thực hiện trước khi tính điểm.

Không sử dụng trực tiếp:

```text
Event -> Score
```

Mà sử dụng:

```text
Event
   |
   v
Evidence
   |
   v
Assessment
   |
   v
Assessment Result
   |
   v
Score
```

### 18.6. NOT_APPLICABLE Exclusion

Task có trạng thái `NOT_APPLICABLE` phải được loại khỏi:

- Total Earned Points.
- Applicable Maximum Score.

Không được tính Task `NOT_APPLICABLE` là 0 điểm trong mẫu số của công thức Final Score.

## 19. Assessment Score Data

Kết quả điểm được lưu trong `assessment_scores`.

Các thông tin chính theo thiết kế dữ liệu hiện tại:

| Field | Description |
|---|---|
| `assessment_result_id` | Assessment Result tương ứng |
| `criterion_id` | Criterion được đánh giá |
| `awarded_score` | Điểm thực tế được trao |
| `max_score` | Điểm tối đa tại thời điểm đánh giá |
| `reason` | Lý do hoặc giải thích kết quả |

`max_score` được lưu cùng kết quả để giữ lại thông tin về trọng số tại thời điểm đánh giá.

Lưu ý:

- Điểm phải được tổng hợp theo Defensive Task.
- Không được cộng trùng điểm khi một Task có nhiều Criteria.
- Tổng điểm của một Task không được vượt quá trọng số của Task đó.
- Việc ánh xạ giữa `assessment_results`, `assessment_criteria` và `assessment_scores` phải tuân theo Database Schema hiện có.

Không tự ý thay đổi cấu trúc bảng chỉ dựa trên tài liệu nghiệp vụ này.

## 20. Assessment Result Data

`assessment_results` lưu kết quả đánh giá của từng Defensive Task.

Các thông tin nghiệp vụ chính:

| Field | Description |
|---|---|
| `assessment_id` | Assessment |
| `defensive_task_id` | Defensive Task |
| `status` | Trạng thái nhiệm vụ |
| `detected_at` | Thời điểm phát hiện |
| `responded_at` | Thời điểm phản ứng |
| `completed_at` | Thời điểm hoàn thành |
| `detection_time` | Thời gian phát hiện |
| `response_time` | Thời gian phản ứng |
| `reason` | Lý do kết quả |

Tên cột và kiểu dữ liệu thực tế phải được đối chiếu với SQL Schema.

`detection_time` và `response_time` được lưu dưới dạng khoảng thời gian, ví dụ số giây, nếu schema hỗ trợ.

## 21. Example: SSH Brute Force

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
Maximum Points = 10
Earned Points = 10
```

## 22. Example: Host Isolation

Defensive Task:

```text
Isolate Attacked Host
```

Assessment Engine kiểm tra trạng thái của target host.

Nếu Evidence xác nhận host đã được cô lập:

```text
Criterion:
Target host isolated

       |

       v

SATISFIED

       |

       v

Task:
COMPLETED
```

Scoring Engine:

```text
Maximum Points = 20
Earned Points = 20
```

## 23. Example: Service Recovery

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
Maximum Points = 20
Earned Points = 20
```

## 24. Incident Report Assessment

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

Criterion có thể kiểm tra:

- Báo cáo đã được gửi.
- Các thông tin bắt buộc đã được cung cấp.
- Nội dung đáp ứng điều kiện đánh giá đã định nghĩa.

White Team có thể review nội dung báo cáo.

Incident Report có trọng số 10 điểm trong rubric V1 cho mỗi Exercise, không phải 10 điểm riêng cho từng Scenario được sử dụng trong cùng Exercise.

## 25. White Team and Assessment

Assessment Engine không thay thế White Team.

White Team vẫn có vai trò:

- Theo dõi Exercise.
- Kiểm tra tiến trình.
- Xem Event và Evidence.
- Xem Assessment Result.
- Review Incident Report.
- Kiểm tra kết quả đánh giá.
- Phục vụ quá trình tổng kết Exercise.

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

Quyền review và quyền thay đổi kết quả phải tuân theo Security/RBAC Specification.

## 26. Database Mapping

Assessment và Scoring sử dụng các Entity đã được định nghĩa trong Database Design.

Scenario và Defensive Task:

```text
scenarios
    |
    v
defensive_tasks
    |
    v
assessment_criteria
```

Exercise:

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

Các quan hệ và khóa ngoại phải tuân theo Database Schema hiện có.

## 27. Assessment and Scoring Flow

Toàn bộ quy trình V1:

```text
                       SCENARIO
                          |
                          v
                       EXERCISE
                          |
               +----------+----------+
               |                     |
               v                     v
           RED TEAM              BLUE TEAM
               |                     |
               |                Defensive Tasks
               |                     |
               +----------+----------+
                          |
                          v
                        EVENTS
                          |
                          v
                       EVIDENCE
                          |
               +----------------------+
               |   ASSESSMENT ENGINE  |
               +----------+-----------+
                          |
                          v
                 ASSESSMENT RESULT
                          |
               +----------+----------+
               |                     |
               v                     v
        Detection Time         Response Time
               |                     |
               +----------+----------+
                          |
                          v
               +----------------------+
               |    SCORING ENGINE    |
               +----------+-----------+
                          |
                          v
                    TASK SCORES
                          |
                          v
                    FINAL SCORE
                          |
                          v
                        REPORT
```

## 28. Example Final Result

Một Exercise có thể tạo kết quả:

```text
Exercise: Blue Team Training

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

Trong ví dụ này, tất cả Task đều áp dụng.

Ngoài điểm số, hệ thống có thể hiển thị:

```text
Detection Time
Response Time
Task Status
Evidence
Assessment Reason
Incident Report
```

Nếu một Task có trạng thái `NOT_APPLICABLE`, điểm tối đa của Task đó phải được loại khỏi mẫu số khi tính Final Score.

## 29. Explainability

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

## 30. V1 Scenario Mapping

Các Scenario V1:

```text
Port Scanning
SSH Brute Force
Web Attack
```

Các nhóm Defensive Task:

```text
Detect Port Scan
Detect Brute Force
Detect Web Attack
Identify Compromised Host
Isolate Attacked Host
Recover Service
Incident Report
```

Assessment Engine sử dụng cùng một pipeline:

```text
Scenario
   |
   v
Defensive Task
   |
   v
Assessment Criteria
   |
   v
Evidence
   |
   v
Assessment Result
   |
   v
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

## 31. V1 Limitations

V1 có các giới hạn:

- Assessment sử dụng Rule-based Assessment.
- Scoring sử dụng Rule-based Scoring.
- Chưa sử dụng AI/ML.
- Chưa có Behavior-based Assessment phức tạp.
- Chưa có Adaptive Scoring.
- Chưa có Automated Incident Response.
- Chưa đánh giá toàn diện kỹ năng Blue Team ngoài các Criteria đã định nghĩa.
- Chưa sử dụng Partial Scoring ở cấp Defensive Task.
- Chi tiết Event/Evidence Rule phụ thuộc vào từng Scenario.

Các giới hạn này phù hợp với phạm vi Cyber Range quy mô nhỏ của V1.

## 32. Extensibility

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

- Privilege Escalation.
- Persistence.
- Windows Server.
- Active Directory.
- Additional Web Attacks.
- Additional Defensive Tasks.
- Individual Assessment.
- Team Assessment.
- Automated Response.
- AI/ML-based Assessment.

## 33. Separation of Responsibilities

| Component | Responsibility |
|---|---|
| Cyber Range | Cung cấp môi trường thực hành |
| Event Collector | Thu thập Event |
| Evidence Processing | Xử lý và chuẩn hóa Evidence |
| Assessment Engine | Xác định nhiệm vụ và Criteria có đạt hay không |
| Scoring Engine | Tính điểm |
| Web Platform | Hiển thị và quản lý dữ liệu |
| White Team | Giám sát và review |
| Report Module | Tổng hợp kết quả |

Các Component không được thay thế trách nhiệm của nhau.

## 34. Design Principles

1. Assessment và Scoring là hai trách nhiệm riêng biệt.
2. Assessment phải dựa trên Evidence có thể quan sát được.
3. Event không đồng nghĩa với Evidence.
4. Evidence không đồng nghĩa với Assessment Result.
5. Assessment Result không đồng nghĩa với Score.
6. Điểm phải được tính ở Backend.
7. Client không được tự quyết định Assessment Result.
8. Client không được tự quyết định Score.
9. V1 chỉ sử dụng 5 trạng thái Assessment Result đã định nghĩa.
10. `NOT_APPLICABLE` phải được loại khỏi Applicable Maximum Score.
11. `PENDING` và `IN_PROGRESS` không tự động được xem là `FAILED`.
12. Final Score chỉ được xác định khi các kết quả thuộc phạm vi đánh giá đã được hoàn tất.
13. Detection Time và Response Time phải được định nghĩa rõ ràng.
14. V1 sử dụng Rule-based Assessment.
15. V1 sử dụng Rule-based Scoring.
16. White Team vẫn giữ vai trò giám sát và review.
17. Một Defensive Task không được cộng điểm trùng ngoài Rule cho phép.
18. Awarded Points không được vượt Maximum Points.
19. Assessment Result phải có khả năng truy xuất Evidence làm cơ sở.
20. Các Scenario mới phải có khả năng sử dụng lại Assessment/Scoring Pipeline.
21. Assessment và Scoring phải phù hợp với Database Design.
22. Assessment và Scoring phải phù hợp với API Contract.
23. Chi tiết Rule của từng Scenario được định nghĩa tại tài liệu Scenario tương ứng.

## 35. Final Architecture

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
                 |    SCORING ENGINE    |
                 |                      |
                 | Task Score           |
                 | Earned Points        |
                 | Applicable Max Score |
                 | Final Score          |
                 +----------+-----------+
                            |
                            v
                       FINAL RESULT
                            |
               +------------+------------+
               |                         |
               v                         v
             REPORT              WHITE TEAM REVIEW
```

## 36. Final V1 Definition

Assessment Engine của hệ thống có nhiệm vụ:

> **Tự động xác định mức độ hoàn thành các nhiệm vụ phòng thủ của Blue Team dựa trên Event, Evidence, trạng thái hệ thống và Assessment Criteria.**

Scoring Engine có nhiệm vụ:

> **Tính điểm dựa trên Assessment Result và trọng số của các Defensive Task đã được định nghĩa. Các nhiệm vụ `NOT_APPLICABLE` được loại khỏi điểm tối đa có thể đạt.**

Pipeline chính của hệ thống:

```text
EVENT
   |
   v
EVIDENCE
   |
   v
ASSESSMENT
   |
   v
ASSESSMENT RESULT
   |
   v
SCORING
   |
   v
ASSESSMENT SCORE
   |
   v
FINAL SCORE
   |
   v
REPORT
```

Đây là pipeline chuẩn được sử dụng làm cơ sở cho Database, API, Backend Implementation và các Scenario của Cyber Range.