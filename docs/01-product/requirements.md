# Phạm vi và yêu cầu

Nguồn: README gốc. ID nối yêu cầu → API → dữ liệu → tình huống kiểm thử → issue.

| ID | MVP | Điều kiện nghiệm thu |
|---|---|---|
| AUTH-01 | Đăng ký, đăng nhập, đăng xuất | Đăng ký chỉ STUDENT; mật khẩu băm; logout vô hiệu phiên |
| AUTH-02 | Role và khóa tài khoản | Kiểm tra ADMIN/TEACHER/STUDENT và trạng thái active |
| USER-01 | Phê duyệt Teacher | Admin quyết định; user không tự cấp quyền |
| CAT-01 | Danh mục | Admin CRUD; không xóa danh mục đang dùng |
| COURSE-01 | Quản lý khóa, ảnh bìa, giá, publication | Owner/admin; MVP chỉ publish price=0 |
| COURSE-02 | Marketplace | Guest thấy khóa đã publish; không lộ nội dung học riêng |
| ENROLL-01 | Ghi danh, khóa đã tham gia | Free + published; membership không trùng |
| LESSON-01 | YouTube, transcript, PDF/TXT, thứ tự bài | Owner quản lý; nội dung dành cho người ghi danh |
| QUIZ-01 | Quiz/question/option, review và publish | ≥2 options và đúng 1 đáp án đúng; Published bất biến |
| ATTEMPT-01 | Làm bài, chấm, lịch sử | Chấm server; transaction; submit lặp không đổi kết quả |
| AI-01 | Sinh nháp quiz/flashcard | Validate JSON; teacher sửa/duyệt; không auto publish |
| CARD-01 | Quản lý và xem flashcard | Teacher duyệt; student đã ghi danh chỉ xem Published |
| STAT-01 | Thống kê | Điểm TB, lượt nộp, câu sai nhiều, tỷ lệ hoàn thành |
| UI-01 | Giao diện đủ vai trò | Có loading/empty/error/forbidden và luồng demo hoàn chỉnh |

Flashcard thuộc MVP **theo đề xuất**, cần lead chốt vì README vừa gọi bắt buộc vừa gọi mở rộng. Chưa có spaced repetition.

## Ngoài MVP

Thanh toán, streaming/upload video, speech-to-text, tự lấy transcript YouTube, chatbot, mobile, realtime, social login, OCR. Lưu price để mở rộng; UI chưa có nút mua khóa trả phí.

## Vai trò

Guest xem marketplace/giới thiệu. STUDENT ghi danh/học/làm quiz. TEACHER quản lý nội dung và thống kê của mình. ADMIN quản trị và xử lý nội dung. Giữ một role/tài khoản theo README; teacher muốn demo học dùng tài khoản student riêng.

## Chất lượng

API private kiểm tra role và ownership/membership. Không lộ hash/token/API key hoặc đáp án trước submit. Input và lỗi có cấu trúc. AI lỗi không phá dữ liệu đã lưu. Lịch sử điểm không đổi khi sửa nội dung. Java/Python kiểm tra bằng cùng tình huống và contract. Video chỉ lưu URL/ID YouTube.

Hạn nộp, ngân sách AI, công nghệ frontend và phiên bản công cụ chưa có trong README; xem `decisions.md` trước triển khai.
