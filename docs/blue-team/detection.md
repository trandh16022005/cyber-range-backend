# Blue Team Detection

## 1. Mục đích

Tài liệu này mô tả nghiệp vụ phát hiện sự kiện và hành vi bất thường (Detection) trong hệ thống Cyber Range.

Detection là quá trình Blue Team thu thập, phân tích và đánh giá các sự kiện bảo mật được ghi nhận trong môi trường Cyber Range nhằm phát hiện sớm các hoạt động tấn công hoặc dấu hiệu xâm nhập.

Mục tiêu của Detection:

- Theo dõi các sự kiện phát sinh trên những máy chủ và dịch vụ trong Cyber Range.
- Phát hiện các hành vi bất thường hoặc có dấu hiệu tấn công.
- Xác định tài sản (Asset) có khả năng bị ảnh hưởng.
- Ghi nhận bằng chứng phục vụ quá trình đánh giá.
- Cung cấp dữ liệu cho Assessment Engine để xác định khả năng phát hiện của Blue Team.

Detection không trực tiếp thực hiện hành động ngăn chặn hoặc khôi phục hệ thống. Những hoạt động này thuộc phạm vi Incident Response.

## 2. Phạm vi phát hiện

Trong phiên bản V1, hệ thống tập trung vào việc phát hiện một số hành vi tấn công cơ bản trong môi trường Cyber Range.

Các nhóm sự kiện được theo dõi:

| Nhóm sự kiện | Dữ liệu theo dõi | Mục đích |
|---|---|---|
| Authentication | Đăng nhập thành công, thất bại | Phát hiện đăng nhập bất thường |
| SSH | Các lần đăng nhập SSH thất bại, thành công | Phát hiện dấu hiệu brute force |
| Web Access | HTTP request, URL, HTTP method, response status | Phát hiện hành vi tấn công Web |
| Linux System | Hoạt động tiến trình, trạng thái hệ thống | Hỗ trợ xác định dấu hiệu xâm nhập |
| Network | Kết nối mạng, địa chỉ nguồn và đích, cổng dịch vụ | Phát hiện hoạt động quét cổng |
| Asset Status | Trạng thái máy chủ và dịch vụ | Theo dõi tình trạng tài sản |

Các nguồn dữ liệu có thể được mở rộng trong những phiên bản tiếp theo.

## 3. Nguồn dữ liệu sự kiện

Hệ thống tiếp nhận dữ liệu từ các thành phần trong Cyber Range.

### 3.1. Authentication Logs

Ghi nhận hoạt động xác thực trên hệ thống:

- Thời điểm đăng nhập.
- Tài khoản được sử dụng.
- Địa chỉ IP nguồn nếu có.
- Kết quả xác thực.
- Máy chủ thực hiện xác thực.

Mục đích:

- Phát hiện nhiều lần đăng nhập thất bại.
- Phát hiện hoạt động đăng nhập bất thường.
- Xác định tài khoản có dấu hiệu bị sử dụng trái phép.

### 3.2. SSH Logs

Ghi nhận hoạt động truy cập SSH trên Linux Server:

- Địa chỉ IP nguồn.
- Tên tài khoản đăng nhập.
- Thời điểm đăng nhập.
- Kết quả đăng nhập.
- Máy chủ nhận kết nối.

Mục đích:

- Phát hiện nhiều lần đăng nhập SSH thất bại.
- Xác định dấu hiệu brute force.
- Hỗ trợ xác định tài khoản hoặc máy chủ có nguy cơ bị xâm nhập.

### 3.3. Web Access Logs

Ghi nhận hoạt động truy cập Web Server:

- Địa chỉ IP nguồn.
- HTTP method.
- URL được truy cập.
- HTTP response status.
- Thời điểm request.
- Web Server tiếp nhận request.

Mục đích:

- Phát hiện request có dấu hiệu bất thường.
- Phát hiện các hoạt động tấn công Web thuộc phạm vi kịch bản.
- Xác định Web Server có khả năng bị ảnh hưởng.

