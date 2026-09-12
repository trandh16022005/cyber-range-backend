# 1. Đặc tả đề tài

## 1.1. Thông tin chung

### Tên đề tài

**XÂY DỰNG CYBER RANGE VÀ HỆ THỐNG TỰ ĐỘNG ĐÁNH GIÁ KỸ NĂNG PHÒNG THỦ**

### Tên tiếng Anh

**Cyber Range and Automated Defensive Skills Assessment System**

### Tên hệ thống Web

**SecureLab – Cybersecurity Training and Assessment Platform**

---

## 1.2. Mô tả đề tài

Đề tài xây dựng một môi trường **Cyber Range** mô phỏng hệ thống mạng doanh nghiệp, phục vụ cho việc đào tạo, thực hành và đánh giá kỹ năng phòng thủ an toàn thông tin.

Cyber Range cung cấp môi trường mô phỏng gồm nhiều máy chủ và dịch vụ như:

* Web Server
* Linux Server
* Windows Server
* Database
* SSH
* Active Directory

Trong môi trường này, người học được tổ chức thành các nhóm **Red Team** và **Blue Team**.

**Red Team** thực hiện các hoạt động tấn công nhằm tạo ra các tình huống an toàn thông tin cần được phát hiện và xử lý.

**Blue Team** thực hiện các hoạt động giám sát, phát hiện, ứng phó, ngăn chặn và khôi phục hệ thống.

Hệ thống Cyber Range thu thập các dữ liệu phát sinh trong quá trình thực hành, bao gồm:

* Network logs
* Web access logs
* Authentication logs
* SSH logs
* Windows/Linux system logs
* Firewall events
* Security events
* System/service status

Các dữ liệu này được sử dụng làm cơ sở cho **Assessment Engine** xác định kết quả thực hiện nhiệm vụ của Blue Team.

Hệ thống tiếp tục sử dụng **Scoring Engine** để tính điểm dựa trên các tiêu chí được định nghĩa trước, đồng thời ghi nhận các chỉ số như:

* Trạng thái hoàn thành nhiệm vụ
* Thời gian phát hiện
* Thời gian phản ứng
* Điểm số
* Kết quả xử lý sự cố
* Báo cáo incident

Kết quả đánh giá được hiển thị trên nền tảng Web để người học và giảng viên theo dõi.

Hệ thống được thiết kế theo hướng có khả năng mở rộng, cho phép bổ sung các attack scenario, defensive task và tiêu chí đánh giá mới trong tương lai.

---

# 2. Lý do chọn đề tài và tính cấp thiết

Trong bối cảnh các cuộc tấn công mạng ngày càng gia tăng và đa dạng, nhu cầu đào tạo và nâng cao kỹ năng phòng thủ an toàn thông tin ngày càng trở nên cần thiết.

Việc học lý thuyết hoặc thực hành trên các hệ thống đơn lẻ chưa đủ để mô phỏng đầy đủ quá trình tấn công, phát hiện, ứng phó và khôi phục trong một hệ thống mạng thực tế.

Cyber Range cung cấp môi trường mô phỏng cho phép xây dựng các hệ thống mạng và máy chủ tương tự môi trường doanh nghiệp. Qua đó, người học có thể thực hành các hoạt động tấn công và phòng thủ trong một môi trường được kiểm soát.

Tuy nhiên, việc đánh giá kỹ năng phòng thủ trong các bài thực hành thường phụ thuộc nhiều vào người hướng dẫn và quá trình chấm điểm thủ công. Điều này gây khó khăn trong việc:

* Đánh giá khách quan kết quả thực hành.
* Xác định chính xác nhiệm vụ đã hoàn thành.
* Theo dõi thời gian phát hiện sự cố.
* Theo dõi thời gian phản ứng.
* Đánh giá khả năng xử lý và khôi phục hệ thống.
* Thực hiện đánh giá nhất quán giữa nhiều người học hoặc nhiều lần thực hành.

