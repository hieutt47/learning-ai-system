# Backend Java

Spring Boot, Java 17, Maven, Spring Security/JWT, JPA/Hibernate và MySQL. Package chính vn.edu.learningai gồm controller, service, repository, entity, dto, mapper, security và ai.

## Chạy local

Tạo database MySQL và cấu hình DBMS_CONNECTION, DBMS_USERNAME, DBMS_PASSWORD, JWT_SIGNING_KEY theo .env.example. JWT_SIGNING_KEY là Base64 của khóa ngẫu nhiên tối thiểu 32 byte. Chạy HTTP local đặt AUTH_COOKIE_SECURE=false.

Backend đọc biến môi trường của tiến trình, không tự nạp .env. Khai báo trong Run Configuration của IntelliJ rồi chạy LearningaiApplication, hoặc nạp biến môi trường trước khi chạy Maven:

```bash
./mvnw spring-boot:run
```

Port mặc định 8080. Flyway lấy migration từ database/migrations qua Maven resources; Hibernate dùng ddl-auto=validate. Seed demo trong database/seeds chạy sau migration.

## Xác minh email

V4 thêm users.email_verified_at và email_verification_tokens. User đã tồn tại trước migration được đánh dấu đã xác minh; đăng ký mới cần mở link email và xác nhận trước khi đăng nhập. Seed demo có email_verified_at.

| Biến | Giá trị local mặc định | Ý nghĩa |
|---|---|---|
| MAIL_HOST | localhost | Máy chủ SMTP |
| MAIL_PORT | 1025 | Port SMTP |
| MAIL_USERNAME / MAIL_PASSWORD | rỗng | Tài khoản SMTP nếu cần |
| MAIL_SMTP_AUTH | false | Bật xác thực SMTP |
| MAIL_STARTTLS | false | Bật STARTTLS theo máy chủ |
| MAIL_FROM | learningai@example.test | Địa chỉ gửi email |
| APP_FRONTEND_BASE_URL | http://localhost:5173 | Gốc URL frontend trong link xác minh |

SMTP local phải chạy ở host/port đã cấu hình; SMTP thật cần địa chỉ gửi và thông tin xác thực hợp lệ. APP_FRONTEND_BASE_URL phải trùng địa chỉ frontend mà người nhận mở được, kể cả khi Vite đổi port.

Email có bản HTML và plain text; template ở src/main/resources/templates/verification-email.html. Link có hạn 24 giờ, frontend tự gửi POST xác minh khi mở link có token; không tự đăng nhập sau xác minh. Lỗi SMTP trả 503 EMAIL_SEND_FAILED và rollback dữ liệu của thao tác đăng ký/gửi lại.

## Quên mật khẩu

V5 thêm password_reset_tokens. POST /api/v1/auth/forgot-password nhận email, trả 204 chung cho email hợp lệ về định dạng; chỉ tài khoản active đã xác minh được gửi link. Token ngẫu nhiên 32 byte, chỉ lưu hash SHA-256, hạn 30 phút và dùng một lần. Gửi link mới vô hiệu hóa các link cũ; lỗi SMTP trả 503 EMAIL_SEND_FAILED và rollback thao tác. Email dùng cấu hình SMTP hiện có, APP_FRONTEND_BASE_URL và template src/main/resources/templates/password-reset-email.html; link trỏ tới /reset-password?token=... trên frontend.

POST /api/v1/auth/reset-password nhận token và new_password dài 10–72 ký tự, khác mật khẩu hiện tại. Thành công trả 204, lưu hash BCrypt mới, vô hiệu hóa mọi link đặt lại mật khẩu, thu hồi tất cả refresh session của tài khoản và xóa cookie refreshToken; không tự đăng nhập. Hai endpoint không yêu cầu Bearer token; client không gửi Authorization. Access token cũ còn hiệu lực đến exp nếu user active. Đổi mật khẩu khi đang đăng nhập vẫn giữ nguyên các phiên. Frontend tích hợp tại /forgot-password và /reset-password, dùng auth: false. Đặt lại thành công xóa phiên/cache trong trình duyệt, xóa các ô mật khẩu và token khỏi URL rồi hiển thị nút đăng nhập.

## Kiểm thử

```bash
./mvnw test
```

Cấu hình mặc định loại nhóm integration dùng MySQL. Test xác minh email và quên mật khẩu dùng mail sender giả, không gửi email thật. Test API quên mật khẩu chạy qua SecurityFilterChain thực với service giả. Chỉ chạy integration trên database test riêng.
