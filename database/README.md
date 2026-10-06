# Thiết kế CSDL dùng chung

`data-dictionary.md` là baseline field/constraint; `erd.md` là sơ đồ; `migration-policy.md` là quy trình thay đổi. Chưa có SQL schema/migration/seed thực thi.

- `migrations/`: SQL có thứ tự, bất biến sau khi dùng; một nguồn cho Java/Python.
- `seeds/`: dữ liệu demo/dev/test, không dùng mật khẩu thật.
- `queries/`: query thống kê và chứng minh môn CSDL.

Migration đầu tiên chỉ viết sau khi lead chốt thiết kế. Không tạo đồng thời schema.sql, JPA auto-DDL và Alembic độc lập. Database module không thuộc riêng người làm auth; mọi thay đổi được review ảnh hưởng toàn hệ thống.