Vì vậy, đề tài **“Xây dựng Cyber Range và hệ thống tự động đánh giá kỹ năng phòng thủ”** được thực hiện nhằm kết hợp môi trường Cyber Range với cơ chế thu thập sự kiện, tự động đánh giá và chấm điểm kỹ năng phòng thủ của Blue Team.

Đề tài hướng tới việc xây dựng một nền tảng hỗ trợ đào tạo thực hành an toàn thông tin, giúp người học rèn luyện kỹ năng phòng thủ trong môi trường mô phỏng và cung cấp phương thức đánh giá tự động, nhất quán.

---

# 3. Mục tiêu đề tài

## 3.1. Mục tiêu tổng quát

Xây dựng một hệ thống Cyber Range mô phỏng môi trường mạng doanh nghiệp, cho phép thực hiện các kịch bản tấn công và phòng thủ, kết hợp hệ thống tự động đánh giá và chấm điểm kỹ năng phòng thủ của Blue Team dựa trên các log, security event và trạng thái hệ thống.

## 3.2. Mục tiêu cụ thể

Đề tài hướng tới các mục tiêu sau:

1. Xây dựng môi trường Cyber Range mô phỏng hệ thống mạng doanh nghiệp.

2. Xây dựng các máy chủ và dịch vụ phù hợp với phạm vi đề tài, bao gồm Web Server, Linux Server, Database, SSH và các thành phần Windows/Active Directory theo phạm vi triển khai.

3. Xây dựng các attack scenario cơ bản cho Red Team.

4. Xây dựng các defensive task tương ứng cho Blue Team.

5. Thu thập và quản lý log, security event và trạng thái hệ thống.

6. Xây dựng cơ chế xác định kết quả thực hiện nhiệm vụ phòng thủ.

7. Xây dựng **Assessment Engine** để tự động đánh giá kết quả.

8. Xây dựng **Scoring Engine** để tự động tính điểm.

9. Ghi nhận các chỉ số đánh giá như thời gian phát hiện và thời gian phản ứng.

10. Xây dựng nền tảng Web để quản lý người dùng, nhóm, scenario, nhiệm vụ và kết quả.

11. Sinh báo cáo kết quả thực hành.

12. Thiết kế hệ thống có khả năng mở rộng thêm scenario, defensive task và tiêu chí đánh giá.

---

# 4. Phạm vi và đối tượng nghiên cứu

## 4.1. Phạm vi nghiên cứu

Đề tài tập trung xây dựng một Cyber Range quy mô nhỏ phục vụ đào tạo và đánh giá kỹ năng phòng thủ.

### Phạm vi chức năng chính

Hệ thống bao gồm:

* Quản lý người dùng.
* Quản lý Red Team và Blue Team.
* Quản lý Cyber Range.
* Quản lý attack scenario.
* Quản lý defensive task.
* Thu thập log và security event.
* Theo dõi trạng thái hệ thống.
* Tự động đánh giá nhiệm vụ.
* Tự động chấm điểm.
* Theo dõi thời gian phát hiện và phản ứng.
* Sinh báo cáo.
* Hiển thị kết quả trên Web.

### Phạm vi Cyber Range

Môi trường Cyber Range được thiết kế theo mô hình mạng doanh nghiệp thu nhỏ, có thể bao gồm:

* Web Server
* Linux Server
* Windows Server
* Database
* SSH
* Active Directory

Việc triển khai thực tế có thể sử dụng Virtual Machine hoặc Container tùy thuộc vào điều kiện tài nguyên và phạm vi triển khai của nhóm.

---

## 4.2. Phạm vi phiên bản đầu tiên — V1

Để đảm bảo khả năng hoàn thành đề tài, phiên bản đầu tiên tập trung vào một số scenario tiêu biểu.

### Red Team V1

* Port Scanning
* SSH Brute Force
* Web Attack

### Blue Team V1

* Phân tích log.
* Phát hiện tấn công.
* Xác định máy bị compromise.
* Cô lập máy bị tấn công.
* Khôi phục dịch vụ.
* Lập báo cáo incident.

