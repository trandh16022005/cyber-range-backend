# Vận hành SSH Security Incident Response

## Kiến trúc

React sử dụng API JWT của Spring Boot để chọn Exercise, Scenario và Assessment của đội BLUE. Lab runner chạy trên host, đọc trạng thái mong muốn từ Backend và quản lý đúng Compose project mang tên `cyber-range-ssh-<mã phiên>`. Ubuntu Target, client thực hành và Event Collector dùng bridge `internal`. OpenSSH không publish cổng ra host. Runner sử dụng quyền Docker sẵn có của người vận hành, còn Backend và container không giữ Docker socket.

OpenSSH ghi log UTC dạng RFC 3339 qua rsyslog vào `/var/log/lab/auth.log`. Collector đọc volume log chỉ đọc, ghi checkpoint và hàng đợi SQLite trong volume theo phiên. Runner chuyển batch từ Collector đến Backend qua loopback. Backend kiểm tra phiên, Asset, Service, timestamp và khóa bản ghi, rồi ghi Event vào PostgreSQL. Detection mặc định tìm 5 lỗi SSH trong 60 giây của cùng phiên, Exercise, Service, IP nguồn và tài khoản. Event đến muộn trong 10 phút vẫn được đánh giá. Sự kiện cũ hơn vẫn có thể được lưu nếu thuộc phiên. Health dựa trên heartbeat có hiệu lực 30 giây và các probe chạy thực tế.

Alert gắn với Assessment được chọn cho phiên Lab. Yêu cầu cô lập chỉ áp dụng cho IP đã được đối chiếu với client trên mạng Lab. Runner gỡ client khỏi đúng mạng và xác minh bằng Docker, rồi chạy probe xác thực SSH bằng khóa riêng của dịch vụ. Khi nộp Incident Report, Backend lấy Exercise và Team từ Alert đã được tiếp nhận. Evidence chấm điểm trỏ đến Event và nguồn nghiệp vụ đã được Backend kiểm tra. Hai task thủ công do ADMIN/INSTRUCTOR đối chiếu log và xác nhận tài khoản, máy chủ. Review cuối yêu cầu đóng Alert và hoàn tất các task áp dụng.

## Chuẩn bị database

Với database đang sử dụng, rà soát trước thay đổi với bản backup, sau đó chỉ chạy migration và seed khi được người quản trị phê duyệt. `database/database.sql` dành cho database mới. Nó đã được đồng bộ với các cột hiện có của Assessment, Evidence và Incident Report.

1. Tạo bản backup theo quy trình PostgreSQL của môi trường, ví dụ dùng `pg_dump -Fc` với credential cấu hình riêng và lưu tệp ngoài repo.
2. Chạy `database/migrations/001_ssh_defensive_workflow.sql` bằng `psql -X -v ON_ERROR_STOP=1`. Script bổ sung bảng Lab, Alert, liên kết Event, lịch sử phản ứng và các cột khóa ingestion, phạm vi Evidence.
3. Kiểm tra Scenario đã publish tên `SSH Security Incident Response`, có đủ bảy Defensive Tasks đúng thứ tự. Tạo Exercise, Team và participants bằng các màn hình quản lý nếu môi trường mới chưa có dữ liệu này.
4. Chạy `database/seeds/ssh_defensive_configuration.sql` bằng `psql -X -v ON_ERROR_STOP=1`. Script thêm Criteria, Asset `ubuntu-target` và Service `SSH` còn thiếu. Nếu cấu hình hiện có mâu thuẫn hoặc trùng khóa nghiệp vụ, toàn bộ lần chạy seed được rollback.

Migration và seed có thể chạy lại trên cùng schema. Điểm, Event, Alert, hành động, Incident Report và Evidence phát sinh qua workflow. Fixture nằm trong `src/test/resources/ssh`.

## Cấu hình và khởi động