### 3.4. Network Events

Ghi nhận các sự kiện liên quan đến hoạt động mạng:

- Địa chỉ IP nguồn.
- Địa chỉ IP đích.
- Cổng nguồn và cổng đích nếu có.
- Giao thức mạng.
- Thời điểm kết nối.
- Tài sản liên quan.

Mục đích:

- Phát hiện dấu hiệu quét cổng.
- Theo dõi các kết nối bất thường.
- Xác định tài sản đang nhận các kết nối đáng chú ý.

### 3.5. System Events

Ghi nhận các sự kiện phát sinh trên Linux Server và Windows Server:

- Hoạt động của hệ thống.
- Sự kiện liên quan đến tiến trình.
- Thay đổi trạng thái máy chủ.
- Các sự kiện bảo mật do hệ điều hành ghi nhận.

Mục đích:

- Hỗ trợ phân tích dấu hiệu xâm nhập.
- Xác định máy chủ có khả năng bị xâm phạm.
- Cung cấp thông tin phục vụ Incident Response.

### 3.6. Asset và Service Status

Theo dõi trạng thái của các Asset và Service trong Cyber Range.

Các trạng thái Asset được sử dụng:

- `ONLINE`: Tài sản đang hoạt động.
- `OFFLINE`: Tài sản không hoạt động.
- `MAINTENANCE`: Tài sản đang được bảo trì.

Mục đích:

- Theo dõi tình trạng máy chủ.
- Phát hiện sự thay đổi trạng thái bất thường.
- Hỗ trợ xác định ảnh hưởng của sự cố đến hệ thống.

## 4. Event Collection và xử lý dữ liệu

### 4.1. Event Collection

Event Collection là quá trình tiếp nhận các sự kiện từ những nguồn dữ liệu trong Cyber Range.

Quy trình tổng quát:

1. Các máy chủ và dịch vụ phát sinh log hoặc sự kiện.
2. Thành phần thu thập dữ liệu tiếp nhận sự kiện.
3. Hệ thống ghi nhận thông tin về nguồn phát sinh và thời điểm xảy ra.
4. Sự kiện được chuẩn hóa và lưu trữ.
5. Dữ liệu được chuyển đến thành phần Detection để phân tích.

Event Collection có nhiệm vụ thu thập và ghi nhận dữ liệu, không tự quyết định kết quả đánh giá Blue Team.

### 4.2. Event Normalization

Các nguồn dữ liệu có thể sử dụng định dạng log khác nhau. Vì vậy, hệ thống cần chuẩn hóa dữ liệu để thuận tiện cho việc phân tích và xử lý.

Một sự kiện sau khi chuẩn hóa cần có những thông tin nghiệp vụ phù hợp như:

| Trường dữ liệu | Ý nghĩa |
|---|---|
| `id` | Định danh sự kiện |
| `exercise_id` | Exercise phát sinh sự kiện |
| `asset_id` | Asset liên quan đến sự kiện |
| `event_type` | Loại sự kiện |
| `source` | Nguồn dữ liệu phát sinh |
| `source_address` | Địa chỉ nguồn nếu có |
| `destination_address` | Địa chỉ đích nếu có |
| `username` | Tài khoản liên quan nếu có |
| `timestamp` | Thời điểm sự kiện xảy ra |
| `severity` | Mức độ nghiêm trọng |
| `raw_data` | Dữ liệu gốc của sự kiện |
| `created_at` | Thời điểm hệ thống ghi nhận |

Các trường trên là mô hình dữ liệu nghiệp vụ đề xuất để thống nhất cách xử lý Event. Khi triển khai Backend, cần đối chiếu với cấu trúc bảng `events` hiện có trong Database trước khi quyết định bổ sung hoặc thay đổi cột.

### 4.3. Event Processing

Sau khi thu thập và chuẩn hóa, hệ thống thực hiện xử lý sự kiện:

1. Kiểm tra tính hợp lệ của dữ liệu.
2. Xác định Exercise và Asset liên quan.
3. Phân loại sự kiện theo `event_type`.
4. Lưu trữ sự kiện.
5. Chuyển dữ liệu đến thành phần phân tích Detection.

Hệ thống cần đảm bảo sự kiện được liên kết đúng với Exercise tương ứng để tránh sử dụng dữ liệu của Exercise này trong quá trình đánh giá Exercise khác.

## 5. Detection Scenarios

Trong phiên bản V1, hệ thống tập trung vào các tình huống phát hiện sau:

### 5.1. Port Scan Detection

**Mục tiêu:** Phát hiện dấu hiệu một nguồn đang thực hiện hoạt động quét các cổng dịch vụ trên một Asset.

Dữ liệu đầu vào:

- Địa chỉ IP nguồn.
- Địa chỉ IP đích.
- Cổng đích.
- Giao thức.
- Thời điểm kết nối.
- Asset liên quan.

Dấu hiệu cần phát hiện:

- Một nguồn phát sinh nhiều kết nối đến các cổng dịch vụ khác nhau.
- Nhiều sự kiện kết nối liên quan đến cùng một Asset trong một khoảng thời gian.
- Các sự kiện mạng có đặc điểm phù hợp với hành vi quét cổng.

Kết quả Detection:

- Ghi nhận sự kiện nghi vấn.
- Xác định nguồn phát sinh.
- Xác định Asset liên quan.
- Lưu bằng chứng phục vụ đánh giá.

### 5.2. Brute Force Detection

**Mục tiêu:** Phát hiện dấu hiệu có nhiều lần thử đăng nhập thất bại nhằm truy cập trái phép vào hệ thống.

Dữ liệu đầu vào:

- Địa chỉ IP nguồn.
- Tên tài khoản.
- Thời điểm đăng nhập.
- Kết quả xác thực.
- Máy chủ tiếp nhận yêu cầu.

Dấu hiệu cần phát hiện:

- Nhiều lần đăng nhập thất bại liên tiếp.
- Nhiều lần thử xác thực liên quan đến cùng một tài khoản.
- Nhiều yêu cầu xác thực từ cùng một nguồn trong một khoảng thời gian.

Kết quả Detection:

- Ghi nhận hoạt động đăng nhập đáng ngờ.
- Xác định tài khoản liên quan.
- Xác định máy chủ có khả năng bị nhắm mục tiêu.
- Lưu bằng chứng phục vụ đánh giá.

### 5.3. Web Attack Detection

**Mục tiêu:** Phát hiện các request có dấu hiệu tấn công Web trong phạm vi kịch bản Cyber Range.

Dữ liệu đầu vào:

- Địa chỉ IP nguồn.
- HTTP method.
- URL.
- HTTP response status.
- Thời điểm request.
- Web Server tiếp nhận request.

Dấu hiệu cần phát hiện:

- Request có đặc điểm bất thường.
- Các request phù hợp với nhóm hành vi tấn công được định nghĩa trong Scenario.
- Nhiều request đáng chú ý liên quan đến cùng một Web Server.

Kết quả Detection:

- Ghi nhận request nghi vấn.
- Xác định nguồn request.
- Xác định Web Server liên quan.
- Lưu bằng chứng phục vụ đánh giá.

Các tiêu chí nhận diện cụ thể cần được định nghĩa theo từng Scenario, thay vì áp dụng một quy tắc chung cho mọi môi trường.

### 5.4. Compromised Host Identification

**Mục tiêu:** Xác định Asset có dấu hiệu đã bị xâm nhập hoặc bị ảnh hưởng trong quá trình thực hiện Scenario.

Dữ liệu đầu vào:

- Authentication Logs.
- SSH Logs.
- System Events.
- Network Events.
- Asset Status.
- Các bằng chứng liên quan đến Exercise.

Thông tin cần xác định:

- Asset có dấu hiệu bị ảnh hưởng.
- Thời điểm xuất hiện dấu hiệu bất thường.
- Các sự kiện liên quan.
- Bằng chứng hỗ trợ nhận định.
- Mức độ tin cậy của kết quả nhận diện.

