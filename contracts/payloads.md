# Payload và visibility

Field dưới đây là thiết kế; khi viết OpenAPI phải khai báo required/nullability/giới hạn cụ thể. Mọi ID string; thời gian UTC; không nhận các field server sở hữu như owner_id, score, generated_by_ai, is_published trong CRUD tùy ý.

| Group | Request | Response chính |
|---|---|---|
| Register | full_name, email, password | User: id, full_name, email, role, is_active, created_at |
| Login | email, password | AuthResponse: access_token; Set-Cookie: refreshToken |
| Refresh | Cookie: refreshToken; không body; không gửi header Authorization | AuthResponse: access_token; Set-Cookie: refreshToken mới |
| Logout | Cookie: refreshToken nếu có; không body; không gửi header Authorization | 204, không body; Set-Cookie xóa refreshToken |
| Profile | full_name bắt buộc, không null/rỗng/chỉ có khoảng trắng, dài 1–120 ký tự; body thiếu full_name trả 400 VALIDATION_ERROR | User (200); chỉ cập nhật full_name, không đổi email/password/role/is_active |
| UserStatus | is_active | User |
| TeacherRequest/Decision | reason bắt buộc, không null/rỗng/chỉ có khoảng trắng, tối đa 4000 ký tự / status bắt buộc APPROVED hoặc REJECTED, decision_note nullable tối đa 4000 ký tự | id, user_id, reason, status, decided_by nullable, decision_note nullable, decided_at nullable, created_at; các ID là string, timestamp UTC |
| CategoryWrite | POST: name bắt buộc, description nullable; PATCH: chỉ cập nhật name/description khác null, field thiếu hoặc null giữ nguyên; description rỗng được lưu là chuỗi rỗng | id, name, description |
| CourseWrite | title, description, category_id, price | CourseOwner: id, fields trên, thumbnail_url nullable, is_published, archived_at nullable, teacher_id, created_at, updated_at |
| Publication/Archive | is_published / archived=true | CourseOwner |
| CoursePublic | Không ghi | id, title, description, category, teacher {id,full_name}, thumbnail_url, price, currency, total_students, outline [{id,title,order_index}] |
| Enrollment/Member | Không ghi | course_id, student_id, enrolled_at; Member thêm full_name, không lộ email public |
| CourseEnrollment | Không ghi | CoursePublic + enrolled_at; khóa unpublish/archive vẫn xuất hiện cho enrolled user |
| LessonWrite | title, video_url, transcript_text nullable, order_index | Lesson: id, course_id, title, normalized video_url, order_index, created_at; transcript_text chỉ owner |
| Document | multipart file | id, lesson_id, original_name, media_type, size_bytes, created_at; không storage_key/extracted_text |
| QuizWrite | title, generation_id nullable | QuizAuthor: id, lesson_id, title, status, created_at, questions[] |
| QuestionWrite | content, explanation nullable, order_index, options[{content,is_correct,order_index}], generation_id nullable | QuestionAuthor: id, content, explanation, options[{id,content,is_correct,order_index}], generated_by_ai |
| QuizStudent | Không ghi | id, lesson_id, title, status, questions[{id,content,order_index,options[{id,content,order_index}]}] |
| QuizSummary | Không ghi | id, lesson_id, title, status, question_count |
| Status | status=PUBLISHED hoặc CLOSED | QuizAuthor/Card; transition được kiểm tra |
| CardWrite | front_text, back_text, generation_id nullable | Card: id, lesson_id, front_text, back_text, status, generated_by_ai, created_at |
| GenerationRequest | lesson_id, source_type=TRANSCRIPT hoặc DOCUMENT, document_id nếu DOCUMENT, count, difficulty cho QUIZ | Candidate + generation_id; xem ai-output.md |
| Attempt | Không ghi trực tiếp | id, quiz_id, student_id, status, started_at; IN_PROGRESS không có review/đáp án |
| Submit | answers[{question_id,selected_option_id nullable}] | Result |
| Result | Không ghi | Attempt + score, total_questions, correct_count, submitted_at, review[{question_id,selected_option_id,is_correct,correct_option_id,explanation}] |
| AttemptSummary | Không ghi | id, quiz_id, status, score nullable, started_at, submitted_at nullable |
| CourseStatistics | Không ghi | course_id, enrolled_students, quiz_count, submitted_attempts, completed_students, completion_rate, generated_at |
| QuizStatistics | Không ghi | quiz_id, submitted_attempts, unique_students, average_score nullable, best_scores[{student_id,score}], hardest_questions[{question_id,wrong_count}], generated_at |
| SystemStatistics | Không ghi | active_users, teachers, published_courses, enrollments, submitted_attempts, generated_at |

