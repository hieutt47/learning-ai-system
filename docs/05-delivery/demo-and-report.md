# Demo và báo cáo

Tài khoản demo: admin, teacher A/B, student A/B; credential cấu hình riêng. Video YouTube mẫu và tài liệu do nhóm được phép sử dụng. AI mock dự phòng theo cùng schema; không trình bày mock như call provider thật.

## Demo chính

1. Admin duyệt Teacher request.
2. Teacher tạo khóa free, thêm ảnh, lesson/YouTube/transcript, publish.
3. Generate quiz preview, sửa một câu, lưu Draft, publish; tương tự flashcard nếu D07 giữ.
4. Student đăng ký/login, marketplace, enroll, video/document, quiz submit, result/review/history, flashcard.
5. Teacher xem thống kê; teacher B thử sửa khóa A bị chặn.
6. Retry submit trả cùng điểm; logout không dùng lại session.
7. Mở ERD/query và giải thích transaction, constraints, API portability.

## Hồ sơ bàn giao

- README setup/run sau implementation; môi trường và version pin.
- Yêu cầu, use cases, kiến trúc, ERD, data dictionary, migration/seed/query.
- OpenAPI hoàn chỉnh, screenshot UI, evidence các tình huống chính.
- Báo cáo phần Java: OOP, tầng, interfaces, JPA, security, exception, transaction.
- Báo cáo CSDL: keys/constraints, joins/aggregate, transaction, index/EXPLAIN.
- Kế hoạch Python và bằng chứng parity nếu đã triển khai.
- Slide, demo script, known limitations, đóng góp thực tế từng người từ issue/PR (không điền trước).

Runbook phải đủ để thành viên khác tạo DB test và demo từ đầu, có cách chọn mock/live AI và xử lý provider lỗi. Trước buổi bảo vệ chạy rehearsal theo đúng máy/mạng dự kiến.
