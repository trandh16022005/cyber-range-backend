# Blue Team Incident Response

## 1. Mục đích

Tài liệu này mô tả nghiệp vụ xử lý sự cố (Incident Response) của Blue Team trong hệ thống Cyber Range.

Incident Response là quá trình tiếp nhận thông tin cảnh báo, phân tích sự cố, thực hiện các hành động xử lý và khôi phục hệ thống về trạng thái hoạt động bình thường.

Mục tiêu của Incident Response:

- Tiếp nhận và xác minh các sự cố được phát hiện.
- Xác định phạm vi và mức độ ảnh hưởng của sự cố.
- Thực hiện các hành động ngăn chặn hoặc cô lập tài sản bị ảnh hưởng.
- Khôi phục dịch vụ sau khi sự cố được kiểm soát.
- Ghi nhận bằng chứng và kết quả xử lý.
- Cung cấp dữ liệu cho Assessment Engine để đánh giá khả năng phản ứng của Blue Team.

Incident Response tập trung vào quá trình xử lý sau khi phát hiện sự cố. Việc thu thập và phân tích sự kiện để phát hiện dấu hiệu tấn công thuộc phạm vi Detection.

## 2. Phạm vi xử lý sự cố

Trong phiên bản V1, Incident Response tập trung vào ba nhóm nghiệp vụ chính:

| Nghiệp vụ | Mục tiêu |
|---|---|
| Incident Identification | Xác định và phân tích sự cố |
| Host Isolation | Cô lập máy chủ bị ảnh hưởng |
| Service Recovery | Khôi phục dịch vụ sau sự cố |

Ngoài ra, hệ thống cần ghi nhận quá trình xử lý và tạo báo cáo sự cố để phục vụ đánh giá.

Các hành động xử lý cụ thể phải nằm trong phạm vi Scenario được cấu hình cho từng Exercise.

## 3. Incident Identification

### 3.1. Mục tiêu

Incident Identification là quá trình xác định một sự kiện có thực sự tạo thành sự cố bảo mật hay không, đồng thời đánh giá phạm vi ảnh hưởng.

Blue Team sử dụng dữ liệu từ Detection và Evidence để phân tích tình trạng hệ thống.

### 3.2. Dữ liệu đầu vào

Các thông tin được sử dụng trong quá trình xác định sự cố:

- Detection Result.
- Event liên quan.
- Evidence đã thu thập.
- Asset có dấu hiệu bị ảnh hưởng.
- Trạng thái Service.
- Thông tin Scenario và Exercise.

### 3.3. Nội dung phân tích

Blue Team cần xác định:

- Loại sự cố.
- Asset bị ảnh hưởng.
- Thời điểm xảy ra sự cố.
- Các sự kiện liên quan.
- Mức độ ảnh hưởng đến hệ thống.
- Dịch vụ có bị gián đoạn hay không.
- Bằng chứng hỗ trợ kết luận.

### 3.4. Kết quả

Sau khi phân tích, Blue Team ghi nhận kết quả xác định sự cố.

Thông tin nghiệp vụ cần có:

| Thông tin | Ý nghĩa |
|---|---|
| Incident Description | Mô tả sự cố |
| Incident Type | Loại sự cố |
| Related Exercise | Exercise liên quan |
| Affected Assets | Danh sách Asset bị ảnh hưởng |
| Detection Time | Thời điểm phát hiện |
| Evidence | Bằng chứng liên quan |
| Impact | Mức độ ảnh hưởng |
| Analysis Result | Kết quả phân tích |

Kết quả này là cơ sở để Blue Team lựa chọn hành động xử lý phù hợp.

## 4. Host Isolation

### 4.1. Mục tiêu

Host Isolation là quá trình cô lập máy chủ bị ảnh hưởng nhằm hạn chế phạm vi tác động của sự cố và ngăn tình trạng ảnh hưởng lan rộng trong môi trường Cyber Range.

Trong phiên bản V1, đây là một trong những nhiệm vụ quan trọng để đánh giá khả năng phản ứng của Blue Team.

### 4.2. Điều kiện thực hiện

Trước khi thực hiện cô lập, Blue Team cần:

- Xác định Asset cần cô lập.
- Có bằng chứng hoặc thông tin xác nhận Asset bị ảnh hưởng.
- Kiểm tra vai trò và quyền thực hiện.
- Xác định phạm vi tác động của hành động cô lập.
- Đảm bảo hành động phù hợp với Scenario hiện tại.