Email register/login được chuẩn hóa trong service bằng `trim().toLowerCase(Locale.ROOT)` trước khi lưu hoặc xác thực; ví dụ `"User@Example.com"` trở thành `"user@example.com"`. DTO được validation trước khi gọi service: null/rỗng bị `@NotBlank` từ chối; register có thêm `@Email`, nên email có khoảng trắng đầu/cuối có thể bị trả 400 trước khi service kịp trim. DTO không tự thay đổi email; password luôn được giữ nguyên.

TeacherRequest mới có status=PENDING, decided_by/decision_note/decided_at=null; user_id lấy từ người đang đăng nhập. Hai danh sách TeacherRequest nhận page/page_size/sort; sort whitelist id, status, created_at, decided_at, mặc định created_at desc rồi id desc. Thiếu hoặc sai reason/status, hoặc decision_note vượt giới hạn trả 400 VALIDATION_ERROR; status=PENDING không hợp lệ khi quyết định. Chỉ yêu cầu PENDING được xét duyệt; APPROVED yêu cầu user vẫn là STUDENT active và cập nhật role thành TEACHER, nếu không trả 409 INVALID_STATE. REJECTED giữ nguyên role; quyết định lưu decided_by/decision_note/decided_at. User có thể gửi yêu cầu mới sau khi bị từ chối nếu vẫn là STUDENT và chưa có PENDING.

Giới hạn đề xuất: full_name 1–120; email ≤254; password đăng ký 10–72 ký tự; title 1–200; question/card mỗi mặt ≤4000; options 2–6, content 1–1000; order_index≥0; price≥0 với 2 chữ số thập phân; count 1–10; difficulty EASY/MEDIUM/HARD. Không trả full_name/email ngoài phạm vi yêu cầu; không bao giờ trả password_hash hoặc refresh_hash.

Login cũng giới hạn password tối đa 72 ký tự, nhưng không áp lại độ dài tối thiểu của đăng ký để tránh khóa tài khoản được tạo theo chính sách cũ. Password không trim, normalize hoặc cắt ngắn; input vượt giới hạn trả 400 VALIDATION_ERROR. Theo lựa chọn hiện tại, dự án giả định mật khẩu ASCII và dùng `@Size` để kiểm tra độ dài; chưa có validation cấm Unicode hoặc kiểm tra số byte UTF-8. Giới hạn 72 ký tự chỉ tương ứng giới hạn 72 byte của BCrypt dưới giả định ASCII này.

QuizStudent không có explanation, generated metadata nhạy cảm hoặc is_correct. Preview AI chỉ teacher/admin được thấy. Result review được hiện sau submit. QuizStatistics không có attempt IN_PROGRESS và phải phân trang/giới hạn mảng nếu quy mô lớn, cần chốt trong OpenAPI.

Ví dụ request submit (một câu bỏ trống):

```json
{"answers":[{"question_id":"101","selected_option_id":"1002"},{"question_id":"102","selected_option_id":null}]}
```

Ví dụ response Result rút gọn:

```json
{"id":"500","quiz_id":"20","student_id":"3","status":"SUBMITTED","score":50.0,"total_questions":2,"correct_count":1,"started_at":"2026-10-06T02:00:00Z","submitted_at":"2026-10-06T02:05:00Z","review":[{"question_id":"101","selected_option_id":"1002","is_correct":true,"correct_option_id":"1002","explanation":"Giải thích mẫu"},{"question_id":"102","selected_option_id":null,"is_correct":false,"correct_option_id":"1010","explanation":"Giải thích mẫu"}]}
```
