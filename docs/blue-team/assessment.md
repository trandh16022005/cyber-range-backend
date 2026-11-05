# Blue Team Assessment

## 1. Mục đích

Tài liệu này mô tả nghiệp vụ đánh giá (Assessment) khả năng phòng thủ của Blue Team trong hệ thống Cyber Range.

Assessment là quá trình kiểm tra và xác định mức độ hoàn thành các nhiệm vụ phòng thủ của Blue Team dựa trên dữ liệu, sự kiện và bằng chứng được ghi nhận trong quá trình thực hiện Exercise.

Mục tiêu của Assessment:

- Đánh giá khả năng phát hiện các hành vi tấn công.
- Đánh giá khả năng xác định máy chủ bị ảnh hưởng.
- Đánh giá khả năng cô lập máy chủ bị tấn công.
- Đánh giá khả năng khôi phục dịch vụ.
- Đánh giá khả năng lập báo cáo sự cố.
- Cung cấp kết quả đánh giá cho Scoring Engine để tính điểm.

Assessment Engine chịu trách nhiệm xác định kết quả hoàn thành nhiệm vụ, không trực tiếp tính điểm tổng kết của Blue Team.

## 2. Assessment Overview

### 2.1. Assessment trong Cyber Range

Trong hệ thống Cyber Range, Assessment là thành phần nằm giữa quá trình thực hiện nhiệm vụ của Blue Team và quá trình tính điểm.

Quy trình tổng quát:

1. Scenario xác định các tình huống và nhiệm vụ phòng thủ.
2. Cyber Range tạo môi trường thực hành.
3. Red Team thực hiện các hoạt động theo Scenario.
4. Event Collection thu thập sự kiện từ môi trường.
5. Blue Team thực hiện các nhiệm vụ phát hiện và xử lý.
6. Evidence được ghi nhận trong quá trình thực hiện.
7. Assessment Engine kiểm tra kết quả từng nhiệm vụ.
8. Assessment Result được lưu trữ.
9. Scoring Engine sử dụng Assessment Result để tính điểm.
10. Hệ thống hiển thị kết quả cho người dùng có quyền phù hợp.

### 2.2. Phân biệt Assessment Engine và Scoring Engine

Hai thành phần có trách nhiệm khác nhau:

| Thành phần | Trách nhiệm |
|---|---|
| Assessment Engine | Kiểm tra điều kiện hoàn thành nhiệm vụ và xác định Assessment Result |
| Scoring Engine | Tính điểm dựa trên Assessment Result và trọng số nhiệm vụ |

Ví dụ:

- Assessment Engine xác định nhiệm vụ phát hiện Brute Force đã hoàn thành hay chưa.
- Scoring Engine sử dụng kết quả đó để tính điểm tương ứng.

Assessment Engine không tự quyết định điểm tổng kết của Exercise.

## 3. Assessment Tasks

Trong phiên bản V1, hệ thống sử dụng 7 nhiệm vụ để đánh giá khả năng phòng thủ của Blue Team.

Tổng điểm tối đa là 100 điểm.

| STT | Nhiệm vụ | Điểm tối đa |
|---|---|---:|
| 1 | Detect Port Scan | 10 |
| 2 | Detect Brute Force | 10 |
| 3 | Detect Web Attack | 15 |
| 4 | Identify Compromised Host | 15 |
| 5 | Isolate Attacked Host | 20 |
| 6 | Recover Service | 20 |
| 7 | Incident Report | 10 |
| | **Tổng cộng** | **100** |

### 3.1. Detect Port Scan

**Mục tiêu:** Đánh giá khả năng phát hiện hoạt động quét cổng trong môi trường Cyber Range.

Dữ liệu đầu vào:

- Network Events.
- Địa chỉ IP nguồn và đích.
- Thông tin cổng dịch vụ.
- Thời điểm phát sinh sự kiện.
- Evidence liên quan.

Hành động mong đợi:

- Blue Team nhận diện được dấu hiệu quét cổng.
- Xác định nguồn phát sinh hoạt động đáng ngờ.
- Xác định Asset liên quan.
- Ghi nhận kết quả phát hiện.

Điều kiện hoàn thành:

- Có kết quả phát hiện phù hợp với Scenario.
- Xác định đúng Asset liên quan theo tiêu chí đánh giá.
- Có dữ liệu hoặc Evidence xác minh.

Điểm tối đa: **10 điểm**.

### 3.2. Detect Brute Force

**Mục tiêu:** Đánh giá khả năng phát hiện hoạt động đăng nhập bất thường hoặc có dấu hiệu Brute Force.

Dữ liệu đầu vào:

- Authentication Logs.
- SSH Logs.
- Địa chỉ IP nguồn.
- Tài khoản liên quan.
- Thời điểm đăng nhập.
- Evidence liên quan.

Hành động mong đợi:

- Blue Team nhận diện các lần đăng nhập thất bại bất thường.
- Xác định tài khoản hoặc máy chủ bị nhắm mục tiêu.
- Ghi nhận kết quả phát hiện.

Điều kiện hoàn thành:

- Có kết quả phát hiện phù hợp với Scenario.
- Xác định đúng thông tin liên quan theo tiêu chí đánh giá.
- Có dữ liệu hoặc Evidence xác minh.

Điểm tối đa: **10 điểm**.

### 3.3. Detect Web Attack

**Mục tiêu:** Đánh giá khả năng phát hiện các hành vi tấn công Web trong phạm vi Scenario.

Dữ liệu đầu vào:

- Web Access Logs.
- HTTP method.
- URL được truy cập.
- HTTP response status.
- Địa chỉ IP nguồn.
- Evidence liên quan.

Hành động mong đợi:

- Blue Team nhận diện request có dấu hiệu tấn công.
- Xác định Web Server bị nhắm mục tiêu.
- Ghi nhận kết quả phát hiện.

Điều kiện hoàn thành:

- Phát hiện đúng hành vi theo Scenario.
- Xác định đúng Web Server liên quan.
- Có dữ liệu hoặc Evidence xác minh.

Điểm tối đa: **15 điểm**.

### 3.4. Identify Compromised Host

**Mục tiêu:** Đánh giá khả năng xác định máy chủ có dấu hiệu bị xâm nhập hoặc bị ảnh hưởng bởi hoạt động tấn công.

Dữ liệu đầu vào:

- Detection Result.
- Authentication Logs.
- SSH Logs.
- System Events.
- Network Events.
- Asset Status.
- Evidence liên quan.

Hành động mong đợi:

- Blue Team phân tích các sự kiện liên quan.
- Xác định máy chủ bị ảnh hưởng.
- Ghi nhận kết quả nhận diện.

Điều kiện hoàn thành:

- Xác định đúng Asset theo Scenario.
- Có bằng chứng hỗ trợ kết quả nhận diện.
- Kết quả được ghi nhận trong hệ thống.

Điểm tối đa: **15 điểm**.

### 3.5. Isolate Attacked Host

**Mục tiêu:** Đánh giá khả năng cô lập máy chủ bị tấn công nhằm hạn chế ảnh hưởng của sự cố.

Dữ liệu đầu vào:

- Asset được xác định là bị ảnh hưởng.
- Thông tin Exercise.
- Yêu cầu xử lý theo Scenario.
- Kết quả thực hiện hành động cô lập.

Hành động mong đợi:

- Blue Team xác định đúng máy chủ cần cô lập.
- Thực hiện hành động cô lập theo cơ chế của Cyber Range.
- Kiểm tra kết quả cô lập.

Điều kiện hoàn thành:

- Cô lập đúng Asset theo Scenario.
- Hành động cô lập được thực hiện thành công.
- Trạng thái thực tế của Asset phù hợp với kết quả cô lập.
- Có Evidence xác minh.

Điểm tối đa: **20 điểm**.

### 3.6. Recover Service

**Mục tiêu:** Đánh giá khả năng khôi phục dịch vụ sau khi sự cố đã được kiểm soát.

Dữ liệu đầu vào:

- Thông tin Service bị ảnh hưởng.
- Asset cung cấp Service.
- Trạng thái Service trước khi khôi phục.
- Kết quả thực hiện khôi phục.
- Evidence liên quan.

Hành động mong đợi:

- Blue Team xác định đúng Service cần khôi phục.
- Thực hiện khôi phục theo Scenario.
- Kiểm tra trạng thái hoạt động của Service.

Điều kiện hoàn thành:

- Khôi phục đúng Service theo Scenario.
- Service hoạt động trở lại theo yêu cầu.
- Kết quả được xác minh từ môi trường Cyber Range.
- Có Evidence xác minh.

Điểm tối đa: **20 điểm**.

### 3.7. Incident Report

**Mục tiêu:** Đánh giá khả năng tổng hợp và báo cáo quá trình xử lý sự cố của Blue Team.

Nội dung báo cáo:

- Mô tả sự cố.
- Loại sự cố.
- Asset bị ảnh hưởng.
- Thời điểm phát hiện.
- Kết quả phân tích.
- Hành động cô lập.
- Hành động khôi phục.
- Evidence liên quan.
- Kết quả xử lý cuối cùng.

Điều kiện hoàn thành:

- Báo cáo thuộc đúng Exercise.
- Có đầy đủ các thông tin bắt buộc theo Scenario.
- Nội dung phù hợp với dữ liệu và Evidence đã ghi nhận.
- Báo cáo được lưu thành công trong hệ thống.

Điểm tối đa: **10 điểm**.

## 4. Assessment Result

### 4.1. Mục đích

Assessment Result là kết quả đánh giá một nhiệm vụ cụ thể của Blue Team trong một Exercise.

Mỗi nhiệm vụ được đánh giá cần có kết quả riêng để hệ thống theo dõi tiến độ và tính điểm.

### 4.2. Assessment Result Status

Hệ thống sử dụng 5 trạng thái:

| Status | Ý nghĩa |
|---|---|
| `PENDING` | Nhiệm vụ đang chờ đánh giá hoặc chưa có kết quả đánh giá |
| `IN_PROGRESS` | Nhiệm vụ đang được thực hiện |
| `COMPLETED` | Nhiệm vụ đã đáp ứng điều kiện hoàn thành |
| `FAILED` | Nhiệm vụ không đạt điều kiện đánh giá |
| `NOT_APPLICABLE` | Nhiệm vụ không áp dụng cho Exercise hiện tại |

Không sử dụng trạng thái `NOT_COMPLETED`.

### 4.3. Quy tắc chuyển trạng thái

Các trạng thái được sử dụng theo nghiệp vụ:

- `PENDING`: Trạng thái ban đầu của nhiệm vụ.
- `IN_PROGRESS`: Khi nhiệm vụ bắt đầu được thực hiện.
- `COMPLETED`: Khi hệ thống xác minh nhiệm vụ đáp ứng đầy đủ điều kiện.
- `FAILED`: Khi kết quả đánh giá không đáp ứng điều kiện hoàn thành.
- `NOT_APPLICABLE`: Khi nhiệm vụ không thuộc phạm vi đánh giá của Exercise.

Việc chuyển trạng thái phải dựa trên dữ liệu và quy tắc nghiệp vụ, không chỉ dựa trên giá trị do Client gửi lên.

### 4.4. NOT_APPLICABLE

`NOT_APPLICABLE` được sử dụng khi một nhiệm vụ không áp dụng cho Exercise hiện tại.

Ví dụ:

Một Exercise chỉ yêu cầu phát hiện và phân tích sự kiện, không yêu cầu khôi phục dịch vụ. Khi đó, nhiệm vụ Recover Service có thể được đánh dấu `NOT_APPLICABLE`.

Quy tắc quan trọng:

- Nhiệm vụ `NOT_APPLICABLE` không được tính vào tổng điểm tối đa của Exercise.
- Nhiệm vụ `NOT_APPLICABLE` không được xem là nhiệm vụ thất bại.
- Hệ thống cần phân biệt nhiệm vụ không áp dụng với nhiệm vụ chưa hoàn thành.

## 5. Assessment Workflow

Quy trình đánh giá tổng quát:

1. Exercise được khởi tạo.
2. Hệ thống xác định các nhiệm vụ cần đánh giá theo Scenario.
3. Assessment Result được tạo với trạng thái ban đầu phù hợp.
4. Blue Team thực hiện các nhiệm vụ phòng thủ.
5. Event và Evidence được ghi nhận.
6. Assessment Engine tiếp nhận dữ liệu.
7. Assessment Engine kiểm tra điều kiện hoàn thành của từng nhiệm vụ.
8. Assessment Result được cập nhật.
9. Scoring Engine nhận kết quả đánh giá.
10. Hệ thống tính điểm và hiển thị kết quả.

### Sơ đồ quy trình

```text
Exercise
   |
   v
Scenario & Assessment Tasks
   |
   v
Create Assessment Results
   |
   v
Blue Team Activities
   |
   v
Event & Evidence Collection
   |
   v
Assessment Engine
   |
   v
Evaluate Task Conditions
   |
   v
Update Assessment Results
   |
   v
Scoring Engine
   |
   v
Calculate Final Score
   |
   v
Assessment Report
```

## 6. Assessment Criteria

### 6.1. Mục đích

Assessment Criteria là các tiêu chí dùng để xác định một nhiệm vụ đã đáp ứng yêu cầu đánh giá hay chưa.

Mỗi nhiệm vụ cần có tiêu chí rõ ràng để kết quả đánh giá có thể kiểm tra và xác minh.

### 6.2. Nguyên tắc xây dựng tiêu chí

