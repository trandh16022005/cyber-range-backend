# Tiến độ triển khai Cyber Range

## Nộp bài có phiên bản và xác nhận trước công bố

- TIME_THRESHOLD dùng durationField (giây), maximumSeconds và score. Engine phân biệt chưa đo, giá trị không hợp lệ, vượt ngưỡng và đạt. Đã thêm kiểm thử biên 0 và 60 giây.
- SSH Practice lưu snapshot bài nộp trong lịch sử cùng người nộp, Assessment, Team, Lab Session và phiên bản. Yêu cầu bổ sung mở lại phần báo cáo. CONFIRM_REVIEW xác nhận phiên bản, PUBLISH_REVIEW mới ghi điểm chính thức. REVIEW tiếp tục hỗ trợ bài nộp cũ chưa có submittedBy.
- Database chính đang dùng rubric cũ. Còn cần cấu hình rubric riêng, chấm tự động khi nộp, điểm thủ công theo criterion và nghiệm thu trình duyệt trên dữ liệu chính. Chưa chạy migration.

## Scoring dùng chung

- Tách xử lý rule và phép chuẩn hóa vào package scoring. Assessment tiếp tục điều phối giao dịch và bảo vệ kết quả REVIEWED trước lần chấm lại.
- Thêm SET_MATCH theo tỷ lệ ID đúng trên hợp hai tập và hỗ trợ điểm một phần khi Criterion có cấu hình tương ứng. Cấu hình SSH đang dùng rule cũ. Chưa áp dụng migration hoặc đổi điểm trên database chính.
- Còn lại tích hợp tiêu chí SSH với SET_MATCH, TIME_THRESHOLD, phiên bản bài nộp, giao diện review và nghiệm thu qua trình duyệt.

## Checkbox điều tra và DTO bài thực hành

- Lượt ACTIVE của Exercise #3 đã xác nhận cảnh báo, nhưng chưa lưu lựa chọn bản ghi. Qua mã nguồn, bảng chỉ nhận callback chọn khi BLUE đã xác nhận cảnh báo. Chưa xác minh điều kiện cụ thể làm nút bị khóa trong phiên trình duyệt gặp lỗi.
- Tách cột chọn, tô sáng dòng đã chọn, hiển thị số lượng và liệt kê các điều kiện còn thiếu. Giữ bản nháp theo ID lượt qua polling và khôi phục lựa chọn đã lưu khi mở lại.
- Defensive Tasks lấy từ Assessment Results, trả thêm taskId và giữ name/maximum. Dữ liệu chính của Assessment #3 có bảy tên nhiệm vụ và tổng 100 điểm. Chưa thu được payload của lỗi chỉ hiện số thứ tự.
- Đã tách request và response SSH Practice sang package dto. Nghiệm thu đăng nhập BLUE, lưu điều tra và kiểm tra luồng ba vai trò trên giao diện chính vẫn còn.

## Event và Alert trong bài thực hành SSH

- Trước thay đổi, Exercise #3 có lượt APPROVED, kết nối NONE, một Event xác thực thành công và không có Alert. Mô phỏng trước đây chỉ lưu trạng thái trong lượt.
- Bộ dữ liệu cố định nay đi qua ingress và Detection Engine, ghi nguồn SSH_PRACTICE và provenance SIMULATED_PRACTICE. Correlation tách nguồn Collector. Workflow vận hành thực từ chối thao tác phản ứng lên Alert mô phỏng. Evidence mới lưu ID Alert, còn rubric dùng các Event thực sự được Engine liên kết.
- Viewer hiển thị bộ lọc, trang, ID Alert và nguồn dữ liệu trên 100 Event gần nhất. Raw log từ Collector hiện không được trả cho học viên. Phân trang server và lọc/redact raw log Collector còn lại.
- Hồi quy trên PostgreSQL cô lập xác nhận STUDENT BLUE tải lượt đã có Event, mô phỏng phát lại vẫn chỉ có sáu Event và một Alert, và điểm phục hồi đúng/sai là 100/85 theo rubric. TypeScript và Vite build đạt. Chưa thu được stack trace của request STUDENT trả 500 trên sản phẩm chính và chưa nghiệm thu đủ ba vai trò qua giao diện.

## Kiểm tra khởi động sản phẩm chính

- Đã lưu cấu hình PostgreSQL trong thư mục riêng của tài khoản Windows và chạy `./mvnw.cmd spring-boot:run` với `cyber_range_db`. Backend khởi động trên `127.0.0.1:8080` với Hibernate validate, Runner xác thực và gửi heartbeat `RUNNING` được Backend chấp nhận. Frontend trả HTTP 200 trên cổng 5173 và proxy tới API có bảo vệ trả HTTP 401 khi thiếu xác thực.
- Lỗi duyệt lượt trong giao diện vẫn cần tái hiện với phiên đăng nhập ADMIN hoặc INSTRUCTOR. Chưa áp dụng migration hoặc thay đổi Exercise Lifecycle trong bước cấu hình này.