### 4.3. Hành động cô lập

Tùy theo cấu hình Cyber Range và Scenario, hành động cô lập có thể được thực hiện thông qua thành phần quản lý môi trường.

Về mặt nghiệp vụ, hệ thống cần ghi nhận:

- Asset được yêu cầu cô lập.
- Người thực hiện hành động.
- Thời điểm thực hiện.
- Kết quả thực hiện.
- Trạng thái Asset sau khi xử lý.
- Bằng chứng xác nhận kết quả.

Hệ thống không được chỉ dựa vào yêu cầu từ người dùng để kết luận Asset đã được cô lập thành công. Kết quả cần được xác minh từ trạng thái thực tế của môi trường hoặc nguồn dữ liệu đáng tin cậy.

### 4.4. Điều kiện hoàn thành

Nhiệm vụ Host Isolation được xem là hoàn thành khi:

1. Blue Team xác định đúng Asset cần cô lập.
2. Hành động cô lập được thực hiện thành công.
3. Trạng thái thực tế của Asset phản ánh kết quả cô lập.
4. Hệ thống ghi nhận đầy đủ thông tin và bằng chứng.

Nếu hành động thất bại hoặc không có đủ bằng chứng xác minh, hệ thống không được tự động ghi nhận nhiệm vụ hoàn thành.

## 5. Service Recovery

### 5.1. Mục tiêu

Service Recovery là quá trình khôi phục dịch vụ bị ảnh hưởng sau khi sự cố đã được kiểm soát.

Mục tiêu là đưa dịch vụ trở lại trạng thái hoạt động bình thường theo yêu cầu của Scenario.

### 5.2. Điều kiện thực hiện

Trước khi khôi phục dịch vụ, Blue Team cần:

- Xác định Service bị ảnh hưởng.
- Xác định Asset đang cung cấp Service.
- Đánh giá tình trạng hiện tại.
- Xác nhận điều kiện cho phép khôi phục theo Scenario.
- Đảm bảo quá trình khôi phục không làm ảnh hưởng đến các thành phần khác ngoài phạm vi cho phép.

### 5.3. Hành động khôi phục

Tùy theo Scenario, việc khôi phục có thể bao gồm:

- Đưa Service trở lại trạng thái hoạt động.
- Kiểm tra trạng thái Service.
- Kiểm tra khả năng truy cập.
- Xác nhận Service đáp ứng yêu cầu hoạt động của Scenario.

Hệ thống cần ghi nhận:

- Service được khôi phục.
- Asset cung cấp Service.
- Người thực hiện.
- Thời điểm bắt đầu khôi phục.
- Thời điểm hoàn thành.
- Kết quả kiểm tra Service.
- Bằng chứng xác nhận.

### 5.4. Điều kiện hoàn thành

Nhiệm vụ Service Recovery được xem là hoàn thành khi:

1. Service được xác định đúng.
2. Hành động khôi phục được thực hiện thành công.
3. Service đạt trạng thái hoạt động theo yêu cầu Scenario.
4. Kết quả được xác minh.
5. Hệ thống ghi nhận thông tin và bằng chứng.

Không được xem một Service đã khôi phục chỉ vì người dùng gửi yêu cầu khôi phục thành công. Cần có kết quả xác minh từ môi trường thực tế.

## 6. Incident Report

### 6.1. Mục tiêu

Incident Report là báo cáo tổng hợp quá trình phát hiện, phân tích và xử lý sự cố của Blue Team.

Báo cáo giúp giảng viên và hệ thống đánh giá theo dõi quá trình thực hiện nhiệm vụ, đồng thời cung cấp dữ liệu để đánh giá năng lực phòng thủ.

### 6.2. Nội dung báo cáo

Một Incident Report cần có các thông tin:

| Thông tin | Ý nghĩa |
|---|---|
| Exercise | Exercise liên quan |
| Incident Description | Mô tả sự cố |
| Incident Type | Loại sự cố |
| Affected Assets | Asset bị ảnh hưởng |
| Detection Time | Thời điểm phát hiện |
| Analysis | Kết quả phân tích |
| Isolation Action | Hành động cô lập |
| Recovery Action | Hành động khôi phục |
| Evidence | Bằng chứng liên quan |
| Final Result | Kết quả xử lý |
| Created By | Người tạo báo cáo |
| Created At | Thời điểm tạo báo cáo |

