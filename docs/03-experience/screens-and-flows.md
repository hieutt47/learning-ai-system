# Giao diện và luồng

Frontend dùng API chung, chưa chọn framework. Không render trực tiếp entity/ORM hoặc biết backend đang Java/Python. URL backend cấu hình môi trường.

| Vai trò | Màn hình |
|---|---|
| Guest | Marketplace, lọc category/tìm khóa, course detail/outline, register/login |
| Student | My courses, learning workspace video + documents + quiz/card, quiz attempt, result/review, attempt history, gửi Teacher request |
| Teacher | Dashboard courses, course editor/thumbnail/publication, lesson editor/transcript/documents, quiz editor/options, AI candidate preview, flashcard review/publish, course/quiz statistics |
| Admin | Users/lock, Teacher request approvals, category CRUD, system statistics |

Luồng Student: marketplace → detail → login nếu cần → enroll miễn phí → My courses → lesson → start quiz → submit → score/review → history. Quiz start/submit lỗi mạng cho phép retry theo semantics, không mất kết quả; không hiển thị đáp án trước response submit.

Luồng Teacher: create course Draft → thêm lesson/YouTube/transcript → upload thumbnail → publish course → AI preview → sửa candidate → save draft quiz/questions hoặc card → publish riêng → xem thống kê. Cho phép soạn quiz thủ công nếu AI lỗi.

Luồng Admin: xem Teacher requests → approve/reject có note → kiểm tra role → quản lý users/category → system statistics.

Mỗi màn hình có loading/empty/error; trạng thái 401 dẫn login, 403 thông báo thiếu quyền, 409 hiển thị xung đột, AI timeout cho thử lại thủ công. Button disabled trong mutation đang chạy. Form giữ dữ liệu khi validation lỗi. Video chưa truy cập được hiển thị link dự phòng; không dựa vào thời lượng video để xác nhận hoàn thành.

Không có UI thanh toán hoặc tự động public nội dung AI. Candidate preview chỉ Teacher/Admin, không trộn với student view. Tài liệu học truy cập qua endpoint có quyền.