### Assessment V1

Assessment Engine cần có khả năng:

* Tự động xác định nhiệm vụ đã hoàn thành.
* Ghi nhận thời gian phát hiện.
* Ghi nhận thời gian phản ứng.
* Xác định kết quả của defensive task.
* Chuyển kết quả sang Scoring Engine.

### Scoring V1

Scoring Engine tính điểm dựa trên các tiêu chí được định nghĩa trước.

| Tiêu chí đánh giá         |    Điểm |
| ------------------------- | ------: |
| Phát hiện port scan       |      10 |
| Phát hiện brute force     |      10 |
| Phát hiện web attack      |      15 |
| Xác định compromised host |      15 |
| Cô lập máy bị tấn công    |      20 |
| Khôi phục dịch vụ         |      20 |
| Báo cáo incident          |      10 |
| **Tổng**                  | **100** |

Các tiêu chí và trọng số trên được xem là **bộ tiêu chí mẫu cho V1** và có thể được cấu hình/mở rộng trong thiết kế hệ thống.

---

# 5. Đối tượng nghiên cứu

Đối tượng nghiên cứu của đề tài bao gồm:

### 5.1. Cyber Range

* Công nghệ Cyber Range.
* Môi trường mô phỏng hệ thống mạng.
* Mô hình triển khai máy chủ và dịch vụ.

### 5.2. Red Team và Blue Team

* Mô hình hoạt động Red Team.
* Mô hình hoạt động Blue Team.
* Mối quan hệ giữa hoạt động tấn công và hoạt động phòng thủ.

### 5.3. Tấn công và phòng thủ

Các kỹ thuật tấn công và phòng thủ phù hợp với phạm vi của Cyber Range, đặc biệt là các scenario được lựa chọn cho V1.

Red Team được mô hình hóa theo các giai đoạn:

1. Reconnaissance
2. Scanning
3. Exploitation
4. Privilege Escalation
5. Persistence

Blue Team được mô hình hóa theo các hoạt động:

1. Log Analysis
2. Detection
3. Incident Response
4. Blocking
5. Recovery

Trong V1, nhóm tập trung trước hết vào các hoạt động cần thiết cho Port Scanning, SSH Brute Force và Web Attack.

### 5.4. Log và Security Event

Nghiên cứu cơ chế:

* Thu thập log.
* Quản lý log.
* Thu thập security event.
* Theo dõi trạng thái hệ thống.
* Sử dụng event làm bằng chứng đánh giá.

### 5.5. Automated Assessment

Nghiên cứu phương pháp xác định một defensive task đã được hoàn thành dựa trên:

* Log.
* Event.
* System state.
* Service state.
* Các hành động của Blue Team.

### 5.6. Scoring

Nghiên cứu phương pháp:

* Định nghĩa tiêu chí.
* Gán trọng số.
* Tính điểm.
* Theo dõi kết quả.
* Sinh báo cáo đánh giá.

---

# 6. Phương pháp nghiên cứu

## 6.1. Nghiên cứu tài liệu

Tìm hiểu các tài liệu liên quan đến:

* Cyber Range.
* Red Team.
* Blue Team.
* Incident Response.
* Log Management.
* Security Event.
* Cybersecurity Training.
* Automated Assessment.
* Scoring trong Cyber Range.

## 6.2. Phân tích và thiết kế hệ thống

Thực hiện:

* Phân tích yêu cầu.
* Xác định các thành phần hệ thống.
* Thiết kế kiến trúc.
* Thiết kế cơ sở dữ liệu.
* Thiết kế các module.
* Thiết kế API.
* Thiết kế cơ chế Assessment.
* Thiết kế Scoring.
* Thiết kế Web Platform.

## 6.3. Thực nghiệm

Xây dựng môi trường Cyber Range trong môi trường ảo hóa hoặc container.

Tiến hành:

1. Khởi tạo Cyber Range.
2. Khởi tạo attack scenario.
3. Red Team thực hiện scenario.
4. Thu thập log/event.
5. Blue Team thực hiện defensive task.
6. Tiếp tục thu thập event và system state.
7. Assessment Engine phân tích kết quả.
8. Scoring Engine tính điểm.
9. Sinh báo cáo.
10. Kiểm tra kết quả trên Web Platform.

## 6.4. Đánh giá

Đánh giá hệ thống dựa trên:

* Khả năng thu thập log/event.
* Khả năng xác định nhiệm vụ hoàn thành.
* Độ chính xác của cơ chế assessment.
* Khả năng tính điểm.
* Thời gian phát hiện.
* Thời gian phản ứng.
* Khả năng sinh báo cáo.
* Khả năng mở rộng scenario và tiêu chí.

---

# 7. Kiến trúc khái niệm

Hệ thống được tổ chức theo luồng tổng quát:

```text
                         CYBER RANGE
                              |
              +---------------+---------------+
              |               |               |
         Web Server      Linux Server    Windows Server
              |               |               |
          Database           SSH        Active Directory
              |               |               |
              +---------------+---------------+
                              |
                       Logs / Events
                              |
              +---------------+---------------+
              |                               |
          RED TEAM                        BLUE TEAM
              |                               |
      Reconnaissance                    Log Analysis
      Scanning                          Detection
      Exploitation                      Incident Response
      Privilege Escalation              Blocking
      Persistence                       Recovery
              |                               |
              +---------------+---------------+
                              |
                    Assessment Engine
                              |
                    Scoring Engine
                              |
              +---------------+---------------+
              |               |               |
            Score           Report          Metrics
                              |
                              v
                       WEB PLATFORM
```

Đây là **kiến trúc khái niệm ở mức đặc tả**, chưa phải kiến trúc triển khai kỹ thuật cuối cùng.

Kiến trúc chi tiết sẽ được xác định trong **`docs/02-architecture.md`** ở bước tiếp theo.

---

# 8. Thành phần chức năng chính

## 8.1. Cyber Range

Cyber Range cung cấp môi trường mô phỏng hệ thống mạng và máy chủ.

Các thành phần dự kiến:

* Web Server.
* Linux Server.
* Windows Server.
* Database.
* SSH.
* Active Directory.

Môi trường có thể được triển khai bằng Virtual Machine hoặc Container tùy phạm vi và tài nguyên.

---

## 8.2. Red Team

Red Team tạo ra các tình huống tấn công nhằm kiểm tra khả năng phòng thủ của Blue Team.

Các giai đoạn được mô hình hóa:

```text
Reconnaissance
      ↓
Scanning
      ↓
Exploitation
      ↓
Privilege Escalation
      ↓
Persistence
```

Trong V1, các scenario tập trung vào:

* Port Scanning.
* SSH Brute Force.
* Web Attack.

Các giai đoạn nâng cao sẽ được xem xét trong các phiên bản mở rộng.

---

## 8.3. Blue Team

Blue Team chịu trách nhiệm phát hiện và xử lý các cuộc tấn công.

Các hoạt động được mô hình hóa:

```text
Log Analysis
      ↓
Detection
      ↓
Incident Response
      ↓
Blocking
      ↓
Recovery
```

Trong V1, Blue Team thực hiện các nhiệm vụ:

* Phân tích log.
* Phát hiện tấn công.
* Xác định compromised host.
* Cô lập máy bị tấn công.
* Khôi phục dịch vụ.
* Lập báo cáo incident.

---

## 8.4. Log and Event Collection

Thành phần này thu thập dữ liệu từ Cyber Range.

Các loại dữ liệu dự kiến:

| Loại dữ liệu        | Ví dụ                             |
| ------------------- | --------------------------------- |
| Network Logs        | Network connection, traffic event |
| Web Logs            | Web access/request event          |
| Authentication Logs | Login/logout/failure              |
| SSH Logs            | SSH authentication event          |
| System Logs         | Linux/Windows system events       |
| Firewall Events     | Allow/deny events                 |
| Security Events     | Security-related events           |
| System State        | Trạng thái máy chủ                |
| Service State       | Trạng thái dịch vụ                |

