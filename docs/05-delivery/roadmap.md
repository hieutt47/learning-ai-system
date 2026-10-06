# Lộ trình theo cổng nghiệm thu

Chưa gắn ngày vì không biết deadline hoặc thời gian nhóm.

| Giai đoạn | Đầu ra | Điều kiện qua |
|---|---|---|
| 0 — Khung hiện tại | Tài liệu, ranh giới module, contract bản thiết kế, ERD, workflow | Lead đọc được và chỉ rõ điểm cần chốt; chưa có app |
| 1 — Chốt thiết kế | Toolchain, frontend, flashcard; OpenAPI/schema; SQL migration/seed; kế hoạch chia việc | Contract validate, DB tạo được, ownership/reviewer rõ |
| 2 — Nền tảng | Java chạy, auth/user/approval/category; frontend nền | Login/logout/quyền đúng, không lộ secret |
| 3 — MOOC core | Course/marketplace/enroll/lesson/video/documents | Guest → student ghi danh và học được |
| 4 — Assessment | Quiz/questions, attempt/result/history, giao diện | Teacher soạn → student submit → result; retry/race đúng |
| 5 — AI/flashcard/statistics | Mock trước, provider thật sau, review, stats | Output lỗi xử lý; không auto publish; demo không phụ thuộc AI live |
| 6 — Bàn giao Java/CSDL | Integration, báo cáo, slide, runbook, backup | Demo lặp lại từ DB sạch; evidence đầy đủ |
| 7 — Python | Adapter/ORM/router mới, frontend/contract/schema giữ | Cùng shared scenarios trên fixture tương đương |

Nếu thiếu thời gian: giảm đẹp UI hoặc AI nâng cao trước; đề xuất đổi flashcard phải có lead quyết định và cập nhật toàn bộ tài liệu. Không cắt phân quyền, tính toàn vẹn điểm hoặc khả năng demo luồng chính.
