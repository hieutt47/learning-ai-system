# Chuyển backend sang Python

Đổi implementation; không cần Java/Python gọi nhau. Một frontend trỏ backend được chọn bằng cấu hình.

| Java | Python | Tài sản giữ |
|---|---|---|
| Spring Controller + DTO | FastAPI router + Pydantic | Route, field, error, HTTP status |
| Application service + domain rules | Service + domain rules | Use case, validation, transaction boundary |
| Repository port + JPA adapter | Repository port + SQLAlchemy adapter | Query semantics, constraint, PostgreSQL schema |
| Spring Security/JWT | Auth dependency/JWT adapter | Role, session, ownership, token semantics |
| AI/storage adapters | AI/storage adapters | Candidate schema, provenance, file access |
| Java checks | Python checks | Fixtures và acceptance scenarios chung |

1. Chốt contract và DB migration Java ổn định; ghi tag/baseline khi nhóm có Git.
2. Chạy Java scenarios và lưu expected outcome; không lưu timestamp/token/hash/ID tạo ngẫu nhiên để so byte tuyệt đối.
3. Tạo DB test Python riêng từ cùng migration/fixture. Python validate schema, không tự tạo schema mới.
4. Implement từng use case cùng modules; chuẩn hóa validation FastAPI sang lỗi 400/conventions, không để default 422 khác Java.
5. Xác minh decimal/score, timezone, ID string, transaction/retry, quyền, file storage, password hash và JWT/session tương thích. Nếu không giữ phiên/hash khi cutover, phải có kế hoạch reset/relogin rõ trước dùng dữ liệu thật.
6. Trỏ frontend vào Python và chạy lại luồng demo; không sửa payload để chiều riêng backend.
7. Chỉ bổ sung text processing Python nâng cao sau parity; tách optional capabilities, không phá MVP contract.

Không chạy cả hai runner migration cùng lúc. Chuyển backend trên DB có dữ liệu cần backup, migration rehearsal và kiểm tra owner/FK/history. Bài tập demo có thể dùng DB sạch seeded; phải ghi rõ trường hợp đó không chứng minh cutover dữ liệu thật.
