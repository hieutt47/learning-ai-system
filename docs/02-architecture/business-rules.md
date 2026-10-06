# Quy tắc nghiệp vụ chung

Baseline đề xuất áp dụng cùng Java/Python.

## Tài khoản và quyền

Đăng ký chỉ tạo STUDENT; từ chối role tự gửi. Email trim/lowercase, UNIQUE. Teacher request được admin approve trong transaction cập nhật role và request. Admin tạo bằng quy trình riêng/seed development, không đăng ký public.

Private request kiểm tra user active và session chưa revoked. JWT chứa session ID; access token đề xuất 15 phút, refresh 7 ngày, rotate refresh token, chỉ lưu hash refresh. Logout revoke phiên và lặp lại trả 204. Khóa user vô hiệu tất cả phiên. Owner/admin sửa khóa; enrolled student học; chỉ chủ attempt xem kết quả của mình, owner/admin xem theo quyền.

Không gửi `is_correct`, `correct_option_id`, explanation trong response student trước submit.

## Khóa học và file

Marketplace: is_published=true, archived_at=null. Publish cần title, description, category, thumbnail, ít nhất một lesson hợp lệ, price=0. Draft được lưu price>0; MVP không publish/enroll khóa trả phí. Unpublish/archive dừng enroll mới; student đã ghi danh vẫn xem bài và quiz Published. Outline công khai không chứa video/document/transcript private.

Enroll kiểm tra và thêm membership trong transaction; lặp trả membership cũ. Xóa khóa có membership bị từ chối, dùng archive. Không cascade xóa kết quả. Lesson/quiz có lịch sử không xóa cứng.

YouTube chỉ nhận URL/ID hợp lệ, không nhận HTML iframe. PDF/TXT tối đa đề xuất 10 MiB; ảnh JPEG/PNG/WebP 5 MiB; validate loại thực, tên server tạo. PDF scan không OCR trả lỗi text rỗng. File được truy cập qua API kiểm tra quyền, không public storage path. Giới hạn AI text đề xuất 20.000 ký tự.

## Quiz, attempt và điểm

Quiz `DRAFT → PUBLISHED → CLOSED`. Publish cần ≥1 câu, ≥2 options/câu và đúng 1 đáp án đúng. Published/CLOSED bất biến cả title/content/options/explanation/order. Sửa bằng draft mới. Chưa mở lại CLOSED.

Start cần STUDENT đã ghi danh, quiz Published. Tối đa một attempt IN_PROGRESS/student/quiz; start lặp trả attempt đó. Có thể làm lại sau submit. Quiz đóng không start mới, nhưng attempt đã bắt đầu vẫn submit.

Submit nhận danh sách question_id/selected_option_id. Bỏ trống chấm sai. Từ chối câu ngoài quiz, option ngoài câu, câu trùng. Chấm server: `round(correct_count * 100 / total_questions, 2)`, thang 0–100. Không tin điểm client.

`IN_PROGRESS → SUBMITTED` một lần; lưu answers (bỏ trống selected_option_id=null), total_questions, correct_count, score và submitted_at cùng transaction. Retry trả kết quả đã lưu, không ghi lại answers. Submit đồng thời phải trả cùng kết quả. Review chỉ sau submit. Nội dung published bất biến bảo vệ lịch sử.

## AI và flashcard

AI chỉ chạy từ lesson của owner/admin có text hợp lệ. Generate trả preview candidate, chưa tạo quiz/card. Validate số lượng, JSON, kiểu, options/đáp án; dữ liệu AI không phải lệnh thực thi. Teacher sửa preview rồi lưu Draft qua CRUD, kèm generation_id hợp lệ thuộc nguồn; Publish riêng.

Flashcard `DRAFT → PUBLISHED → CLOSED`; front/back không rỗng, Published bất biến. Student enrolled chỉ thấy Published. Chưa có ôn tập theo lịch.

## Thống kê

Chỉ tính Submitted. Điểm trung bình trên mọi lượt nộp; best score/student là chỉ số riêng. Câu sai gồm bỏ trống. Hoàn thành quiz = đã submit ≥1 lần. Hoàn thành khóa = submit mọi quiz hiện Published. `completion_rate = completed_students/enrolled_students*100`; mẫu số 0 hoặc không có quiz Published trả 0. Tập quiz hiện tại có thể đổi tỷ lệ, response kèm quiz_count và generated_at.
