# Từ điển dữ liệu — baseline đề xuất

PK BIGINT identity; wire ID string. TIMESTAMPTZ UTC; price NUMERIC(14,2); score NUMERIC(5,2). Enum lưu VARCHAR + CHECK để hai ORM ánh xạ cùng nghĩa. Mặc định field bắt buộc trừ đánh dấu `?` nullable. Text/candidate giới hạn tại validation. FK RESTRICT mặc định, không cascade xóa lịch sử.

| Bảng | Field |
|---|---|
| users | id PK, full_name VARCHAR(120), email VARCHAR(254) UNIQUE normalized, password_hash VARCHAR(255), role VARCHAR CHECK ADMIN/TEACHER/STUDENT, is_active BOOLEAN, created_at, updated_at |
| auth_sessions | id PK, user_id FK users, refresh_token_hash VARCHAR(255) UNIQUE, expires_at, revoked_at?, created_at, updated_at |
| teacher_requests | id PK, user_id FK users, reason TEXT, status CHECK PENDING/APPROVED/REJECTED, decided_by? FK users, decision_note?, decided_at?, created_at |
| categories | id PK, name VARCHAR(120) UNIQUE, description? TEXT, created_at, updated_at |
| courses | id PK, teacher_id FK users, category_id FK categories, title VARCHAR(200), description TEXT, thumbnail_url?, thumbnail_storage_key?, price NUMERIC CHECK ≥0 DEFAULT 0, is_published BOOLEAN DEFAULT false, archived_at?, created_at, updated_at |
| course_members | course_id FK courses + student_id FK users = composite PK, enrolled_at |
| lessons | id PK, course_id FK courses, title VARCHAR(200), video_url VARCHAR(2048), transcript_text? TEXT, order_index INT CHECK ≥0, created_at, updated_at |
| lesson_documents | id PK, lesson_id FK lessons, uploaded_by FK users, original_name VARCHAR(255), storage_key VARCHAR(512) UNIQUE, media_type VARCHAR(100), size_bytes BIGINT CHECK >0, extracted_text? TEXT, content_hash VARCHAR(64), created_at |
| ai_generations | id PK, lesson_id FK lessons, source_document_id? FK lesson_documents SET NULL, source_type CHECK TRANSCRIPT/DOCUMENT, generation_type CHECK QUIZ/FLASHCARD, requested_count INT CHECK 1..10, difficulty? CHECK EASY/MEDIUM/HARD, provider VARCHAR(100), model VARCHAR(100), prompt_version VARCHAR(50), source_hash VARCHAR(64), status CHECK SUCCEEDED/FAILED, candidate_json? JSONB, error_code?, created_by FK users, created_at |
| quizzes | id PK, lesson_id FK lessons, title VARCHAR(200), status CHECK DRAFT/PUBLISHED/CLOSED DEFAULT DRAFT, generation_id? FK ai_generations, created_by FK users, published_at?, closed_at?, created_at, updated_at |
| questions | id PK, quiz_id FK quizzes, content TEXT, question_type CHECK MULTIPLE_CHOICE, explanation? TEXT, order_index INT CHECK ≥0, generated_by_ai BOOLEAN DEFAULT false, generation_id? FK ai_generations, created_at |
| question_options | id PK, question_id FK questions, content TEXT, is_correct BOOLEAN, order_index INT CHECK ≥0 |
| quiz_attempts | id PK, quiz_id FK quizzes, student_id FK users, status CHECK IN_PROGRESS/SUBMITTED, score? NUMERIC CHECK 0..100, total_questions INT CHECK >0, correct_count? INT CHECK 0..total_questions, started_at, submitted_at?, version BIGINT DEFAULT 0 |
| student_answers | id PK, attempt_id FK quiz_attempts, question_id FK questions, selected_option_id? FK question_options, is_correct BOOLEAN |
| flashcards | id PK, lesson_id FK lessons, front_text TEXT, back_text TEXT, status CHECK DRAFT/PUBLISHED/CLOSED DEFAULT DRAFT, generated_by_ai BOOLEAN DEFAULT false, generation_id? FK ai_generations, created_by FK users, published_at?, closed_at?, created_at, updated_at |

## Ràng buộc và chỉ mục

- Email lưu normalized lowercase; UNIQUE thực tế dùng cùng quy tắc register/login/import.
- Một Teacher request PENDING/user: partial unique index; decision field phải phù hợp status, không tự approve mình.
- auth_sessions index user_id; private request kiểm tra revoked_at/expiry và user active.
- lessons UNIQUE(course_id, order_index); questions UNIQUE(quiz_id, order_index); options UNIQUE(question_id, order_index). Reorder cần transaction và chiến lược tránh trùng tạm thời, chốt khi viết migration.
- Partial unique option đúng theo question_id: ngăn >1 đáp án đúng; ≥2 option và đúng 1 khi publish kiểm tra ở service/transaction. DB CHECK đơn hàng không bảo đảm số option trên các row; nếu cần trigger cho môn CSDL phải viết và kiểm thử riêng.
- Partial unique attempt IN_PROGRESS theo (quiz_id, student_id); UNIQUE(attempt_id, question_id) cho answers.
- SUBMITTED bắt buộc submitted_at/score/correct_count; IN_PROGRESS các field này null. Bỏ trống answer option=null và is_correct=false.
- Option phải thuộc question; question phải thuộc quiz của attempt. Service kiểm tra cùng transaction; khi viết SQL cần composite FK/trigger tương ứng để chống dữ liệu sai ngoài service. FK đơn không đủ bảo đảm quan hệ này.
- Published quiz/card bất biến được application bảo vệ; DB trigger là quyết định riêng, không tuyên bố đã có trong khung.
- courses index (is_published, created_at), teacher_id/category_id; lessons course_id; quizzes lesson_id/status; course_members student_id; attempts (quiz_id, submitted_at) và (student_id, started_at); answers question_id/is_correct. Kiểm tra bằng EXPLAIN trước thêm index theo cảm tính.
- Giá >0 chỉ hợp lệ ở draft trong MVP; archived phải is_published=false. Unpublish không xóa membership.
- Generation source_document_id chỉ tham chiếu tài liệu cùng lesson; generation_id phải đúng type/lesson/creator và SUCCEEDED. Hash/provenance không mất khi tài liệu bị xóa.

## Bổ sung so với README

categories phục vụ quản trị danh mục; auth_sessions cho logout có hiệu lực; teacher_requests cho approval; lesson_documents cho upload/extraction; ai_generations truy vết nháp; flashcard.status cho review. Thêm archived_at để giữ lịch sử; attempt.status/version/count để chống nộp trùng và ghi điểm nhất quán. Không thêm bảng thanh toán, progress video, chatbot hay thông báo.