### 6.3. Yêu cầu đối với báo cáo

- Báo cáo phải thuộc đúng Exercise.
- Nội dung cần phản ánh quá trình xử lý thực tế.
- Các hành động cần có thông tin người thực hiện và thời điểm.
- Bằng chứng cần được liên kết phù hợp.
- Không cho phép người dùng tự khai báo kết quả để thay thế dữ liệu xác minh từ hệ thống.
- Báo cáo cần được lưu trữ để phục vụ đánh giá và truy xuất.

## 7. Response Evidence

Response Evidence là bằng chứng chứng minh Blue Team đã thực hiện các hành động xử lý sự cố.

Các loại bằng chứng có thể bao gồm:

- Kết quả cô lập Asset.
- Trạng thái Asset sau khi cô lập.
- Kết quả kiểm tra Service.
- Trạng thái Service sau khi khôi phục.
- Log ghi nhận hành động xử lý.
- Thông tin người thực hiện.
- Thời điểm thực hiện.
- Kết quả xác minh từ Cyber Range.

Nguyên tắc quản lý:

1. Evidence phải có nguồn gốc xác định.
2. Evidence phải liên kết với đúng Exercise.
3. Evidence cần liên kết với Asset hoặc Service liên quan nếu có.
4. Phân biệt bằng chứng do môi trường ghi nhận với nội dung do người dùng cung cấp.
5. Không sử dụng một yêu cầu hành động làm bằng chứng cho kết quả thực hiện thành công.
6. Bảo toàn thông tin thời gian và nguồn phát sinh bằng chứng.

## 8. Response Time và Recovery Time

Hệ thống cần ghi nhận các mốc thời gian để đánh giá tốc độ phản ứng và khả năng khôi phục của Blue Team.

### 8.1. Response Time

Response Time thể hiện khoảng thời gian từ khi Blue Team phát hiện sự cố đến khi bắt đầu thực hiện hành động phản ứng.

Các mốc thời gian:

- `detection_time`: Thời điểm phát hiện sự cố.
- `response_time`: Thời điểm bắt đầu hành động xử lý.

Công thức:

`Response Time = Response Timestamp - Detection Timestamp`

Response Time càng nhỏ thể hiện Blue Team bắt đầu phản ứng càng nhanh.

### 8.2. Recovery Time

Recovery Time thể hiện khoảng thời gian cần thiết để khôi phục dịch vụ sau khi bắt đầu quá trình khôi phục.

Các mốc thời gian:

- `recovery_start_time`: Thời điểm bắt đầu khôi phục.
- `recovery_complete_time`: Thời điểm xác minh khôi phục thành công.

Công thức:

`Recovery Time = Recovery Complete Timestamp - Recovery Start Timestamp`

### 8.3. Nguyên tắc ghi nhận thời gian

- Thời gian phải được ghi nhận thống nhất.
- Ưu tiên timestamp do hệ thống hoặc môi trường Cyber Range xác định.
- Không cho phép Client tự thay đổi timestamp của các hành động đã ghi nhận.
- Nếu thiếu dữ liệu thời gian thì không tự suy đoán kết quả.
- Cần phân biệt thời điểm gửi yêu cầu và thời điểm hành động thực sự hoàn thành.

Các chỉ số thời gian là dữ liệu đầu vào cho Assessment Engine và Scoring Engine.

## 9. Incident Response Workflow

Quy trình xử lý sự cố tổng quát:

1. Detection phát hiện dấu hiệu tấn công.
2. Hệ thống ghi nhận Event và Evidence.
3. Blue Team tiếp nhận thông tin sự cố.
4. Blue Team phân tích và xác định Asset bị ảnh hưởng.
5. Blue Team thực hiện Host Isolation nếu Scenario yêu cầu.
6. Hệ thống xác minh kết quả cô lập.
7. Blue Team thực hiện Service Recovery khi đủ điều kiện.
8. Hệ thống xác minh trạng thái Service.
9. Blue Team hoàn thiện Incident Report.
10. Assessment Engine đánh giá kết quả thực hiện.
11. Scoring Engine tính điểm dựa trên Assessment Result.

### Sơ đồ quy trình

```text
Detection
    |
    v
Event & Evidence Collection
    |
    v
Incident Identification
    |
    v
Impact Analysis
    |
    v
Host Isolation
    |
    v
Isolation Verification
    |
    v
Service Recovery
    |
    v
Recovery Verification
    |
    v
Incident Report
    |
    v
Assessment Engine
    |
    v
Scoring Engine
```

