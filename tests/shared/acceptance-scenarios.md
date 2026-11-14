# Tình huống kiểm thử dùng chung

**Chưa có automated test.** Đây là bộ tiêu chí dùng tay trước và tự động hóa sau cho hai backend. Fixture: admin, teacher A/B, student A/B; free Published/Draft/Archived course, quiz Draft/Published/Closed. Mỗi lần reset chỉ DB test. Đối chiếu business outcome, HTTP code/field; không so token/timestamp tuyệt đối.

| ID | Requirement | Given/When | Expected |
|---|---|---|---|
| T01 | AUTH-01 | Đăng ký hợp lệ | 201, STUDENT, không có hash/token thừa |
| T02 | AUTH-01 | Email chỉ khác hoa/thường; đăng ký hợp lệ có thêm role=ADMIN | Email trùng sau chuẩn hóa trả 409; request hợp lệ có role=ADMIN trả 201, role tự gửi bị bỏ qua và tài khoản được tạo là STUDENT |
| T03 | AUTH-01 | Login sai mật khẩu, gọi private API bằng access hết hạn, refresh không gửi Authorization để rotate và gia hạn, refresh session hết hạn | 401 khi sai mật khẩu/access hết hạn trên private API; login tạo refresh session hạn 7 ngày; refresh đúng trả access_token và Set-Cookie refreshToken mới với Max-Age=604800, expires_at = thời điểm refresh + 7 ngày; refresh token cũ hoặc session hết hạn bị từ chối 401, không gia hạn |
| T04 | AUTH-01 | Logout/refresh không gửi Authorization; gọi private API bằng access token cũ; logout lần hai hoặc thiếu cookie; logout/refresh gửi kèm Bearer token hợp lệ, còn hạn của user bị khóa | Không gửi Authorization: logout thu hồi refresh session nếu tìm thấy, xóa cookie và trả 204, kể cả khi thiếu cookie hoặc retry; refresh bằng token đã thu hồi trả 401; access token cũ còn dùng được trước exp nếu user active và đủ quyền, hết hạn trả 401. Gửi kèm Bearer token của user bị khóa: trả 403 ACCOUNT_INACTIVE trước khi xử lý endpoint; logout không thu hồi session hoặc xóa cookie |
| T05 | AUTH-02 | Admin khóa user đang login | Private/refresh bị chặn, không giữ quyền cũ |
| T06 | USER-01 | Student gửi yêu cầu và gửi trùng/đồng thời qua API; xem lịch sử; admin lọc/approve/reject; xét duyệt lại/đồng thời; tự xét duyệt; lỗi khi lưu quyết định | Tạo 201 PENDING, service bảo đảm chỉ một PENDING/user qua API và trùng trả 409 INVALID_STATE; lịch sử chỉ có yêu cầu của mình; admin lọc status, phân trang và sort whitelist; APPROVED cập nhật role TEACHER cùng quyết định trong transaction, REJECTED giữ role và cho phép gửi mới; chỉ một quyết định thành công, quyết định lại trả 409 INVALID_STATE; student hoặc admin tự xét duyệt trả 403; không tồn tại trả 404; payload sai trả 400; lỗi lưu rollback cả role và quyết định |
| T07 | CAT-01 | CRUD category; xóa category có course | Admin đúng; delete liên quan 409 |
| T08 | COURSE-02 | Guest list/detail Draft và Published | Chỉ Published; Draft 404; không lộ video/đáp án |
| T09 | COURSE-01 | Teacher B sửa course A bằng course/lesson/question ID | 403/404 theo visibility, DB không đổi |
| T10 | COURSE-01 | Publish thiếu lesson/ảnh; price>0 | Validation/conflict; không publish |
| T11 | ENROLL-01 | Enroll free, retry và request đồng thời | 201/200; một membership duy nhất |
| T12 | ENROLL-01 | Enroll Draft/Archived hoặc chưa login | 404/409 theo route state; 401 khi chưa login |
| T13 | LESSON-01 | Unenrolled student đọc video/document | 403/404, không trả URL storage/transcript |
| T14 | LESSON-01 | URL YouTube/iframe không hợp lệ | 400, không lưu HTML iframe |
| T15 | LESSON-01 | PDF/TXT hợp lệ; file quá lớn/sai loại/PDF scan | Upload có metadata; 413/415/422 tương ứng |
| T16 | QUIZ-01 | Publish 0 câu, 1 option, 0 hoặc 2 đáp án đúng | Không publish, lỗi chỉ ra nguyên nhân |
| T17 | QUIZ-01 | Student GET quiz trước submit | Không có is_correct/explanation/correct_option_id |
| T18 | QUIZ-01 | Sửa/xóa Published quiz/question/option | 409; lịch sử/nội dung không đổi |
| T19 | ATTEMPT-01 | Start chưa enroll hoặc quiz Draft/Closed | Không tạo attempt |
| T20 | ATTEMPT-01 | Start hai lần đồng thời | Một IN_PROGRESS, trả cùng attempt |
| T21 | ATTEMPT-01 | Submit 2 đúng trong 3 câu | 66.67, correct_count=2, total_questions=3 |
| T22 | ATTEMPT-01 | Bỏ trống, question ngoài quiz, option sai question, question trùng | Bỏ trống sai; input còn lại 400 và DB không đổi |
| T23 | ATTEMPT-01 | Submit retry khác answers và submit đồng thời | Cùng Result đã lưu; không thêm/ghi lại answers |
| T24 | ATTEMPT-01 | Lỗi DB giữa ghi answer và score | Rollback toàn bộ; retry được, không có SUBMITTED nửa chừng |
| T25 | ATTEMPT-01 | Student B GET attempt A | 403/404; student A review sau submit có đáp án |
| T26 | ATTEMPT-01 | Quiz đóng sau start | Start mới chặn, attempt cũ submit được |
| T27 | AI-01 | Source hợp lệ/mock | Candidate đúng count; generation trace; chưa có nội dung Published |
| T28 | AI-01 | JSON lỗi, nhiều đáp án đúng, timeout/provider lỗi | 502/504 theo code; không lưu quiz/card lỗi |
| T29 | AI-01 | Generation của lesson khác/teacher khác/type khác khi save | Từ chối; không giả generated_by_ai |
| T30 | CARD-01 | Draft card, publish, enrolled/unenrolled view | Chỉ Published và đúng membership; không auto publish |
| T31 | STAT-01 | Fixture có Submitted và IN_PROGRESS | Chỉ Submitted tính điểm; wrong_count gồm bỏ trống |
| T32 | STAT-01 | 0 enrollment hoặc 0 quiz Published | completion_rate=0, không chia 0 |
| T33 | COURSE/ENROLL | Archive khóa đã có enrollment | Enroll mới chặn; member cũ vẫn học; không xóa history |
| T34 | UI-01 | Teacher → student → result → statistics | Luồng hoàn chỉnh, loading/error/empty hợp lý |
| T35 | Portability | Chạy T01–T34 Java/Python trên fixture tương đương | Cùng contract/business outcome |
| T36 | AUTH-01 | User active đổi mật khẩu; sai mật khẩu hiện tại, mật khẩu mới trùng/có độ dài sai; guest gọi API | Thành công 204, mật khẩu mới được băm, mật khẩu cũ không đăng nhập được, tất cả refresh session bị thu hồi, cookie bị xóa; lỗi payload/nghiệp vụ trả 400 theo field và không đổi dữ liệu/phiên; guest trả 401 |
| T37 | AUTH-01 | User active có nhiều refresh session gọi logout-all; thử refresh các phiên cũ và đăng nhập mới | 204, thu hồi tất cả refresh session của đúng user và xóa cookie; refresh phiên cũ trả 401, user khác không bị ảnh hưởng; access token cũ còn hiệu lực đến exp nếu user active; đăng nhập mới thành công |
| T38 | AUTH-02 | Admin tìm email một phần, khác hoa/thường, có khoảng trắng hoặc ký tự %, _ và !; bỏ điều kiện, tìm không có kết quả, chuỗi quá dài; student gọi API | Tìm theo chuỗi literal sau trim, không phân biệt hoa/thường; giữ phân trang/sắp xếp; bỏ điều kiện trả danh sách không lọc, không khớp trả items rỗng; vượt 254 ký tự trả 400 theo field email; student trả 403 |

Mỗi lần chạy ghi backend, version, fixture, pass/fail và evidence trong báo cáo riêng. Không đổi expected chỉ để khớp implementation; đổi nghiệp vụ phải lead review. Race/rollback cần integration test MySQL thực khi code có transaction; không thể xác nhận bằng tài liệu hiện tại.
