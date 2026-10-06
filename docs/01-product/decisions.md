# Quyết định và điểm cần chốt

| ID | Nội dung | Baseline | Trạng thái |
|---|---|---|---|
| D01 | Nhóm | 5 người; chưa gán việc | Người dùng xác nhận |
| D02 | Backend/DB | Spring Boot/JPA → FastAPI/SQLAlchemy; PostgreSQL | README |
| D03 | Kiến trúc | Modular monolith, module tương ứng hai ngôn ngữ | Đề xuất |
| D04 | API | `/api/v1`, snake_case, ID string | Đề xuất |
| D05 | Migration | SQL PostgreSQL dùng chung là nguồn chuẩn | Đề xuất |
| D06 | Frontend | Một giao diện chung; framework chưa chọn | Cần chốt |
| D07 | Flashcard | MVP sinh, sửa, duyệt, publish, xem | Cần chốt |
| D08 | Giá | Draft có price; chỉ publish khóa miễn phí | Đề xuất |
| D09 | Auth | Access JWT + refresh session, revoke logout | Đề xuất |
| D10 | Role | Một role/tài khoản, thêm teacher_requests | Đề xuất theo README |
| D11 | Quiz/card đã publish | Bất biến; muốn sửa tạo draft mới | Đề xuất |
| D12 | AI | Đồng bộ nội dung ngắn, timeout, adapter mock | Đề xuất |
| D13 | File | PDF có text/TXT, adapter storage; chưa OCR | Đề xuất |
| D14 | Toolchain | JDK, Spring Boot, build tool, version frontend | Chưa chọn; cần yêu cầu môn học |
| D15 | Hoàn thành | Submit ít nhất một lần tất cả quiz hiện Published | Đề xuất |
| D16 | Xóa | Draft chưa được dùng xóa cứng; có lịch sử thì đóng/lưu trữ | Đề xuất |

README chia việc cho 4 người; khung mới chỉ chuẩn bị quy trình cho 5 người, chưa chia module cho người cụ thể. Khóa học trong mô tả gọi PUBLISHED nhưng schema dùng `is_published`: giữ boolean và thêm `archived_at`. Flashcard thiếu trạng thái duyệt; logout, metadata file, category và yêu cầu Teacher thiếu bảng: thiết kế bổ sung có giải thích.

Các quy tắc bảo vệ đáp án, submit lặp, sửa quiz published và transaction được bổ sung trong `business-rules.md`. Không coi việc tạo bộ khung là lead đã chấp thuận mọi đề xuất.

Khi chốt, cập nhật trạng thái tại đây. Quyết định lớn ghi ADR; đổi API/schema phải sửa tài liệu, hai backend, frontend và test liên quan cùng lúc.
