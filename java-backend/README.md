# Backend Java — triển khai trước

Stack theo đề tài: Spring Boot, Spring Security/JWT, Hibernate/JPA và PostgreSQL. **Chưa chọn JDK/framework version/build tool; chưa có pom.xml, Gradle, main class, dependency hoặc ứng dụng chạy được.** Chốt theo quy định môn trước bootstrap.

Package baseline `vn.edu.learningai`; đổi namespace một lần trước viết code nếu nhóm có quy định. Mã tương lai đặt trong `src/main/java/vn/edu/learningai/modules/<module>/`:

- `api/`: Controller, request/response DTO, mapping và validation biên.
- `application/`: Services/use cases, transaction boundary và port interfaces cần thiết.
- `domain/`: Business rules và domain data không phụ thuộc Spring/JPA.
- `infrastructure/`: JPA entity/repository adapter, mapper, integration theo module.

Không cần domain entity và ORM entity trùng nhau mọi field nếu chỉ làm mô hình đơn giản; domain giữ quy tắc có giá trị, persistence giữ mapping DB. Services không trả ORM entity ra API.

`shared/`: config, security, errors và tiện ích dùng chung tối thiểu; `src/main/resources/` dành cấu hình không chứa secret. Migration chỉ trong `database/migrations/`, không tự sao một bộ SQL khác vào backend. `src/test/java/vn/edu/learningai/` cho kiểm tra code; scenarios trong tests/shared dùng chung.

Sau chốt toolchain: tạo build/main/config → validate DB schema → error conventions → security/session → module theo backlog. Framework annotation đặt ở adapter/application khi cần, không kéo vào domain. Lệnh build/run được bổ sung sau khi có project thật, chưa ghi lệnh giả chạy được.
