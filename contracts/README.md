# Hợp đồng độc lập ngôn ngữ

- `api-conventions.md`: quy ước chung và lỗi.
- `api-catalog.md`: endpoint, quyền, nhóm payload.
- `payloads.md`: request/response, visibility và ví dụ.
- `ai-output.md`: candidate AI, validation, provenance.

**Trạng thái: thiết kế bằng Markdown, chưa có OpenAPI/JSON Schema máy đọc và chưa phải API chạy được.** Trước khi làm controller/frontend, chuyển các hợp đồng đã chốt thành `contracts/openapi.yaml`, khai báo từng field, required, enum, example, response và security; validate spec. Không xuất hai spec từ framework rồi để chúng lệch nhau.

Thay đổi contract cần lead và reviewer phía sử dụng API xem xét. Thêm endpoint/field nullable trước; xóa hoặc đổi nghĩa field cần đánh giá tương thích và version. Scenario nghiệp vụ ở `tests/shared` là tài sản dùng chung cùng contract.
