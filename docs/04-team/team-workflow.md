# Quy trình nhóm 5 người

Lead + 4 thành viên; chưa gán tên, vai trò chuyên môn hay module. Lead giữ quyết định API/schema và tích hợp nhưng vẫn tham gia implementation/review khi chia việc. Module là ranh giới sản phẩm, không đồng nghĩa một module bắt buộc một người.

## Chuẩn bị trước phân công

Mọi người đọc requirements, architecture, contract và ERD. Chốt toolchain/DoD. Kiểm kê năng lực và thời gian từng người. Mỗi issue có owner, reviewer khác owner, yêu cầu ID, phạm vi, dependency và tiêu chí nghiệm thu. Lead cân đối theo khối lượng, không chỉ theo số module.

| Thành viên | Tên | Năng lực/thời gian | Owner phạm vi | Reviewer dự phòng |
|---|---|---|---|---|
| Lead | Chưa điền | Chưa điền | Chưa chia | Chưa chia |
| Thành viên 2 | Chưa điền | Chưa điền | Chưa chia | Chưa chia |
| Thành viên 3 | Chưa điền | Chưa điền | Chưa chia | Chưa chia |
| Thành viên 4 | Chưa điền | Chưa điền | Chưa chia | Chưa chia |
| Thành viên 5 | Chưa điền | Chưa điền | Chưa chia | Chưa chia |

## Git và tích hợp

Đề xuất `main` luôn demo được; branch ngắn `feat/<issue>-<topic>`, `fix/...`, `docs/...`. Không thêm develop khi chưa có nhu cầu. PR nhỏ theo một use case; ít nhất một review; API/schema ảnh hưởng nhiều module có lead review. Không tự ý push trực tiếp main. Đây là quy ước cần cấu hình branch protection khi nhóm tạo remote; Git local và remote GitHub đã tạo; branch protection cần cấu hình sau khi push main.

Commit ngắn rõ nghĩa: feat/fix/docs/test/chore. Trước PR cập nhật branch, chạy kiểm tra thích hợp, ghi evidence và limitation. Không commit secret, file build, upload cá nhân hoặc dependency binaries. Thông báo đổi field/enum/route/schema bằng issue/PR trước implementation.

Board: Backlog → Ready → In progress → Review → Done; blocked ghi dependency và người cần hỗ trợ, không đổi sang Done. Check-in đề xuất hai lần/tuần và một buổi integration/demo, nhóm chốt lịch sau. Owner không được merge khi test/contract vẫn hỏng; lead không làm reviewer duy nhất cho mọi PR.

## Definition of Ready

Yêu cầu/đầu ra rõ, API/schema liên quan đã chốt, dependency biết, acceptance scenarios có ID, owner và reviewer có khả năng nhận việc.

## Definition of Done

Đúng use case và phân quyền; không lộ dữ liệu; validate lỗi; thay đổi DB có migration/seed phù hợp; contract/docs cập nhật; kiểm tra có ý nghĩa và kết quả ghi trong PR; màn hình có trạng thái cơ bản; reviewer chấp nhận; demo qua luồng thực. Không yêu cầu viết test chỉ để lặp implementation hoặc cho thay đổi tài liệu đơn giản.

## Bảo vệ tiến độ

Tích hợp theo chiều dọc: một luồng course/enroll/lesson/quiz/result đầy đủ trước, rồi AI và thống kê. UI dùng fixture theo contract khi backend chưa xong. Hạng mục mới phải đánh giá MVP và deadline; không âm thầm thêm payment/chatbot. Chia việc cụ thể sẽ thực hiện ở bước sau với lead.