Lưu ý: Các bước Host Isolation và Service Recovery được thực hiện theo yêu cầu của từng Scenario. Không phải mọi sự cố đều bắt buộc phải thực hiện tất cả các hành động.

## 10. Incident Response và Assessment Engine

Incident Response cung cấp dữ liệu để Assessment Engine đánh giá khả năng xử lý sự cố của Blue Team.

Các nhiệm vụ được đánh giá trong V1:

| Nhiệm vụ | Điểm tối đa | Nội dung đánh giá |
|---|---:|---|
| Identify Compromised Host | 15 | Xác định đúng máy chủ bị ảnh hưởng |
| Isolate Attacked Host | 20 | Cô lập thành công máy chủ bị tấn công |
| Recover Service | 20 | Khôi phục thành công dịch vụ |
| Incident Report | 10 | Hoàn thành báo cáo sự cố |

Assessment Engine kiểm tra điều kiện hoàn thành của từng nhiệm vụ dựa trên dữ liệu và bằng chứng.

Scoring Engine sử dụng kết quả đánh giá để tính điểm.

Incident Response không trực tiếp quyết định điểm số cuối cùng.

## 11. V1 Scope

Phạm vi Incident Response trong phiên bản V1:

| Chức năng | Mô tả |
|---|---|
| Incident Identification | Xác định và phân tích sự cố |
| Asset Identification | Xác định Asset bị ảnh hưởng |
| Host Isolation | Thực hiện cô lập máy chủ theo Scenario |
| Isolation Verification | Xác minh kết quả cô lập |
| Service Recovery | Khôi phục dịch vụ |
| Recovery Verification | Xác minh dịch vụ hoạt động trở lại |
| Incident Report | Tạo và lưu báo cáo sự cố |
| Response Evidence | Ghi nhận bằng chứng xử lý |
| Response Time | Ghi nhận thời điểm bắt đầu phản ứng |
| Recovery Time | Ghi nhận thời gian khôi phục |

Các chức năng nâng cao như tự động ứng phó sự cố, SOAR, phân tích nguyên nhân gốc rễ tự động và điều phối phản ứng trên nhiều hệ thống chưa thuộc phạm vi bắt buộc của V1.

## 12. Nguyên tắc triển khai Backend

Khi triển khai Incident Response trong Backend, cần tuân thủ các nguyên tắc:

- Mọi hành động xử lý phải thuộc đúng Exercise.
- Kiểm tra quyền người dùng trước khi thực hiện hành động.
- Chỉ người có quyền phù hợp mới được thực hiện thao tác quản lý môi trường.
- Phân biệt yêu cầu thực hiện hành động với kết quả thực tế.
- Không đánh dấu hoàn thành nếu chưa xác minh kết quả.
- Mọi hành động quan trọng cần ghi nhận người thực hiện và timestamp.
- Evidence phải được liên kết với đúng Exercise và đối tượng liên quan.
- Không cho phép Client tự gửi điểm số hoặc tự xác nhận nhiệm vụ đã hoàn thành.
- Các thao tác cập nhật trạng thái cần kiểm tra trạng thái hiện tại để tránh xung đột.
- Các thao tác quan trọng cần được ghi nhận trong Audit Log.

Các bảng Database liên quan cần được đối chiếu khi triển khai:

- `exercises`
- `exercise_participants`
- `assets`
- `services`
- `events`
- `evidence`
- `incident_reports`
- `assessments`
- `assessment_results`
- `assessment_scores`
- `audit_logs`

Cần ưu tiên sử dụng cấu trúc Database hiện có. Không tự ý tạo thêm bảng hoặc thay đổi schema khi chưa kiểm tra thiết kế tổng thể.

## 13. Kết luận

Incident Response là thành phần chịu trách nhiệm xử lý sự cố sau khi Detection ghi nhận dấu hiệu tấn công.

Trong phiên bản V1, Blue Team tập trung vào việc xác định máy chủ bị ảnh hưởng, cô lập máy chủ theo yêu cầu Scenario, khôi phục dịch vụ và hoàn thiện báo cáo sự cố.

Toàn bộ quá trình cần được ghi nhận bằng dữ liệu và bằng chứng có thể xác minh. Đây là cơ sở để Assessment Engine đánh giá khả năng phản ứng của Blue Team và Scoring Engine tính điểm cho Exercise.