| Biến môi trường | Nơi cấu hình | Ý nghĩa |
|---|---|---|
| `DATABASE_URL` | Backend | Mặc định `jdbc:postgresql://localhost:5432/cyber_range_db` |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Backend | Credential PostgreSQL |
| `JWT_SECRET` | Backend | Khóa ký JWT theo yêu cầu của JwtConfig hiện tại |
| `SSH_LAB_RUNNER_TOKEN` | Backend và runner | Windows tự sinh khi chạy chế độ quản lý. Cấu hình thủ công cần ít nhất 32 ký tự. |
| `SSH_LAB_COLLECTOR_TOKEN` | Backend và runner | Windows tự sinh token độc lập. Cấu hình thủ công cần token khác Runner và ít nhất 32 ký tự. |
| `SSH_LAB_BACKEND_PORT` | Runner thủ công | Cổng Backend loopback, mặc định `8080` |
| `SERVER_ADDRESS`, `SERVER_PORT` | Backend | Mặc định `127.0.0.1:8080` |
| `SSH_DETECTION_THRESHOLD`, `SSH_DETECTION_WINDOW_SECONDS` | Backend | Mặc định 5 Event / 60 giây |
| `VITE_BACKEND_PROXY_TARGET` | Vite dev server | Mặc định `http://127.0.0.1:8080` |