Các dữ liệu này là đầu vào cho Assessment Engine.

---

# 9. Assessment Engine

**Assessment Engine là thành phần cốt lõi của hệ thống đánh giá.**

Assessment Engine có nhiệm vụ phân tích các dữ liệu được thu thập từ Cyber Range để xác định Blue Team đã hoàn thành defensive task hay chưa.

Luồng xử lý khái niệm:

```text
Red Team Attack
       ↓
Cyber Range
       ↓
Logs / Events / System State
       ↓
Blue Team Action
       ↓
Logs / Events / System State
       ↓
Assessment Engine
       ↓
Task Result
```

Ví dụ:

```text
Port Scan
    ↓
Cyber Range ghi nhận Event
    ↓
Blue Team phát hiện
    ↓
Assessment Engine kiểm tra bằng chứng
    ↓
Detection Task = Completed
    ↓
Scoring Engine
    ↓
+10 điểm
```

Assessment Engine không chỉ kiểm tra việc Blue Team có thực hiện một hành động hay không, mà cần hướng tới việc xác định kết quả dựa trên **bằng chứng có thể quan sát được từ hệ thống**.

Ví dụ các bằng chứng có thể được sử dụng:

* Log event.
* Security event.
* Authentication event.
* Network event.
* System state.
* Service state.
* Firewall state.
* Thời điểm xảy ra event.

Cách xác định bằng chứng cụ thể cho từng task sẽ được đặc tả chi tiết trong tài liệu Assessment ở các bước sau.

---

# 10. Scoring Engine

Scoring Engine nhận kết quả từ Assessment Engine và tính điểm dựa trên các tiêu chí được định nghĩa.

Luồng tổng quát:

```text
Assessment Result
       ↓
Scoring Rules
       ↓
Score Calculation
       ↓
Total Score
```

Các nhóm tiêu chí chính:

* Detection.
* Response.
* Isolation/Blocking.
* Recovery.
* Incident Report.

Kết quả scoring bao gồm:

* Điểm từng task.
* Tổng điểm.
* Trạng thái hoàn thành.
* Thời gian phát hiện.
* Thời gian phản ứng.

Bộ tiêu chí scoring phải được thiết kế theo hướng có thể mở rộng và cấu hình, thay vì hard-code toàn bộ tiêu chí trong mã nguồn.

---

# 11. Web Platform

Web Platform là giao diện quản lý và hiển thị kết quả của hệ thống.

Các chức năng chính dự kiến:

### Quản lý người dùng

* Quản lý user.
* Phân quyền.
* Quản lý nhóm.

### Quản lý Cyber Range

* Quản lý môi trường.
* Theo dõi trạng thái.

### Quản lý Scenario

* Quản lý attack scenario.
* Quản lý defensive task.
* Quản lý tiêu chí đánh giá.

### Thực hành

* Theo dõi scenario đang thực hiện.
* Theo dõi trạng thái nhiệm vụ.

### Assessment

* Xem task status.
* Xem điểm.
* Xem detection time.
* Xem response time.

### Reporting

* Xem báo cáo incident.
* Xem kết quả thực hành.
* Xem tổng điểm.
* Xem các chỉ số đánh giá.

---

# 12. Luồng hoạt động tổng thể

Quá trình thực hành được mô hình hóa như sau:

```text
1. Giảng viên tạo Scenario
             ↓
2. Hệ thống khởi tạo Cyber Range
             ↓
3. Red Team thực hiện Attack Scenario
             ↓
4. Cyber Range ghi nhận Logs / Events
             ↓
5. Blue Team phân tích và phòng thủ
             ↓
6. Hệ thống tiếp tục thu thập Events
             ↓
7. Assessment Engine phân tích kết quả
             ↓
8. Scoring Engine tính điểm
             ↓
9. Hệ thống tạo Report
             ↓
10. Sinh viên / Giảng viên xem kết quả
```