- Tiêu chí phải phù hợp với Scenario.
- Tiêu chí cần có điều kiện hoàn thành rõ ràng.
- Tiêu chí phải có dữ liệu đầu vào để xác minh.
- Tiêu chí cần xác định Evidence liên quan.
- Không phụ thuộc hoàn toàn vào nội dung tự khai báo của người dùng.
- Có khả năng mở rộng cho các Scenario khác nhau.

### 6.3. Các thành phần của một tiêu chí

Một Assessment Criteria về mặt nghiệp vụ cần xác định:

| Thành phần | Ý nghĩa |
|---|---|
| Task | Nhiệm vụ được đánh giá |
| Description | Mô tả tiêu chí |
| Expected Result | Kết quả mong đợi |
| Verification Condition | Điều kiện xác minh |
| Evidence Requirement | Bằng chứng cần có |
| Applicable Scenario | Scenario áp dụng |

Các thành phần trên mô tả nghiệp vụ. Khi triển khai cần đối chiếu với cấu trúc bảng `assessment_criteria` hiện có trong Database.

## 7. Assessment Verification

### 7.1. Mục đích

Assessment Verification là quá trình kiểm tra dữ liệu và bằng chứng nhằm đảm bảo kết quả đánh giá phản ánh đúng hoạt động thực tế của Blue Team.

### 7.2. Dữ liệu xác minh

Tùy theo nhiệm vụ, Assessment Engine có thể sử dụng:

- Event.
- Evidence.
- Detection Result.
- Asset Status.
- Service Status.
- Incident Report.
- Timestamp của các hành động.
- Thông tin Exercise và Scenario.

### 7.3. Nguyên tắc xác minh

1. Kiểm tra dữ liệu thuộc đúng Exercise.
2. Kiểm tra nhiệm vụ có áp dụng cho Exercise hay không.
3. Kiểm tra dữ liệu đầu vào có hợp lệ hay không.
4. Kiểm tra Evidence có liên quan đến nhiệm vụ hay không.
5. Kiểm tra kết quả thực tế có đáp ứng tiêu chí hay không.
6. Ghi nhận kết quả và lý do đánh giá.

### 7.4. Trường hợp không đủ dữ liệu

Nếu hệ thống không có đủ dữ liệu để xác minh một nhiệm vụ:

- Không tự động kết luận nhiệm vụ đã hoàn thành.
- Không tự suy đoán kết quả.
- Ghi nhận tình trạng phù hợp theo quy trình đánh giá.
- Cung cấp thông tin để người có quyền kiểm tra khi cần thiết.

Việc thiếu dữ liệu không đồng nghĩa với `NOT_APPLICABLE`. Trạng thái này chỉ được sử dụng khi nhiệm vụ thực sự không áp dụng cho Exercise.

## 8. Scoring Integration

### 8.1. Mục đích

Sau khi Assessment Engine hoàn thành đánh giá, Scoring Engine sử dụng Assessment Result để tính điểm cho Blue Team.

### 8.2. Trọng số nhiệm vụ V1

| Nhiệm vụ | Điểm tối đa |
|---|---:|
| Detect Port Scan | 10 |
| Detect Brute Force | 10 |
| Detect Web Attack | 15 |
| Identify Compromised Host | 15 |
| Isolate Attacked Host | 20 |
| Recover Service | 20 |
| Incident Report | 10 |
| **Tổng cộng** | **100** |

### 8.3. Quy tắc tính điểm

Trong phiên bản V1, mỗi nhiệm vụ có điểm tối đa được xác định trước.

Quy tắc cơ bản:

- `COMPLETED`: Được tính điểm theo trọng số của nhiệm vụ.
- `FAILED`: Không được tính điểm hoàn thành cho nhiệm vụ.
- `PENDING`: Chưa có kết quả đánh giá cuối cùng.
- `IN_PROGRESS`: Nhiệm vụ đang được thực hiện.
- `NOT_APPLICABLE`: Loại khỏi tổng điểm tối đa của Exercise.

Công thức tính điểm:

`Final Score = (Total Earned Points / Applicable Maximum Points) * 100`

Trong đó:

- `Total Earned Points`: Tổng điểm các nhiệm vụ được đánh giá đạt.
- `Applicable Maximum Points`: Tổng điểm tối đa của các nhiệm vụ áp dụng cho Exercise.
- Các nhiệm vụ `NOT_APPLICABLE` không được tính vào Applicable Maximum Points.

Ví dụ:

Một Exercise có 7 nhiệm vụ nhưng nhiệm vụ Recover Service được đánh dấu `NOT_APPLICABLE`.

Khi đó:

- Tổng điểm tối đa ban đầu: 100.
- Điểm tối đa của Recover Service: 20.
- Applicable Maximum Points: 80.

