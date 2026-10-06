# Migration, seed và truy vấn

Một nguồn SQL PostgreSQL trong `database/migrations/`; tên dự kiến `0001_initial.sql`, `0002_...sql`. Chưa tạo file SQL rỗng giả đã có schema. Khi triển khai, chọn runner hỗ trợ Java trước và Python sau dùng **cùng file và bảng lịch sử migration**. Nếu runner khác, phải xác minh lịch sử tương thích, không để hai runner apply lại.

Không dùng JPA ddl-auto=create/update ở môi trường nhóm; ORM validate schema. Không để SQLAlchemy create_all hoặc Alembic tạo schema song song. Áp migration một lần trước app startup; version/checksum có tracking; migration đã áp không sửa lại.

Thay đổi: ghi ADR nếu phá tương thích → cập nhật từ điển/ERD/contract → SQL migration → dữ liệu/ORM hai phía → kiểm tra DB mới và DB đã có dữ liệu. Với SQLite/H2 không thể chứng minh partial index/transaction giống PostgreSQL; kiểm tra DB thật cho các invariant liên quan.

Seed tách dev/demo/test. Fixtures có admin, 2 teacher, 2 student, teacher request, khóa free Published, khóa Draft, khóa archived, quiz Draft/Published/Closed, flashcard Draft/Published, attempt Submitted và bỏ trống. Password demo lấy từ cấu hình/test fixture, hash hợp lệ; không tái sử dụng mật khẩu thật. Không reset DB có dữ liệu nhóm khi chưa xác định đó là DB test.

Truy vấn cần chuẩn bị: marketplace số enrolled; average score/quiz; số lượt Submitted; câu sai nhiều (có bỏ trống); best score/student; completion rate với mẫu số xác định; thống kê admin. Mỗi query ghi mục đích, quyền, parameter, bảng, expected result và EXPLAIN nếu nói về tối ưu. Hoàn thành với 0 enrollment/0 quiz phải có fixture.