---

# 13. Mô hình đánh giá V1

Một bài thực hành V1 có thể được mô hình hóa:

```text
                    SCENARIO
                       |
             +---------+---------+
             |                   |
         RED TEAM            BLUE TEAM
             |                   |
          Attack              Defense
             |                   |
             +---------+---------+
                       |
                 Logs / Events
                       |
                System State
                       |
                       v
              Assessment Engine
                       |
                 Task Results
                       |
                       v
               Scoring Engine
                       |
             +---------+---------+
             |         |         |
           Score     Metrics   Report
             |         |         |
             +---------+---------+
                       |
                       v
                  Web Platform
```

---

# 14. Kết quả đầu ra của hệ thống

Sau mỗi bài thực hành, hệ thống cần có khả năng cung cấp:

### Task Result

* Task đã hoàn thành/chưa hoàn thành.
* Thời điểm hoàn thành.
* Bằng chứng đánh giá.

### Score

* Điểm từng tiêu chí.
* Tổng điểm.
* Tỷ lệ hoàn thành.

### Metrics

* Detection Time.
* Response Time.
* Thời gian xử lý.
* Các chỉ số liên quan đến quá trình phòng thủ.

### Report

* Thông tin scenario.
* Thông tin team/user.
* Các attack event.
* Các defensive task.
* Kết quả assessment.
* Điểm số.
* Metrics.
* Incident Report.

---

# 15. Khả năng mở rộng

Hệ thống được thiết kế theo hướng có thể mở rộng.

Các hướng mở rộng bao gồm:

### Attack Scenario

* Privilege Escalation.
* Persistence.
* Các Web Attack khác.
* Các Network Attack khác.
* Các scenario trên Windows.

### Cyber Range

* Windows Server.
* Active Directory.
* Nhiều máy chủ hơn.
* Nhiều mạng/phân đoạn mạng.
* Nhiều scenario đồng thời.

### Assessment

* Thêm defensive task.
* Thêm loại evidence.
* Thêm assessment rule.
* Đánh giá nhiều giai đoạn của một incident.

### Scoring

* Scoring theo thời gian.
* Scoring theo mức độ hoàn thành.
* Scoring theo nhiều tiêu chí.
* Custom scoring rule.

### Team Assessment

* Individual Assessment.
* Team Assessment.
* So sánh kết quả giữa các lần thực hành.

### Automation

* Automated Incident Response.
* Advanced Detection.
* Tự động hóa một phần quá trình xử lý sự cố.

### Nghiên cứu nâng cao

* Nghiên cứu ứng dụng AI/ML vào phát hiện và đánh giá sự kiện.

Các nội dung trên **không thuộc phạm vi bắt buộc của V1**, mà là hướng phát triển sau khi hoàn thành hệ thống cơ bản.

---

# 16. Ràng buộc và nguyên tắc của V1

Để kiểm soát phạm vi và đảm bảo khả năng hoàn thành, V1 tuân theo các nguyên tắc:

1. Chỉ triển khai số lượng scenario cần thiết để chứng minh mô hình Cyber Range và Automated Assessment.

2. Ưu tiên các scenario:

    * Port Scanning.
    * SSH Brute Force.
    * Web Attack.

3. Tập trung trọng tâm vào **Blue Team Assessment**, thay vì xây dựng một hệ thống tấn công quá phức tạp.

4. Assessment phải dựa trên các event, log hoặc system state có thể quan sát được.

5. Scoring phải có khả năng mở rộng.

6. Scenario và defensive task phải có khả năng quản lý từ hệ thống.

7. Kiến trúc phải cho phép bổ sung scenario mới mà không cần thay đổi lớn các module cốt lõi.

8. Các thành phần nâng cao như Privilege Escalation, Persistence, Windows/Active Directory đầy đủ, Automated Incident Response và AI/ML được xem là hướng mở rộng.