Kết quả Detection:

- Ghi nhận Asset nghi bị xâm nhập.
- Liên kết các sự kiện và bằng chứng liên quan.
- Cung cấp dữ liệu cho Blue Team thực hiện Incident Response.

Việc xác định một Asset bị xâm nhập cần dựa trên bằng chứng và tiêu chí của Scenario, không chỉ dựa vào một sự kiện đơn lẻ.

## 6. Detection Result

Sau khi phân tích sự kiện, hệ thống cần ghi nhận kết quả Detection để phục vụ quá trình theo dõi và đánh giá.

Một kết quả Detection về mặt nghiệp vụ cần thể hiện:

| Thông tin | Ý nghĩa |
|---|---|
| Exercise | Exercise liên quan |
| Detection Type | Loại hành vi được phát hiện |
| Related Asset | Asset liên quan |
| Related Events | Các sự kiện liên quan |
| Detection Time | Thời điểm phát hiện |
| Evidence | Bằng chứng hỗ trợ |
| Description | Mô tả kết quả phát hiện |

Kết quả Detection cần được liên kết với Exercise và các Event tương ứng.

Lưu ý: Detection Result là khái niệm nghiệp vụ. Việc triển khai có thể sử dụng các bảng hiện có như `events`, `evidence` và `assessment_results` tùy theo thiết kế Database. Không mặc định phải tạo thêm một bảng mới nếu cấu trúc hiện tại đã đáp ứng yêu cầu.

## 7. Evidence Collection

Evidence là bằng chứng được sử dụng để chứng minh một sự kiện hoặc hành động phát hiện đã xảy ra.

Các loại bằng chứng có thể bao gồm:

- Log đăng nhập.
- SSH Logs.
- Web Access Logs.
- Network Events.
- System Events.
- Thông tin trạng thái Asset và Service.
- Kết quả phân tích liên quan đến Detection.

Mỗi Evidence cần được liên kết với Exercise và thông tin liên quan để phục vụ việc kiểm tra.

Các thông tin nghiệp vụ cần quản lý:

- Loại bằng chứng.
- Nguồn bằng chứng.
- Thời điểm ghi nhận.
- Asset liên quan.
- Event liên quan nếu có.
- Nội dung hoặc tham chiếu đến dữ liệu bằng chứng.

Nguyên tắc quản lý Evidence:

1. Bằng chứng phải có nguồn gốc xác định.
2. Bằng chứng phải liên kết với đúng Exercise.
3. Không tự ý thay đổi dữ liệu gốc sau khi đã ghi nhận.
4. Cần bảo toàn thông tin thời gian để phục vụ đánh giá.
5. Chỉ người có quyền phù hợp mới được xem hoặc quản lý bằng chứng.

## 8. Detection Time

Detection Time là thời điểm Blue Team phát hiện được sự kiện hoặc hành vi tấn công.

Thông tin này được sử dụng để đánh giá khả năng phát hiện và tốc độ phản ứng của Blue Team.

Các mốc thời gian nghiệp vụ:

- `attack_time`: Thời điểm hành vi tấn công xảy ra theo dữ liệu Scenario hoặc nguồn ghi nhận đáng tin cậy.
- `detection_time`: Thời điểm Blue Team ghi nhận hoặc xác nhận phát hiện.
- `response_time`: Thời điểm Blue Team bắt đầu thực hiện hành động xử lý.

Thời gian phát hiện có thể được tính:

`Detection Time = Detection Timestamp - Attack Timestamp`

Trong đó:

- Giá trị càng nhỏ thể hiện khả năng phát hiện càng nhanh.
- Nếu không có đủ dữ liệu xác định thời điểm tấn công hoặc phát hiện thì không được tự suy đoán kết quả.
- Cần thống nhất cách xác định timestamp giữa các nguồn dữ liệu để tránh sai lệch.

Detection Time là dữ liệu đầu vào cho Assessment Engine và Scoring Engine, không phải điểm số cuối cùng.

