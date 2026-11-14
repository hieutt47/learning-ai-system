# Quy tắc nghiệp vụ chung

Baseline đề xuất áp dụng cùng Java/Python.

## Tài khoản và quyền

Đăng ký luôn tạo STUDENT; field role do client gửi bị bỏ qua, không ảnh hưởng quyền tài khoản. Email trim/lowercase, UNIQUE. Admin tạo bằng quy trình riêng/seed development, không đăng ký public.

STUDENT active gửi Teacher request với reason; user_id lấy từ người đang đăng nhập, trạng thái ban đầu PENDING và mỗi user chỉ có một PENDING qua API. Gửi trùng PENDING trả 409 INVALID_STATE. User active xem lịch sử yêu cầu của mình, ADMIN xem toàn bộ và có thể lọc status. Chỉ ADMIN xét duyệt, không tự xét duyệt yêu cầu của mình; chỉ PENDING được chuyển thành APPROVED hoặc REJECTED, yêu cầu đã quyết định trả 409 INVALID_STATE. APPROVED kiểm tra user vẫn là STUDENT active rồi cập nhật role TEACHER và quyết định trong cùng transaction; REJECTED giữ nguyên role. Quyết định lưu admin, ghi chú nullable và thời điểm UTC. Sau REJECTED, STUDENT có thể gửi yêu cầu mới. Service khóa dòng user trước khi kiểm tra PENDING và tạo yêu cầu trong cùng transaction để chặn gửi trùng đồng thời qua API.

Đăng ký tạo STUDENT active nhưng email_verified_at=null; lưu hash token, gửi link xác minh qua SMTP. Chưa xác minh không được login/refresh. Token ngẫu nhiên 32 byte, mã hóa Base64 URL-safe, chỉ lưu SHA-256 trong email_verification_tokens, hạn 24 giờ, dùng một lần. Xác minh ghi email_verified_at và used_at cùng transaction, không thay is_active hoặc tạo phiên đăng nhập. Gửi lại trả 204 cho email không tồn tại/đã xác minh/bị khóa; link cũ còn hiệu lực đến hết hạn hoặc khi dùng. Lỗi SMTP rollback thao tác và trả 503 EMAIL_SEND_FAILED. V4 đánh dấu user tồn tại trước migration là đã xác minh; seed demo khai báo email_verified_at.

Private request kiểm tra chữ ký/hạn access JWT và user active. Access token 15 phút; login tạo refresh session hết hạn sau 7 ngày, chỉ lưu hash refresh. Refresh/logout dùng cookie refreshToken, không yêu cầu access token; frontend không gửi header Authorization khi gọi hai endpoint này. Refresh kiểm tra session chưa revoked, chưa hết hạn và user active; thành công thì rotate token, cập nhật expires_at = thời điểm refresh + 7 ngày và đặt lại cookie Max-Age=604800 giây. Phiên hết hạn không được gia hạn; chưa áp dụng hạn tuyệt đối tính từ lần đăng nhập đầu tiên. Logout không gửi Authorization thu hồi refresh session nếu tìm thấy, xóa cookie refreshToken và lặp lại trả 204; frontend xóa access token đang giữ. Nếu client gửi kèm Bearer token hợp lệ, còn hạn của tài khoản bị khóa, refresh/logout bị chặn với 403 ACCOUNT_INACTIVE trước khi xử lý; logout không thu hồi session hoặc xóa cookie trong trường hợp này. Access token đã phát hành vẫn hợp lệ đến exp nếu user còn active; private request không bắt buộc tra session hoặc có session ID trong JWT. Khóa user chặn private request/refresh và thu hồi tất cả refresh session. Owner/admin sửa khóa; enrolled student học; chỉ chủ attempt xem kết quả của mình, owner/admin xem theo quyền.

User active đổi mật khẩu bằng current_password và new_password; current_password phải khớp hash hiện tại, new_password dài 10–72 ký tự và khác mật khẩu hiện tại. Sai mật khẩu hiện tại hoặc mật khẩu mới trùng trả 400 VALIDATION_ERROR theo field. Lưu hash mới trong transaction, giữ nguyên access token, refresh session và cookie; frontend chỉ xóa các ô mật khẩu, báo thành công và giữ trang Hồ sơ. Logout-all xác định user từ access token và thu hồi tất cả refresh session của user đó. Cả hai thao tác trả 204; chỉ logout-all xóa cookie và yêu cầu frontend xóa phiên/cache, chuyển về đăng nhập. Access token đã phát hành vẫn có hiệu lực đến exp nếu user active.

Admin tìm danh sách user theo một phần email, không phân biệt hoa/thường; trim trước khi tìm, tối đa 254 ký tự. Email thiếu/rỗng/chỉ khoảng trắng không lọc danh sách. Các ký tự %, _ và ! được tìm như ký tự thường; kết quả giữ phân trang và sort whitelist hiện tại.

Quên mật khẩu không yêu cầu đăng nhập, chuẩn hóa email bằng trim/lowercase và trả 204 không body cho email hợp lệ về định dạng, kể cả khi không tồn tại/bị khóa/chưa xác minh. Chỉ user active đã xác minh được gửi link /reset-password?token=... qua SMTP. Token ngẫu nhiên 32 byte, Base64 URL-safe, chỉ lưu SHA-256 trong password_reset_tokens, hạn 30 phút và dùng một lần. Gửi link mới vô hiệu hóa các link đặt lại mật khẩu cũ, chưa thay mật khẩu hoặc phiên đăng nhập; SMTP lỗi trả 503 EMAIL_SEND_FAILED và rollback thao tác, giữ hiệu lực link cũ. Hai thao tác gửi link/đặt lại mật khẩu khóa dòng user trước khi xử lý token để tuần tự hóa yêu cầu cho cùng tài khoản.

Đặt lại mật khẩu xác định user từ token, yêu cầu user còn active và đã xác minh email; mật khẩu mới dài 10–72 ký tự, khác mật khẩu hiện tại và không trim/normalize. Token sai/hết hạn/đã dùng trả 400 INVALID_PASSWORD_RESET_TOKEN; payload hoặc mật khẩu mới không hợp lệ trả 400 VALIDATION_ERROR theo field. Thành công lưu hash BCrypt mới, vô hiệu hóa mọi token đặt lại mật khẩu của user và thu hồi tất cả refresh session trong cùng transaction; trả 204 và xóa cookie refreshToken, không thay role/is_active/email_verified_at hoặc tự đăng nhập. Access token cũ còn hiệu lực đến exp nếu user active. Luồng đổi mật khẩu khi đang đăng nhập vẫn giữ nguyên các phiên như quy tắc hiện tại.

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