## Exercise Lifecycle và duyệt lượt SSH

- Trang chi tiết Exercise có nút vòng đời cho ADMIN và INSTRUCTOR. Frontend đọc thao tác từ Backend, theo dõi yêu cầu dừng Lab và cập nhật Exercise sau khi Runner xác nhận. Lab Workflow giữ khả năng chọn Exercise đang chạy, tạm dừng và đã kết thúc để xem lại.
- API kiểm tra điều kiện duyệt cho đội RED và phiên hiện tại trước khi bật nút. Giao dịch duyệt kiểm tra lại điều kiện và từ chối nếu mã phiên đã thay đổi. API thao tác của học viên kiểm tra trạng thái Exercise trong giao dịch ghi.
- Maven đạt 8/8 trên PostgreSQL cô lập. TypeScript và Vite build đạt. Sau khi PostgreSQL chính hoạt động trở lại, truy vấn chỉ đọc xác nhận SQLState `42P01` vì thiếu `public.ssh_practice_runs`, đồng thời xác nhận thiếu bảng lịch sử và index của migration 003.
- Đã phục hồi bản sao lưu mới `cyber_range_db-before-practice-20261128114307.dump` trên cụm riêng và chạy lặp thành công migration 002/003. Số Exercise, Assessment, Event và Evidence giữ nguyên. Nghiệm thu trình duyệt chính và migration chính đang chờ xác nhận.

## Khởi động sản phẩm Windows từ Spring Boot

- Backend chính kiểm tra cấu hình, sinh Service Token trong bộ nhớ và đưa Runner cùng Vite vào Windows Job Object. Hibernate xác minh `cyber_range_db/public`. Khi nhận tín hiệu dừng mềm, Backend yêu cầu Runner dừng phiên qua workflow hiện có trước khi đóng các tiến trình con.
- Tệp mới gồm bốn lớp trong package `runtime` và `WindowsProcessJobTest`. Tệp sửa gồm `pom.xml`, điểm vào `CyberRangeBackendApplication`, `SshLabService`, `runner.py`, README và hướng dẫn SSH. Các bài tích hợp và fixture cho phép thêm cổng kiểm thử cục bộ `55440` với cùng tên database cô lập.
- Đã chạy bằng Maven và cấu hình Run của IntelliJ. IntelliJ cần Node trên `PATH`. Main Backend và Vite lắng nghe loopback. Exercise 3 chuyển từ chờ dừng sang `STOPPED` qua Runner, Frontend trả HTTP 200 và API qua proxy từ chối yêu cầu thiếu credential với HTTP 403. Đã kiểm tra thiếu mật khẩu, cổng bị chiếm, khóa chống Runner trùng, `Ctrl+C` và buộc dừng JVM. Runner và Vite thoát trong các lần dừng kiểm thử.
- Maven compile đạt. Hồi quy đạt 8/8 trên PostgreSQL cô lập ở `127.0.0.1:55440/cyber_range_test`. `npm run build` gồm TypeScript và build Vite đạt. Cú pháp Python và Compose đạt.
- **Còn kiểm chứng trên môi trường chính.** Mở một phiên mới qua tài khoản có quyền để quan sát `READY`, rồi dừng mềm Backend khi phiên đang chạy. Chưa áp dụng migration vào database chính trong lần thay đổi này. Chưa nghiệm thu luồng sinh viên trên môi trường chính.

**INCOMPLETE** — Đã xác minh thu thập sự kiện thực trên môi trường chính. Nghiệm thu Alert, phản ứng và chấm điểm trên môi trường chính còn thiếu Alert hợp lệ.

## Đã hoàn thành

- Migration và seed SSH đã áp dụng vào cyber_range_db qua các lượt đã được phê duyệt. Hibernate validate đã đạt trên kết nối chỉ đọc. Exercise 3 dùng Assessment 3 của Blue Team Alpha và bảy Criteria tổng 100 điểm.
- Backend chạy trên 127.0.0.1:8080. Vite trả HTTP 200 cho trang đăng nhập trên 5173. Lab 342bf283-0ff2-4b19-b445-353ddb7ae5a4 có ba container healthy và heartbeat RUNNING.
- Một lượt xác thực hợp lệ trên Lab chính tạo đúng một dòng Accepted publickey trong auth.log. Collector đã gửi hết hàng đợi và PostgreSQL lưu Event 2, SSH_AUTH_SUCCESS, nguồn OPENSSH_COLLECTOR, tài khoản lab-user và có khóa Collector. Exercise 3 có 0 Alert tại thời điểm kiểm tra.
- Maven test thành công với 3 unit test đạt và 2 test PostgreSQL bỏ qua vì chưa có cụm 55439. TypeScript, Vite build, Compose config và Git diff --check đều đạt. Kết quả End-to-End cô lập trước đó được lưu tại [hồ sơ nghiệm thu](ssh-defensive-progress.md).