---

# 17. Tiêu chí hoàn thành V1

V1 được xem là đạt mục tiêu khi hệ thống có thể thực hiện được luồng tối thiểu:

```text
Create Scenario
      ↓
Start Cyber Range
      ↓
Red Team Attack
      ↓
Collect Events
      ↓
Blue Team Defense
      ↓
Collect Evidence
      ↓
Automatic Assessment
      ↓
Automatic Scoring
      ↓
Generate Report
      ↓
Display Result on Web
```

Cụ thể, hệ thống cần chứng minh được:

* Có thể tạo và quản lý scenario.
* Có thể khởi tạo bài thực hành.
* Có thể ghi nhận attack event.
* Có thể thu thập log/event phục vụ assessment.
* Có thể xác định defensive task hoàn thành.
* Có thể tự động tính điểm.
* Có thể ghi nhận detection time.
* Có thể ghi nhận response time.
* Có thể sinh báo cáo.
* Có thể hiển thị kết quả trên Web.
* Có khả năng bổ sung scenario và scoring rule mới.

---

# 18. Phạm vi ngoài V1

Các nội dung sau không được xem là yêu cầu bắt buộc của phiên bản đầu tiên:

* Triển khai đầy đủ tất cả giai đoạn Privilege Escalation.
* Triển khai đầy đủ Persistence.
* Hệ thống Windows/Active Directory quy mô lớn.
* Nhiều Cyber Range chạy đồng thời ở quy mô lớn.
* Automated Incident Response hoàn toàn tự động.
* Advanced Detection.
* Machine Learning/AI.
* Hệ thống SIEM hoàn chỉnh.
* Phân tích malware chuyên sâu.

Các nội dung này chỉ được triển khai khi các chức năng cốt lõi của V1 đã hoàn thành hoặc được lựa chọn làm hướng phát triển.

---

# 19. Định hướng kiến trúc hệ thống ở mức nghiệp vụ

Từ đặc tả trên, hệ thống có thể được chia thành các nhóm chức năng nghiệp vụ:

```text
SecureLab
│
├── User & Team Management
│
├── Cyber Range Management
│
├── Scenario Management
│   ├── Attack Scenario
│   └── Defensive Task
│
├── Event & Log Management
│
├── Assessment Engine
│
├── Scoring Engine
│
├── Metrics
│
├── Reporting
│
└── Web Platform
```

Đây chỉ là **phân rã nghiệp vụ ban đầu**.

Cách các module giao tiếp với nhau, cấu trúc backend, database, API và deployment sẽ được xác định trong tài liệu kiến trúc ở bước tiếp theo.

---

# 20. Tóm tắt đề tài

Đề tài xây dựng **SecureLab – Cybersecurity Training and Assessment Platform**, một nền tảng Cyber Range phục vụ đào tạo và đánh giá kỹ năng phòng thủ an toàn thông tin.

Hệ thống mô phỏng môi trường mạng doanh nghiệp, trong đó Red Team tạo ra các tình huống tấn công và Blue Team thực hiện các nhiệm vụ phòng thủ.

Cyber Range thu thập log, security event và trạng thái hệ thống trong quá trình thực hành. Các dữ liệu này được đưa vào Assessment Engine để tự động xác định kết quả thực hiện nhiệm vụ. Scoring Engine tiếp tục tính điểm dựa trên các tiêu chí được định nghĩa.

Kết quả cuối cùng được hiển thị trên Web dưới dạng:

```text
Attack Scenario
       +
Defensive Task
       +
Logs / Events
       +
System State
       ↓
Automated Assessment
       ↓
Scoring
       ↓
Metrics
       ↓
Report
```

Mục tiêu cuối cùng là xây dựng một nền tảng có khả năng hỗ trợ **Training + Cyber Range + Red Team/Blue Team + Automated Assessment + Scoring + Reporting**, đồng thời có kiến trúc đủ linh hoạt để mở rộng thành một Cybersecurity Training Platform trong tương lai.
