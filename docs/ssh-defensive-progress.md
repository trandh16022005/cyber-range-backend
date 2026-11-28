# Tiến độ SSH Security Incident Response

- Baseline `9b6f566` với working tree sạch. PostgreSQL nghiệp vụ chỉ được truy vấn ở chế độ đọc để xác nhận Scenario, Exercise, Team và schema.
- Đã nối Exercise, Scenario, Team BLUE, Criteria và Assessment với UI. Đã triển khai Lab runner, OpenSSH/rsyslog, Collector có checkpoint, ingestion và Detection Engine.
- Đã nối Alert, lịch sử phản ứng, Incident Report, Evidence, Assessment và Automated Scoring. API initialize giữ hành vi cũ khi không có tham số `teamId`.
- PostgreSQL cô lập đã qua migration hai lần và seed hai lần. Database trống đã khởi tạo được từ script trong repo.
- Kiểm thử Docker đã xác nhận mạng nội bộ, ba container có health check và các lượt xác thực thông thường đi từ log đến API. Workflow trên PostgreSQL kiểm thử đã nhận fixture phòng thủ, tạo Alert, chạy cô lập và probe phục hồi thật, đóng Alert và chấm lại 100/100.
- Edge headless hiển thị Exercise, Scenario, Assessment, Alert, Event liên quan, lịch sử phản ứng, điểm 100/100 và Evidence đã xác minh. Collector giữ một bản ghi cũ và nhận đúng một bản ghi mới sau log rotation và restart.
- Readiness giảm khi Collector tạm dừng và phục hồi khi Collector chạy lại. Runner trả `STOPPED` sau yêu cầu dừng. Phiên bản cuối của Backend chạy qua 5 test Maven với 0 lỗi, 0 test bị bỏ qua.
- Migration và seed chưa áp dụng vào `cyber_range_db`. Khi tiếp tục triển khai, dùng quy trình tại [Hướng dẫn vận hành](ssh-defensive-workflow.md).

## Tệp mã nguồn và tài liệu

### Tạo mới (28)

- `cyber-range-lab/collector.py`
- `cyber-range-lab/probe.sh`
- `cyber-range-lab/rsyslog-lab.conf`
- `cyber-range-lab/runner.py`
- `database/migrations/001_ssh_defensive_workflow.sql`
- `database/seeds/ssh_defensive_configuration.sql`
- `docs/ssh-defensive-progress.md`
- `docs/ssh-defensive-workflow.md`
- `frontend/src/features/assessments/ScoreEvidence.tsx`
- `frontend/src/features/lab-workflow/components/AssessmentSetup.tsx`
- `frontend/src/features/lab-workflow/components/SshLabOperations.tsx`
- `src/main/java/com/cyberrange/backend/assessment/service/SshAssessmentEvaluator.java`
- `src/main/java/com/cyberrange/backend/event/controller/SshObservationController.java`
- `src/main/java/com/cyberrange/backend/event/controller/SshResponseController.java`
- `src/main/java/com/cyberrange/backend/event/repository/SshAlertRepository.java`
- `src/main/java/com/cyberrange/backend/event/repository/SshObservationRepository.java`
- `src/main/java/com/cyberrange/backend/event/service/SshDetectionWindow.java`
- `src/main/java/com/cyberrange/backend/event/service/SshObservationService.java`
- `src/main/java/com/cyberrange/backend/event/service/SshResponseWorkflow.java`
- `src/main/java/com/cyberrange/backend/incident/repository/IncidentReportRepository.java`
- `src/main/java/com/cyberrange/backend/lab/controller/SshLabController.java`
- `src/main/java/com/cyberrange/backend/lab/repository/SshLabRepository.java`
- `src/main/java/com/cyberrange/backend/lab/service/SshLabService.java`
- `src/main/java/com/cyberrange/backend/lab/service/SshRunnerCredential.java`
- `src/test/java/com/cyberrange/backend/event/service/SshDetectionWindowTest.java`
- `src/test/java/com/cyberrange/backend/event/service/SshWorkflowIntegrationTest.java`
- `src/test/resources/ssh/auth-failures.log`
- `src/test/resources/ssh/exercise-configuration.sql`

### Chỉnh sửa (24)

- `.gitattributes`
- `cyber-range-lab/Dockerfile`
- `cyber-range-lab/compose.yaml`
- `cyber-range-lab/start.sh`
- `database/database.sql`
- `frontend/src/features/assessments/AssessmentDetailPage.tsx`
- `frontend/src/features/assessments/assessment.types.ts`
- `frontend/src/features/assessments/assessmentService.ts`
- `frontend/src/features/lab-workflow/LabWorkflowPage.tsx`
- `frontend/src/features/lab-workflow/components/ScenarioSelector.tsx`
- `frontend/vite.config.ts`
- `src/main/java/com/cyberrange/backend/assessment/controller/AssessmentController.java`
- `src/main/java/com/cyberrange/backend/assessment/dto/AssessmentResponse.java`
- `src/main/java/com/cyberrange/backend/assessment/entity/AssessmentResult.java`
- `src/main/java/com/cyberrange/backend/assessment/repository/AssessmentRepository.java`
- `src/main/java/com/cyberrange/backend/assessment/service/AssessmentScoreEvidenceService.java`
- `src/main/java/com/cyberrange/backend/assessment/service/AssessmentService.java`
- `src/main/java/com/cyberrange/backend/assessment/service/AutomatedScoringService.java`
- `src/main/java/com/cyberrange/backend/config/SecurityConfig.java`
- `src/main/java/com/cyberrange/backend/event/controller/EventController.java`
- `src/main/java/com/cyberrange/backend/evidence/entity/Evidence.java`
- `src/main/java/com/cyberrange/backend/evidence/service/EvidenceService.java`
- `src/main/java/com/cyberrange/backend/exercise/repository/ExerciseRepository.java`
- `src/main/resources/application.properties`


## Nghiệm thu trên môi trường chính — INCOMPLETE

- Backend khởi động trên `127.0.0.1:8080` với `cyber_range_db/public`. Hibernate validation chỉ đọc đã đạt ở phiên trước. Vite trên `127.0.0.1:5173` trả HTTP 200 cho trang đăng nhập và chuyển tiếp API được bảo vệ trả HTTP 401 khi chưa đăng nhập.
- Runner demo đang chạy với hai token cấp qua môi trường tiến trình. Truy vấn PostgreSQL xác nhận runner đã đọc trạng thái phiên. Exercise 3 có Assessment 3 ở trạng thái `PENDING`; tại thời điểm kiểm tra chưa có phiên SSH Lab hoặc Alert.
- Build frontend và cấu hình Compose đạt. Maven có 3 unit test đạt và 2 test PostgreSQL được bỏ qua vì cụm kiểm thử chưa được cấu hình trong phiên này.
- Cần đăng nhập bằng ADMIN hoặc INSTRUCTOR để nghiệm thu các màn hình có bảo vệ, khởi động Lab và kiểm tra Collector với sự kiện xác thực hợp lệ. Phần Alert, phản ứng và chấm điểm trên database chính cần một Alert hợp lệ từ hoạt động Lab thực tế.
- Database chính đã nhận migration và seed trong các lần triển khai được duyệt trước. Lượt nghiệm thu này chỉ đọc database chính.