## 9. Detection và Assessment Engine

Detection cung cấp dữ liệu để Assessment Engine đánh giá khả năng phát hiện của Blue Team.

Quy trình:

1. Scenario xác định hành vi cần được phát hiện.
2. Cyber Range phát sinh sự kiện tương ứng.
3. Event Collection thu thập dữ liệu.
4. Detection phân tích và ghi nhận kết quả.
5. Evidence được lưu trữ và liên kết.
6. Assessment Engine kiểm tra kết quả Detection với tiêu chí đánh giá.
7. Assessment Result được ghi nhận.
8. Scoring Engine sử dụng Assessment Result để tính điểm.

Assessment Engine chịu trách nhiệm xác định nhiệm vụ đã đáp ứng điều kiện đánh giá hay chưa.

Scoring Engine chịu trách nhiệm tính điểm dựa trên kết quả đánh giá.

Detection không trực tiếp quyết định điểm số cuối cùng.

## 10. V1 Scope

Phạm vi Detection trong phiên bản V1:

| Chức năng | Mô tả |
|---|---|
| Event Collection | Tiếp nhận sự kiện từ các nguồn dữ liệu |
| Event Normalization | Chuẩn hóa dữ liệu sự kiện |
| Event Storage | Lưu trữ sự kiện theo Exercise |
| Port Scan Detection | Phát hiện dấu hiệu quét cổng |
| Brute Force Detection | Phát hiện dấu hiệu đăng nhập bất thường |
| Web Attack Detection | Phát hiện hành vi tấn công Web theo Scenario |
| Compromised Host Identification | Xác định Asset có dấu hiệu bị xâm nhập |
| Evidence Collection | Thu thập và liên kết bằng chứng |
| Detection Time | Ghi nhận thời điểm phát hiện |

Các chức năng nâng cao như phân tích hành vi bằng Machine Learning, Threat Intelligence, Correlation Engine phức tạp và tự động phản ứng chưa thuộc phạm vi bắt buộc của V1.

## 11. Nguyên tắc triển khai Backend

Khi triển khai Detection trong Backend, cần tuân thủ các nguyên tắc:

- Mỗi Event phải được liên kết với Exercise phù hợp.
- Event phải được liên kết với Asset nếu xác định được tài sản liên quan.
- Không để dữ liệu của Exercise này ảnh hưởng đến Exercise khác.
- Phân biệt dữ liệu Event gốc với kết quả phân tích Detection.
- Evidence phải có nguồn gốc và thời điểm ghi nhận.
- Không cho phép Client tự gửi kết quả Assessment hoặc điểm số để thay thế kết quả do hệ thống xác định.
- Các thao tác ghi nhận dữ liệu quan trọng cần được kiểm tra quyền truy cập.
- Cần xử lý trường hợp thiếu dữ liệu, dữ liệu trùng lặp hoặc dữ liệu không hợp lệ.
- Các quy tắc Detection cần có khả năng mở rộng theo Scenario.

Khi triển khai Entity và Repository, cần đối chiếu với Database Schema hiện tại, đặc biệt là các bảng:

- `events`
- `evidence`
- `assets`
- `services`
- `exercises`
- `assessments`
- `assessment_results`

Không tự ý tạo thêm bảng hoặc thay đổi cấu trúc Database khi chưa kiểm tra sự tương thích với thiết kế hiện tại.

## 12. Kết luận

Detection là thành phần quan trọng trong quy trình hoạt động của Blue Team. Thành phần này giúp thu thập, phân tích và ghi nhận các dấu hiệu tấn công trong Cyber Range.

Dữ liệu Detection và Evidence là cơ sở để Assessment Engine đánh giá khả năng phát hiện của Blue Team, đồng thời cung cấp thông tin cần thiết cho Incident Response và Scoring Engine.

Trong phiên bản V1, hệ thống tập trung vào bốn nhóm nghiệp vụ chính: phát hiện Port Scan, Brute Force, Web Attack và xác định máy chủ có dấu hiệu bị xâm nhập.