Bật Docker Desktop và PostgreSQL. Trên Windows, chạy `.\mvnw.cmd spring-boot:run` trong thư mục gốc hoặc chạy `CyberRangeBackendApplication` trong IntelliJ. Backend quản lý Runner và Vite, dùng chung Service Token trong bộ nhớ và in đường dẫn log khi khởi động. Xem [cấu hình Windows trong README](../README.md#66-khởi-động-cả-sản-phẩm-trên-windows) để chuẩn bị cấu hình một lần.

Quy trình thủ công dưới đây dành cho chẩn đoán. Trên Windows, đặt `CYBER_RANGE_MANAGED=false` cho Backend trước khi dùng quy trình này. Cấp credential qua môi trường và giữ các giá trị thực ngoài Git.

- Chạy Backend tại repo root bằng `.\mvnw.cmd spring-boot:run`.
- Trong `frontend`, `npm ci` cho lần cài đầu, sau đó `npm run dev`. Mở `http://localhost:5173`.
- Chạy runner tại repo root bằng `python -B cyber-range-lab/runner.py`. Khởi động một runner cho mỗi Backend. Compose được tạo khi người dùng bấm Khởi động Lab. Kiểm tra cú pháp trực tiếp với `docker compose -f cyber-range-lab/compose.yaml config -q`.

## Kiểm tra môi trường demo và cảnh báo IntelliJ

Môi trường demo dùng `localhost:5432/cyber_range_db`, schema `public`, user `postgres`. Cấp `DATABASE_USERNAME=postgres`, `DATABASE_PASSWORD` và `JWT_SECRET` trong terminal hoặc Run Configuration của tiến trình Backend. Vite gọi Backend qua proxy `127.0.0.1:8080`. Khi chạy demo, kiểm tra override `DATABASE_URL` và `VITE_BACKEND_PROXY_TARGET` nếu có.

Nếu IntelliJ báo thiếu `ssh_lab_session_id`, kiểm tra datasource `cyber_range_db@localhost` và schema `public` trong cửa sổ **Database**. Chạy **Diagnostics → Force Refresh** trên datasource nếu Refresh thường vẫn hiển thị metadata cũ. Mở lại `events` và kiểm tra cột kiểu `uuid`. Nếu cột đã xuất hiện trong Database nhưng trình soạn thảo vẫn cảnh báo, chọn datasource này trong **SQL Resolution Scopes** cho module Backend và giữ dialect PostgreSQL.

Để kiểm tra JPA trên database chính, chạy một phiên Backend kiểm tra với `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`, `SERVER_PORT=0` và URL `jdbc:postgresql://localhost:5432/cyber_range_db?options=-c%20default_transaction_read_only%3Don`. Đợi log `Started CyberRangeBackendApplication` rồi dừng phiên kiểm tra. Kiểm tra khóa ngoại, index và các truy vấn JDBC riêng bằng catalog PostgreSQL.

Khi có migration mới, tạo bản sao lưu PostgreSQL dạng custom, dùng `pg_restore --list` để đọc danh mục archive, rồi phục hồi vào cụm riêng và chạy thử migration tại đó trước khi xin phê duyệt áp dụng lên database chính.

## Bài thực hành qua giao diện

### Chuẩn bị môi trường

Chạy migration `002_ssh_practice.sql` rồi `003_ssh_practice_assessment.sql` sau khi đã khôi phục thử backup và được phê duyệt cho database chính. Đặt `SSH_PRACTICE_ENABLED=true` trong cả Backend và runner, giữ nguyên cấu hình kết nối và credential đã mô tả ở trên. Khi thay Dockerfile hoặc Compose, người vận hành build lại image dùng chung với `docker compose -f cyber-range-lab/compose.yaml build ubuntu-target event-collector`. Runner tái sử dụng các image đó khi cấp phiên mới.

Chuẩn bị ba tài khoản riêng gồm một INSTRUCTOR, một STUDENT RED và một STUDENT BLUE. Dùng **Teams** và **Exercises → Participants** để phân công hai học viên vào hai đội khác nhau trong cùng Exercise. Chọn Scenario SSH đã publish và Assessment của đội BLUE có đủ 7 Criteria, tổng 100 điểm. Lượt thực hành mới cần Assessment chưa chấm. Nếu Assessment đã có kết quả, tạo Exercise và Assessment mới bằng UI để giữ lại lịch sử điểm cũ.

### INSTRUCTOR

Trong **Exercises → View → Quản lý vòng đời Exercise**, dùng **Xác nhận sẵn sàng**, **Bắt đầu thực hành**, **Tạm dừng**, **Tiếp tục**, **Dừng thực hành** hoặc **Hoàn thành** theo trạng thái hiện tại. Với kịch bản SSH, chuẩn bị Assessment và đợi Lab báo READY trước khi xác nhận sẵn sàng. Khi dừng hoặc hoàn thành, giữ trang mở để giao diện đợi Runner xác nhận STOPPED. Nếu đã tải lại trang trong lúc đợi, chọn lại thao tác sau khi Lab dừng. Hoàn thành yêu cầu các Assessment đã được duyệt.

Trong **Điều phối bài thực hành**, chọn đội RED và đọc điều kiện cạnh nút duyệt. Backend kiểm tra phiên Lab, Assessment và phân công học viên. Nếu báo schema bài thực hành chưa sẵn sàng, quản trị viên kiểm tra migration 002 và 003 theo mục Chuẩn bị môi trường.

1. Vào **Lab Workflow**, xác nhận Exercise và Scenario, mở Assessment và bấm **Khởi động Lab**. Kiểm tra client, Ubuntu Target, SSH và Collector đều đạt.
2. Trong **Điều phối bài thực hành**, chọn đội RED rồi bấm **Duyệt lượt cho phiên Lab hiện tại**. Học viên sẽ thấy bài trong menu **Bài thực hành**.
3. Khi BLUE gửi đề xuất, đọc log, nhận định và nội dung đề xuất. Nhập nhận xét rồi chọn **Duyệt phản ứng mô phỏng** hoặc **Trả lại đề xuất**. Khi duyệt, bảng log phục hồi mô phỏng mở cho BLUE.
4. Khi BLUE nộp Incident Report, mở báo cáo, đánh giá chất lượng phân tích và báo cáo, nhập nhận xét, bấm **Duyệt điểm thực hành**. Kiểm tra điểm và Evidence có nhãn **Mô phỏng** trong **Assessments**. Dừng Lab sau khi hoàn thành và đợi `STOPPED`.

### RED TEAM

1. Đăng nhập và vào **Bài thực hành**, chọn lượt được giao. Đọc mục tiêu, Target và trạng thái Lab.
2. Bấm **Kiểm tra kết nối**. Hệ thống chạy một lượt xác thực được cấp quyền thông qua Linux client trong phiên. Chờ trạng thái **Đã xác nhận**.
3. Bấm **Bắt đầu tình huống mô phỏng**. Xác nhận UI báo dữ liệu đã được phát hành cho BLUE và xem lịch sử thao tác. Nếu kiểm tra kết nối thất bại, giảng viên kiểm tra phiên Lab trước khi mở lại bài.

### BLUE TEAM

1. Mở **Bài thực hành**. Dùng **Event SSH quan sát thật** để xem dữ liệu của Collector. Khi RED phát hành bài, đọc Alert và bảng **Authentication Events mô phỏng**. Bấm **Xác nhận đã nhận diện sự cố** để ghi mốc thời gian học viên.
2. Chọn các bản ghi làm căn cứ, tự nhập IP nguồn, tài khoản, máy chủ, phạm vi ảnh hưởng và phân tích. Bấm **Lưu điều tra** rồi **Gửi đề xuất**.
3. Khi được duyệt, đọc **Log phục hồi mô phỏng**, chọn kết luận và viết Incident Report. Có thể **Lưu nháp** và tải lại bài để tiếp tục. Điền đủ các mục rồi bấm **Nộp báo cáo và đề xuất đóng sự cố**.
4. Sau review, xem lý do và điểm từng tiêu chí trong **Bài thực hành** hoặc xem Assessment cùng Evidence. Có thể xem lại kết quả sau khi giảng viên dừng phiên.

### Đối chiếu khi demo

Trình diễn trên trình duyệt với ba tài khoản theo thứ tự INSTRUCTOR → RED → BLUE → INSTRUCTOR. Phần kiểm tra kết nối của RED tạo Event quan sát thật qua OpenSSH và Collector. Bảng điều tra và kết quả phản ứng của tình huống mang nhãn **Mô phỏng**. Bằng chứng của bài chỉ được công bố sau khi duyệt, gắn với lượt và Team Assessment. Thời gian học viên nhận diện và đề xuất phản ứng được đo riêng với thời gian xử lý cảnh báo và phản ứng mô phỏng.

Nếu menu **Bài thực hành** báo chưa bật, kiểm tra migration và biến môi trường Backend. Nếu học viên thấy danh sách trống, kiểm tra phân công trong Exercise và lượt đã được giảng viên duyệt. Nếu kiểm tra kết nối dừng ở Đang chờ, xác nhận runner dùng cùng cấu hình `SSH_PRACTICE_ENABLED`, token và Lab đang READY.

## API

Các đường dẫn bên dưới dùng tiền tố `/api/v1`. API vận hành yêu cầu JWT của ADMIN hoặc INSTRUCTOR. Kênh agent trên loopback dùng header `X-Lab-Credential` tương ứng.

| Phương thức và đường dẫn | Dữ liệu hoặc hành vi |
|---|---|
| `GET /exercises/{exerciseId}/assessment-criteria-status` | Criteria hợp lệ và tổng điểm theo task |
| `POST /exercises/{exerciseId}/assessments/initialize?teamId={teamId}` | Khởi tạo hoặc trả lại Assessment của Team BLUE |
| `GET /exercises/{exerciseId}/ssh-lab` | Phiên gần nhất, heartbeat và readiness |
| `POST /exercises/{exerciseId}/ssh-lab/start`, `/stop` | Tạo yêu cầu, start nhận `assessmentId` |
| `GET /events?exerciseId={exerciseId}` | Authentication Events đã ghi nhận và Event của Exercise |
| `GET /exercises/{exerciseId}/security-alerts?status={status}` | Tối đa 100 Alert gần nhất, lọc trạng thái tùy chọn |
| `GET /exercises/{exerciseId}/security-alerts/{alertId}` | Alert, Event, lịch sử và Incident Report |
| `POST /exercises/{exerciseId}/security-alerts/{alertId}/actions` | `ACKNOWLEDGE`, `ANALYZE`, `REQUEST_CONTAINMENT`, `SUBMIT_REPORT`, `CLOSE` |
| `POST /exercises/{exerciseId}/security-alerts/{alertId}/assessment` | Đánh giá từ Evidence, tính điểm và review tùy chọn |
| `GET /assessments/{assessmentId}/results`, `GET /assessment-scores/{scoreId}/evidence` | Kết quả task, Criteria và Evidence liên quan |
| `GET /lab-agent/desired`, `POST /lab-agent/{id}/heartbeat` | Runner lấy yêu cầu và báo kết quả probe |
| `GET /lab-agent/{id}/collector-health`, `POST /lab-agent/{id}/ssh-events` | Kiểm tra quyền Collector và nạp batch Authentication Event |
| `GET /lab-agent/{id}/containment`, `POST /lab-agent/{id}/containment/{alertId}` | Runner nhận yêu cầu cô lập cố định và trả kết quả xác minh |

## API bài thực hành

| Đường dẫn | Vai trò và chức năng |
|---|---|
| `GET /api/v1/ssh-practice` | STUDENT xem các lượt đã phân công theo Exercise và Team. WHITE xem ở chế độ quan sát. |
| `GET /api/v1/assessments/{id}/ssh-practice` | ADMIN/INSTRUCTOR xem tiến độ và bài nộp của Assessment. |
| `POST /api/v1/assessments/{id}/ssh-practice` | ADMIN/INSTRUCTOR mở lượt cho đội RED đã phân công, Assessment BLUE và Lab hiện tại. |
| `POST /api/v1/ssh-practice/{id}/actions` | Nhận thao tác định sẵn. Backend kiểm tra phân công, vai trò và trạng thái trước khi ghi. |
| `POST /api/v1/lab-agent/{session}/practice-connection` và `/{id}` | Runner nhận lệnh kiểm tra kết nối cố định và báo kết quả qua credential riêng. |

Thao tác lớp học dùng chung phiên Lab và Team Assessment. Một Assessment chỉ có một lượt được công bố. Khi tắt `SSH_PRACTICE_ENABLED`, API thực hành tạm ngừng và dữ liệu đã lưu được giữ lại. Luồng Event và Alert vận hành vẫn sử dụng API hiện có.

## Nghiệm thu và xử lý lỗi

1. Đăng nhập bằng ADMIN/INSTRUCTOR. Chọn Exercise ở trạng thái `CREATED` hoặc `READY`, xác nhận Scenario, chọn Team BLUE và khởi tạo Assessment. Đối chiếu bảy task và tổng 100 điểm.
2. Bấm **Khởi động Lab** và đợi **READY**. Với mã phiên trên UI, chạy `docker compose -f cyber-range-lab/compose.yaml -p cyber-range-ssh-<mã phiên> ps` để đối chiếu trạng thái. Có thể dùng `logs event-collector` với cùng tùy chọn project để tra lỗi.
3. Dùng một lượt đăng nhập được cấp quyền từ client Lab đã định sẵn bằng lệnh bên dưới. Thay phần trong dấu ngoặc nhọn bằng mã phiên hiển thị trên UI. Bấm **Tải sự kiện gần nhất** để xác nhận Event `SSH_AUTH_SUCCESS`. Các tình huống nhiều lỗi chạy bằng fixture trong database kiểm thử riêng.

   ```powershell
   docker compose -f cyber-range-lab/compose.yaml -p cyber-range-ssh-{ma-phien} exec -T lab-client ssh -F /dev/null -o BatchMode=yes -o IdentitiesOnly=yes -o UserKnownHostsFile=/run/lab-client/known_hosts -i /run/lab-client/key lab-user@ubuntu-target true
   ```
4. Khi có Alert phù hợp, tiếp nhận, xem log, lưu phân tích và yêu cầu cô lập. Kiểm tra lịch sử có `CONTAINMENT_VERIFIED` và `RECOVERY_VERIFIED`. Nộp báo cáo và đóng Alert.
5. Đối chiếu phân tích với Event, nhập tài khoản và host tìm thấy trong log, chạy điểm tạm thời hoặc xác nhận điểm cuối. Xem từng Criterion và Evidence trong Assessment. Detection Time đo từ Event đầu của cửa sổ phát hiện. Response Time đo đến lúc xác minh hành động phòng thủ đầu tiên. Điểm review được chuẩn hóa theo điểm tối đa của các task áp dụng.
6. Bấm **Dừng Lab** và đợi `STOPPED`.

Nếu khởi tạo Assessment báo thiếu Criteria, xem API trạng thái và kiểm tra seed đã được phê duyệt. Nếu mất readiness, kiểm tra heartbeat của runner, Compose health và hai token ở cả Backend lẫn runner. Nếu Collector mất kết nối, runner lùi thời gian thử tới 30 giây và giữ hàng đợi trên volume. Với rotation, đổi tên `auth.log` thành `auth.log.1`, cho rsyslog mở lại file, và giữ file cũ tới khi Collector đọc xong. Tránh copy-truncate hoặc quay lần kế tiếp khi Collector còn backlog.

Để chạy test tự động, chuẩn bị PostgreSQL riêng chỉ lắng nghe trên `127.0.0.1:55439`, database `cyber_range_test`. Chạy theo thứ tự `database/database.sql`, migration, `src/test/resources/ssh/exercise-configuration.sql` và seed. Đặt `DATABASE_URL=jdbc:postgresql://127.0.0.1:55439/cyber_range_test` cùng credential riêng, rồi chạy `.\mvnw.cmd test`. Fixture có guard kiểm tra chính xác tên database và cổng. Chạy `npm run build` trong `frontend`.

## Rollback

Dừng phiên SSH Lab bằng UI và đợi runner báo `STOPPED`. Nếu Backend hoặc runner không khả dụng, dùng Compose `stop` với đúng project của phiên. Khởi động phiên bản ứng dụng trước và giữ các bảng, cột và volume bổ sung để bảo toàn lịch sử. Khi phải khôi phục bản backup, xác định trước thời điểm phục hồi và dữ liệu phát sinh cần xuất giữ. Dùng database phục hồi riêng để kiểm tra trước khi chuyển ứng dụng sang bản phục hồi.