Nếu Blue Team đạt 60 điểm trên các nhiệm vụ áp dụng:

`Final Score = (60 / 80) * 100 = 75`

Kết quả cuối cùng là 75/100.

Lưu ý: Nếu Exercise chưa có kết quả đánh giá đầy đủ, hệ thống cần phân biệt điểm tạm thời với điểm cuối cùng. Không nên coi nhiệm vụ `PENDING` hoặc `IN_PROGRESS` là nhiệm vụ đã thất bại.

## 9. Assessment Result và Database

Trong Database hiện tại, các bảng liên quan đến nghiệp vụ Assessment gồm:

| Bảng | Vai trò |
|---|---|
| `assessments` | Quản lý thông tin đánh giá |
| `assessment_results` | Lưu kết quả đánh giá nhiệm vụ |
| `assessment_criteria` | Quản lý tiêu chí đánh giá |
| `assessment_scores` | Lưu thông tin điểm số |
| `evidence` | Lưu bằng chứng phục vụ đánh giá |
| `events` | Lưu sự kiện từ Cyber Range |
| `exercises` | Quản lý Exercise |

Nguyên tắc triển khai:

- Assessment Result phải liên kết đúng Exercise.
- Kết quả đánh giá phải liên kết với nhiệm vụ hoặc tiêu chí tương ứng.
- Evidence cần được liên kết phù hợp với kết quả đánh giá.
- Điểm số phải được tính từ kết quả đánh giá phía Server.
- Client không được tự gửi điểm số để thay thế kết quả do hệ thống tính toán.
- Cần đảm bảo dữ liệu đánh giá giữa các Exercise được tách biệt.

Không tự ý tạo thêm bảng hoặc thay đổi cấu trúc Database khi chưa đối chiếu với `database.sql`.

## 10. V1 Scope

Phạm vi Assessment trong phiên bản V1:

| Chức năng | Mô tả |
|---|---|
| Assessment Task Management | Quản lý các nhiệm vụ đánh giá |
| Assessment Criteria | Quản lý tiêu chí đánh giá |
| Assessment Result | Ghi nhận kết quả từng nhiệm vụ |
| Status Management | Quản lý 5 trạng thái Assessment Result |
| Evidence Verification | Xác minh bằng chứng |
| Task Evaluation | Kiểm tra điều kiện hoàn thành |
| Scoring Integration | Cung cấp kết quả cho Scoring Engine |
| Final Score | Tính điểm tổng kết theo quy tắc V1 |
| Assessment Report | Hiển thị kết quả đánh giá |

Các chức năng nâng cao như đánh giá bằng Machine Learning, chấm điểm hành vi tự động phức tạp và phân tích năng lực dài hạn chưa thuộc phạm vi bắt buộc của V1.

## 11. Nguyên tắc triển khai Backend

Khi triển khai Assessment trong Backend, cần tuân thủ các nguyên tắc:

- Assessment Engine và Scoring Engine phải được tách biệt về trách nhiệm.
- Kết quả đánh giá phải được xác định từ dữ liệu phía Server.
- Không tin tưởng kết quả hoàn thành hoặc điểm số do Client tự gửi.
- Mỗi Assessment Result phải thuộc đúng Exercise.
- Chỉ đánh giá những nhiệm vụ thuộc phạm vi Scenario.
- `NOT_APPLICABLE` phải được loại khỏi tổng điểm tối đa.
- Không sử dụng `NOT_COMPLETED` làm Assessment Result Status.
- Cần phân biệt kết quả tạm thời và kết quả cuối cùng.
- Các thao tác cập nhật kết quả cần đảm bảo tính nhất quán dữ liệu.
- Các thao tác quan trọng cần được ghi nhận trong Audit Log.
- Phải kiểm tra quyền truy cập trước khi xem hoặc thay đổi dữ liệu đánh giá.

## 12. Kết luận

Assessment là thành phần chịu trách nhiệm đánh giá khả năng phòng thủ của Blue Team trong hệ thống Cyber Range.

Trong phiên bản V1, hệ thống sử dụng 7 nhiệm vụ với tổng trọng số 100 điểm, tập trung vào khả năng phát hiện tấn công, xác định máy chủ bị xâm nhập, cô lập máy chủ, khôi phục dịch vụ và lập báo cáo sự cố.

Assessment Engine kiểm tra điều kiện hoàn thành và ghi nhận Assessment Result. Scoring Engine sử dụng kết quả đó để tính điểm tổng kết.

Việc đánh giá phải dựa trên dữ liệu và bằng chứng có thể xác minh, đảm bảo kết quả phản ánh đúng quá trình thực hiện nhiệm vụ của Blue Team.