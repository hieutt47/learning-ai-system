# Learning AI System — Khung dự án MOOC

Khung thiết kế cho nhóm **5 người**, làm bài tập lớn Java trước, tái sử dụng cho Python và CSDL sau. Tạo ngày 06/10/2026 từ README đề tài trong thư mục cha.

**Trạng thái:** chỉ có tài liệu, quy ước và thư mục dự kiến. Chưa có ứng dụng chạy được, mã Java/Python/frontend, SQL, cấu hình build, dependency hay CI. Các quyết định bổ sung là đề xuất để lead duyệt; chưa gán việc cho thành viên.

## Đọc theo thứ tự

1. [Yêu cầu](docs/01-product/requirements.md) và [điểm cần chốt](docs/01-product/decisions.md).
2. [Kiến trúc](docs/02-architecture/architecture.md) và [quy tắc nghiệp vụ](docs/02-architecture/business-rules.md).
3. [API chung](contracts/README.md), [danh mục endpoint](contracts/api-catalog.md), [payload](contracts/payloads.md).
4. [ERD](database/erd.md) và [từ điển dữ liệu](database/data-dictionary.md).
5. [Quy trình nhóm](docs/04-team/team-workflow.md), [lộ trình](docs/05-delivery/roadmap.md) và [kiểm thử](tests/shared/acceptance-scenarios.md).

```text
learning-ai-system/
  docs/                 Yêu cầu, kiến trúc, giao diện, làm việc nhóm, bàn giao
  contracts/            API, schema AI và ví dụ JSON dùng chung
  database/             ERD, từ điển dữ liệu, kế hoạch migration/seed/query
  java-backend/         Khung Spring Boot, triển khai trước
  python-backend/       Khung FastAPI, triển khai ở giai đoạn Python
  frontend/             Giao diện chung, công nghệ chưa chốt
  tests/shared/         Tình huống và kế hoạch dữ liệu kiểm thử chung
  infra/                Môi trường, cấu hình và vận hành
  .github/              Mẫu issue, pull request
```

README trong mỗi vùng giải thích vị trí đặt mã sau này. Thư mục trống được giữ bằng `.gitkeep`. Đã khởi tạo Git local nhánh `main` và nối repository GitHub công khai [hieutt47/learning-ai-system](https://github.com/hieutt47/learning-ai-system). Chưa push nội dung; xem [hướng dẫn GitHub](docs/04-team/github-setup.md).

Tái sử dụng **thiết kế, API, dữ liệu, frontend và tình huống kiểm thử**; backend Python được triển khai riêng, không dịch từng dòng Java. Mỗi môi trường chạy một backend. Tài liệu gốc giữ nguyên ở thư mục cha và được sao chép vào [bản tham chiếu](docs/reference/ORIGINAL_README.md).