## Quyết định và tệp thay đổi

Lượt này thêm docs/IMPLEMENTATION_PROGRESS.md. Giữ nguyên mã nguồn, dữ liệu seed và schema hiện hành. Giữ Lab chính chạy để người dùng kiểm tra Event qua giao diện. Dữ liệu kiểm thử vẫn thuộc môi trường cô lập. Danh mục tệp của các lượt trước nằm trong [hồ sơ nghiệm thu](ssh-defensive-progress.md).

## Tiếp tục sau gián đoạn

1. Đăng nhập ADMIN hoặc INSTRUCTOR, mở Lab Workflow của Exercise 3 và chọn Tải sự kiện gần nhất. Đối chiếu Event 2 với bản ghi PostgreSQL và nghiệm thu các màn hình có bảo vệ.
2. Khi có Alert hợp lệ từ hoạt động thực tế, kiểm tra tiếp nhận, phản ứng, Incident Report, Evidence, thời gian và điểm. Dùng [runbook](ssh-defensive-workflow.md) để tái hiện integration test trên PostgreSQL cô lập nếu cần.
3. Dừng Lab từ giao diện sau demo và kiểm tra STOPPED. Chỉ cập nhật COMPLETED sau khi đủ bằng chứng nghiệm thu môi trường chính.


## Nâng cấp luồng thực hành cho sinh viên

INCOMPLETE. Baseline test-compile đạt. Bản hiện có đã có màn hình ba vai trò và migration 002. Đang hoàn thiện xác nhận sự cố, báo cáo có cấu trúc, Evidence và công bố điểm vào Assessment. Migration nghiệp vụ tiếp tục chờ sao lưu, phục hồi thử và phê duyệt.


### Checkpoint bàn giao SSH có hướng dẫn

- **INCOMPLETE trên môi trường chính.** `cyber_range_db` đã qua Hibernate validate qua kết nối chỉ đọc và chưa có bảng `ssh_practice_runs`. Cần phê duyệt migration 002 và 003, sau đó bật `SSH_PRACTICE_ENABLED=true` ở Backend và runner, phân công RED/BLUE và nghiệm thu bằng ba tài khoản thật. Nếu cấu hình chính thay đổi sau bản backup, sao lưu lại ngay trước khi áp dụng.
- **Đã hoàn thiện mã.** Bổ sung xác nhận phát hiện, chọn bằng chứng, đo thời gian học viên, đề xuất và duyệt phản ứng mô phỏng, báo cáo 10 mục có bản nháp lưu server và công bố bài nộp vào Assessment. Evidence mang provenance `TRAINING_SIMULATION`. Thao tác SSH được cấp quyền của RED tạo Event quan sát thật. WHITE chỉ quan sát trong Exercise được giao.
- **Docker và cấu hình.** Compose dùng hai image local chia sẻ giữa các phiên. Runner build khi thiếu image và khởi động bằng image sẵn có. CORS mặc định cho phép hai địa chỉ Vite loopback trên cổng 5173. Docker Lab giữ mạng nội bộ và không cần terminal cho học viên.
- **Kiểm thử.** Maven 7 đạt, không lỗi và không skip. Hai lượt chấm phục hồi đúng/sai cho 100/85. TypeScript, Vite build, Compose config và Git diff --check đạt. Browser Edge headless với ba tài khoản thực sự đăng nhập qua UI trên database cô lập đã hoàn thành Lab. Đối chiếu PostgreSQL có một Event SSH_AUTH_SUCCESS từ OPENSSH_COLLECTOR, bảy Evidence TRAINING_SIMULATION, báo cáo phiên bản 1, Assessment REVIEWED 100 điểm và Lab STOPPED. BLUE xem lại được điểm sau khi dừng.
- **Backup.** Đã kiểm tra archive `target/database-backups/cyber_range_db-before-practice-20261128062444.dump`, phục hồi thử vào `cyber_range_restore_20261128062444` trên cổng 55439 và chạy thành công cả 002 lẫn 003. Chưa áp dụng các script này lên database chính.
- **Tệp mới.** `database/migrations/003_ssh_practice_assessment.sql`, `assessment/service/SshPracticeAssessmentPublisher.java`.
- **Tệp sửa.** Compose, runner, `application.properties`, `SshPracticePage.tsx`, `ScoreEvidence.tsx`, `AssessmentScoreEvidenceResponse.java`, `Assessment.java`, `AssessmentResult.java`, `SshAssessmentEvaluator.java`, `SshPracticeRepository.java`, `SshLabService.java`, `SshPracticeService.java`, `SshPracticeIntegrationTest.java`, tài liệu kiến trúc, runbook SSH và tệp tiến độ này. Không xóa tệp, commit hoặc push.
