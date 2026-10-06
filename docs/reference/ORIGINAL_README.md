# Kế hoạch Xây dựng Hệ thống Học tập Trực tuyến (MOOC) tích hợp AI

## 1. Tổng quan đề tài

### Tên đề tài
**Xây dựng nền tảng học tập trực tuyến (MOOC) tích hợp AI tự động tạo Quiz và Flashcard**

### Ý tưởng chính
Hệ thống hoạt động theo mô hình mở tương tự Coursera. Giảng viên có thể đóng gói kiến thức thành các khóa học (gồm Video bài giảng và Tài liệu đọc), thiết lập ảnh bìa, giá tiền (hoặc miễn phí) và xuất bản lên hệ thống. Học viên có thể lướt danh mục khóa học ở trang chủ, tự ghi danh (enroll) vào các khóa học Free để xem video và làm bài kiểm tra.

Điểm nhấn công nghệ: Hệ thống hỗ trợ Giảng viên tối ưu năng suất bằng AI. Khi Giảng viên cung cấp tài liệu tóm tắt hoặc kịch bản (transcript) của video bài giảng, hệ thống sẽ sử dụng AI (LLM) để tự động sinh ra các bài trắc nghiệm (Quiz) hoặc Flashcard. Giảng viên chỉ cần kiểm duyệt lại trước khi xuất bản.

Đề tài được thiết kế để có thể sử dụng chung cho ba bài tập lớn:
- **Java:** triển khai backend bằng Spring Boot và Hibernate/JPA.
- **Python:** triển khai backend bằng FastAPI và SQLAlchemy, đồng thời xử lý tài liệu và tích hợp AI.
- **Cơ sở dữ liệu:** thiết kế CSDL, ràng buộc, truy vấn, chỉ mục, transaction và thống kê.

## 2. Định hướng sử dụng chung cho các môn

Nhóm giữ nguyên các thành phần sau:
- ý tưởng sản phẩm (Coursera clone);
- yêu cầu nghiệp vụ;
- mô hình dữ liệu;
- quy tắc phân quyền;
- API contract;
- giao diện và luồng sử dụng.

Tùy môn học, nhóm thay đổi công nghệ triển khai backend và ORM:

| Thành phần | Bản Java | Bản Python |
|---|---|---|
| Web framework | Spring Boot | FastAPI |
| Controller | Spring Controller | FastAPI Router |
| Business logic | Service | Service |
| ORM | Hibernate/JPA | SQLAlchemy |
| Repository | JPA Repository | Repository tự xây dựng |
| Entity | Java Entity | SQLAlchemy Model |
| DTO/Validation | DTO + Bean Validation | Pydantic Schema |
| Authentication | Spring Security + JWT | JWT + Auth Dependency |
| CSDL | PostgreSQL | PostgreSQL |
| Xử lý AI | Gọi API cơ bản | Xử lý Text nâng cao, gọi LLM API |

> Mục tiêu là tái sử dụng **thiết kế**, không sao chép từng dòng mã nguồn từ Java sang Python.

## 3. Kiến trúc tổng quát và Xử lý Video

```text
Frontend / User Interface
           │
           ▼
        REST API
           │
           ▼
Controller / Router
           │
           ▼
    Business Service
           │
     ┌─────┴──────────┐
     ▼                ▼
Repository / ORM   AI Integration
     │                │
     ▼                ▼
 PostgreSQL        LLM API
```

**Đặc biệt về xử lý Video:** Để tránh quá tải server và phù hợp với đồ án sinh viên, Video bài giảng sẽ **không** được upload trực tiếp lên server. Giảng viên sẽ upload video lên YouTube (dạng Unlisted/Không công khai) và dán link vào hệ thống. Frontend sẽ dùng `<iframe>` để nhúng video vào trang học tập.

### Khi triển khai bài tập lớn Java
```text
Frontend
   ↓
Spring Boot
   ↓
Service Layer
   ↓
Hibernate / JPA
   ↓
PostgreSQL
```
Trọng tâm trình bày: lập trình hướng đối tượng, kiến trúc nhiều tầng, Spring Boot, Hibernate/JPA, Repository và Service pattern, REST API, xác thực và phân quyền.

### Khi triển khai bài tập lớn Python
```text
Frontend
   ↓
FastAPI
   ↓
Service Layer
   ↓
SQLAlchemy
   ↓
PostgreSQL
```
Trọng tâm trình bày: Python và FastAPI, Pydantic validation, xử lý file văn bản/transcript, gọi API AI, kiểm tra JSON do AI trả về, SQLAlchemy và thao tác CSDL.

## 4. Vai trò người dùng

### Admin
- Quản lý tài khoản người dùng, khóa/mở khóa tài khoản.
- Phê duyệt quyền Giảng viên (Teacher).
- Quản lý danh mục khóa học chung, xem thống kê hệ thống.

### Teacher (Giảng viên)
- Tạo khóa học (thêm ảnh bìa, mô tả, set giá tiền = 0 để làm khóa Free).
- Tạo các Bài học (Lesson), nhúng link Video YouTube và đính kèm tài liệu/transcript (PDF/TXT).
- Dùng AI sinh câu hỏi trắc nghiệm từ nội dung tài liệu đính kèm của bài học.
- Xem, sửa, duyệt nội dung AI sinh ra và xuất bản Quiz.
- Xem kết quả và thống kê học tập của khóa học do mình tạo.

### Student (Học viên)
- Lướt xem danh sách các khóa học đã được xuất bản (PUBLISHED) trên trang chủ (Marketplace).
- **Tự ghi danh (Enroll)** vào các khóa học miễn phí.
- Xem các khóa học đã tham gia.
- Vào không gian học tập: Xem video bài giảng, đọc tài liệu.
- Làm Quiz đã được xuất bản.
- Xem điểm, đáp án sau khi nộp bài và lịch sử các lần làm bài.

## 5. Phạm vi phiên bản đầu tiên (MVP)

### Chức năng bắt buộc
- Đăng ký, đăng nhập và đăng xuất.
- Phân quyền `ADMIN`, `TEACHER`, `STUDENT`.
- Trang chủ (Marketplace) hiển thị danh sách khóa học.
- CRUD khóa học (có ảnh bìa, giá tiền, trạng thái xuất bản).
- Student tự ghi danh vào khóa học Free.
- Teacher tạo và quản lý nội dung Bài học (Lesson) có chứa link YouTube và nội dung transcript.
- CRUD quiz, câu hỏi và phương án trả lời.
- Làm bài, chấm điểm và lưu lịch sử.
- AI sinh câu hỏi trắc nghiệm/flashcard từ kịch bản (transcript) hoặc tài liệu đính kèm của Lesson.
- Giảng viên duyệt nội dung AI trước khi lưu/xuất bản.
- Thống kê kết quả cơ bản.

### Chưa làm trong phiên bản đầu tiên
- Tích hợp cổng thanh toán thật (VNPay/MoMo). Tạm thời set khóa học Free (price = 0).
- Upload và streaming file video trực tiếp trên server (sử dụng YouTube iframe thay thế).
- Tự động bóc băng giọng nói video (Speech-to-Text).
- Chatbot hỏi đáp theo tài liệu.
- Mobile app.
- Hệ thống thông báo thời gian thực.

## 6. Luồng nghiệp vụ chính

### Luồng Học viên tự ghi danh và Học
```text
Học viên lướt Marketplace (Trang chủ)
       ↓
Xem chi tiết khóa học (Giới thiệu, Đề cương)
       ↓
Bấm "Ghi danh miễn phí" (Hệ thống thêm vào course_members)
       ↓
Vào giao diện Học tập
       ↓
Xem Video Bài học (Lesson)
       ↓
Làm Quiz cuối bài -> Nộp bài -> Hệ thống tính điểm
       ↓
Hiển thị kết quả
```

### Luồng AI tạo quiz từ Bài học
```text
Teacher tạo Lesson (nhúng link YouTube)
          ↓
Teacher đính kèm tài liệu / transcript (PDF/TXT) của video đó
          ↓
Hệ thống trích xuất văn bản -> Làm sạch và chia đoạn
          ↓
Gửi nội dung + yêu cầu tới LLM API
          ↓
Nhận danh sách câu hỏi trắc nghiệm dạng JSON
          ↓
Kiểm tra cấu trúc và dữ liệu
          ↓
Teacher duyệt, sửa lỗi (nếu có)
          ↓
Lưu và xuất bản Quiz cho Lesson đó
```

## 7. Phân chia công việc cho nhóm 4 người

| Thành viên | Module chính | Công việc trọng tâm |
|---|---|---|
| Người 1 | User, Authentication và Database Core | ERD, schema, migration, tài khoản, role, đăng nhập, phân quyền. |
| Người 2 | Marketplace, Course và Ghi danh | CRUD khóa học, upload Thumbnail, duyệt khóa học hiển thị trang chủ, API cho Student tự enroll. |
| Người 3 | Lesson, Quiz và Result | CRUD Bài học (Video URL), CRUD Quiz/Câu hỏi, giao diện học tập/làm bài, chấm điểm và lịch sử. |
| Người 4 | AI Integration và Statistics | Xử lý file/transcript, gọi AI sinh JSON, validate kết quả, flashcard, truy vấn thống kê phức tạp. |

### Người 1 — User, Authentication và Database Core
Công việc: Thiết kế ERD, xây dựng đăng ký/đăng nhập, mã hóa mật khẩu, tạo/kiểm tra JWT, phân quyền, chuẩn bị dữ liệu mẫu.
API dự kiến:
```http
POST /api/auth/register
POST /api/auth/login
GET  /api/users/me
GET  /api/users
PUT  /api/users/{userId}
PATCH /api/users/{userId}/status
```

### Người 2 — Marketplace, Course và Ghi danh
Công việc: CRUD khóa học, upload ảnh bìa, API lấy danh sách khóa học cho trang chủ, xử lý luồng sinh viên bấm nút tham gia khóa học (enroll).
API dự kiến:
```http
POST   /api/courses
GET    /api/courses (Marketplace list)
GET    /api/courses/{courseId}
PUT    /api/courses/{courseId}
DELETE /api/courses/{courseId}

POST   /api/courses/{courseId}/enroll
GET    /api/courses/{courseId}/members
```

### Người 3 — Lesson, Quiz, Attempt và Result
Công việc: Quản lý Bài học (nhúng video), CRUD quiz liên kết với bài học, xuất bản quiz, lưu lựa chọn của sinh viên, chấm điểm, lưu lịch sử.
API dự kiến:
```http
POST  /api/courses/{courseId}/lessons
GET   /api/courses/{courseId}/lessons

POST  /api/lessons/{lessonId}/quizzes
GET   /api/lessons/{lessonId}/quizzes
PATCH /api/quizzes/{quizId}/publish

POST   /api/quizzes/{quizId}/questions
PUT    /api/questions/{questionId}

POST /api/quizzes/{quizId}/attempts
POST /api/attempts/{attemptId}/submit
GET  /api/attempts/{attemptId}
```

### Người 4 — AI Integration và Statistics
Công việc: Đọc PDF/TXT transcript, prompt engineering, parse JSON, tạo nháp quiz, thống kê điểm số và tỷ lệ hoàn thành.
API dự kiến:
```http
POST /api/ai/generate-quiz
POST /api/ai/generate-flashcards

GET /api/statistics/courses/{courseId}
GET /api/statistics/quizzes/{quizId}
```
Input tạo quiz:
```json
{
  "lesson_id": 15,
  "number_of_questions": 5,
  "difficulty": "MEDIUM"
}
```

## 8. Thiết kế Cơ sở dữ liệu

### Bảng `users`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã người dùng |
| full_name | VARCHAR | Họ tên |
| email | VARCHAR, UNIQUE | Email đăng nhập |
| password_hash | VARCHAR | Mật khẩu đã băm |
| role | VARCHAR/ENUM | ADMIN, TEACHER, STUDENT |
| is_active | BOOLEAN | Trạng thái tài khoản |
| created_at | TIMESTAMP | Thời điểm tạo |

### Bảng `courses`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã khóa học |
| title | VARCHAR | Tên khóa học |
| description | TEXT | Mô tả chi tiết |
| thumbnail_url | VARCHAR | Link ảnh bìa khóa học |
| price | DECIMAL | Giá tiền (0 = Miễn phí) |
| is_published | BOOLEAN | Trạng thái hiển thị lên Marketplace |
| teacher_id | BIGINT, FK | Giảng viên tạo khóa |
| created_at | TIMESTAMP | Thời điểm tạo |

### Bảng `course_members`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| course_id | BIGINT, PK, FK | Mã khóa học |
| student_id | BIGINT, PK, FK | Mã sinh viên đã ghi danh |
| enrolled_at | TIMESTAMP | Thời điểm ghi danh |

### Bảng `lessons`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã bài học |
| course_id | BIGINT, FK | Thuộc khóa học nào |
| title | VARCHAR | Tiêu đề bài học |
| video_url | VARCHAR | Link YouTube nhúng |
| transcript_text | TEXT | Nội dung tóm tắt để AI đọc |
| order_index | INT | Thứ tự bài học |
| created_at | TIMESTAMP | Thời điểm tạo |

### Bảng `quizzes`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã quiz |
| lesson_id | BIGINT, FK | Bài học sở hữu Quiz |
| title | VARCHAR | Tên quiz |
| status | VARCHAR/ENUM | DRAFT, PUBLISHED, CLOSED |
| created_by | BIGINT, FK | Người tạo |
| created_at | TIMESTAMP | Thời điểm tạo |

### Bảng `questions`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã câu hỏi |
| quiz_id | BIGINT, FK | Quiz chứa câu hỏi |
| content | TEXT | Nội dung câu hỏi |
| question_type | VARCHAR | MULTIPLE_CHOICE |
| explanation | TEXT | Giải thích đáp án |
| generated_by_ai | BOOLEAN | Có được AI sinh hay không |

### Bảng `question_options`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã phương án |
| question_id | BIGINT, FK | Câu hỏi sở hữu |
| content | TEXT | Nội dung phương án |
| is_correct | BOOLEAN | Có phải đáp án đúng không |

### Bảng `quiz_attempts`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã lượt làm bài |
| quiz_id | BIGINT, FK | Quiz được làm |
| student_id | BIGINT, FK | Sinh viên làm bài |
| score | DECIMAL | Điểm số |
| started_at | TIMESTAMP | Thời điểm bắt đầu |
| submitted_at | TIMESTAMP | Thời điểm nộp |

### Bảng `student_answers`
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã câu trả lời |
| attempt_id | BIGINT, FK | Lượt làm bài |
| question_id | BIGINT, FK | Câu hỏi |
| selected_option_id | BIGINT, FK | Phương án đã chọn |
| is_correct | BOOLEAN | Kết quả đúng/sai |

### Bảng `flashcards` (Mở rộng)
| Cột | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| id | BIGINT, PK | Mã flashcard |
| lesson_id | BIGINT, FK | Bài học nguồn |
| front_text | TEXT | Mặt trước |
| back_text | TEXT | Mặt sau |
| generated_by_ai | BOOLEAN | Có được AI sinh hay không |

## 9. Quan hệ dữ liệu chính

```text
User ──< Course
User ──< CourseMember >── Course
Course ──< Lesson
Lesson ──< Quiz
Lesson ──< Flashcard
Quiz ──< Question ──< QuestionOption
User ──< QuizAttempt >── Quiz
QuizAttempt ──< StudentAnswer
```

## 10. Các ràng buộc quan trọng

- Email người dùng phải duy nhất.
- Một sinh viên không được ghi danh cùng một khóa học hai lần.
- Chỉ giáo viên sở hữu khóa học hoặc admin mới được chỉnh sửa khóa học và bài học.
- Khóa học phải có `is_published = true` mới hiển thị trên Marketplace.
- Sinh viên chỉ được làm quiz thuộc khóa học mình đã ghi danh.
- Chỉ quiz ở trạng thái `PUBLISHED` mới được làm.
- Mỗi câu trắc nghiệm phải có ít nhất hai phương án, trong đó có một đáp án đúng.
- Khi submit bài, việc lưu câu trả lời và điểm cần nằm trong cùng một transaction.

## 11. Truy vấn và thống kê có thể dùng khi bảo vệ CSDL

- **Marketplace:** Lấy danh sách các khóa học đã `is_published = true`, kèm theo số lượng học viên đã ghi danh.
- Điểm trung bình theo từng quiz.
- Số lượt làm bài của mỗi quiz.
- Các câu hỏi bị trả lời sai nhiều nhất để đánh giá độ khó.
- Tỷ lệ hoàn thành quiz của sinh viên trong một khóa học.

Ví dụ truy vấn câu hỏi bị sai nhiều nhất:
```sql
SELECT q.id,
       q.content,
       COUNT(*) AS wrong_count
FROM student_answers sa
JOIN questions q ON q.id = sa.question_id
WHERE sa.is_correct = FALSE
GROUP BY q.id, q.content
ORDER BY wrong_count DESC;
```

Ví dụ truy vấn khóa học Marketplace kèm số lượt ghi danh:
```sql
SELECT c.id, c.title, c.thumbnail_url, c.price,
       COUNT(cm.student_id) as total_students
FROM courses c
LEFT JOIN course_members cm ON c.id = cm.course_id
WHERE c.is_published = TRUE
GROUP BY c.id, c.title, c.thumbnail_url, c.price
ORDER BY total_students DESC;
```

## 12. Cấu trúc repository đề xuất

```text
learning-ai-system/
├── docs/
│   ├── requirements.md
│   ├── architecture.md
│   ├── erd.md
│   └── api-spec.md
├── database/
│   ├── schema.sql
│   ├── seed.sql
│   └── queries.sql
├── java-backend/
│   └── ...
├── python-backend/
│   └── ...
└── frontend/
    └── ...
```

Java backend:
```text
java-backend/src/main/java/.../
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── security/
├── exception/
└── config/
```

Python backend:
```text
python-backend/app/
├── routers/
├── services/
├── repositories/
├── models/
├── schemas/
├── auth/
├── ai/
├── database/
└── main.py
```

## 13. Nguyên tắc để dễ đổi framework

- Controller/Router chỉ nhận request, validate dữ liệu và gọi service.
- Business logic phải nằm trong service, không đặt trong controller.
- Service không viết SQL trực tiếp.
- Repository chịu trách nhiệm truy cập CSDL.
- API contract không phụ thuộc Spring Boot hay FastAPI.
- Dùng DTO/Pydantic Schema thay vì trả trực tiếp Entity/Model.
- Thống nhất tên trường, mã lỗi và cấu trúc JSON giữa hai bản.
- Giữ một bộ test case nghiệp vụ chung để kiểm tra cả hai backend.

Ví dụ luồng phụ thuộc:
```text
Controller / Router
        ↓
      Service
        ↓
    Repository
        ↓
     Database
```

## 14. Kế hoạch triển khai gợi ý

### Giai đoạn 1 — Phân tích và thiết kế
- Chốt yêu cầu chức năng.
- Vẽ use case và ERD.
- Chốt schema CSDL.
- Viết API contract bằng Swagger/OpenAPI.

### Giai đoạn 2 — Chức năng nền tảng (MOOC Core)
- Authentication và phân quyền.
- CRUD khóa học, upload thumbnail.
- Marketplace và tính năng Học viên ghi danh (Enroll).

### Giai đoạn 3 — Lesson và Quiz
- Quản lý Bài học (nhúng Video YouTube).
- CRUD quiz/câu hỏi/đáp án.
- Giao diện xem video, làm bài và submit.
- Chấm điểm và lịch sử.

### Giai đoạn 4 — AI và thống kê
- Trích xuất văn bản từ file/transcript.
- Generate quiz/flashcard bằng LLM API.
- Review nội dung AI.
- Thống kê kết quả.

### Giai đoạn 5 — Hoàn thiện
- Integration test.
- Xử lý lỗi (đặc biệt lỗi khi gọi API AI).
- Seed dữ liệu demo.
- Hoàn thiện báo cáo, slide và kịch bản demo.

## 15. Kịch bản demo đề xuất

1. **[Teacher]** Đăng nhập, tạo một khóa học "Nhập môn Machine Learning", thêm ảnh bìa đẹp mắt, set giá 0đ, set trạng thái Published.
2. **[Teacher]** Tạo Bài học 1, dán link một video YouTube vào `video_url`, paste nội dung kịch bản (transcript) hoặc đính kèm PDF vào hệ thống.
3. **[Teacher]** Bấm nút "AI Generate Quiz". Đợi hệ thống gọi LLM và trả về 3 câu trắc nghiệm. Sửa 1 câu rồi bấm "Publish Quiz".
4. **[Student]** Mở trình duyệt ẩn danh, vào Trang chủ lướt thấy khóa học rất đẹp. Đăng ký tài khoản, đăng nhập.
5. **[Student]** Bấm "Ghi danh khóa học". Màn hình chuyển vào không gian học tập.
6. **[Student]** Bấm Play xem video (chạy qua YouTube iframe). Sau đó làm Quiz cuối bài, Submit và xem điểm lập tức.
7. **[Admin/Teacher]** Mở thống kê xem điểm trung bình và các câu bị học viên sai nhiều nhất.
8. Nhóm mở CSDL để giải thích bảng, quan hệ, transaction và cách Backend xử lý luồng AI JSON.

## 16. Kết luận

Hướng triển khai được chốt là:
> **Một sản phẩm MOOC, một CSDL, một API contract; hai backend implementation: Spring Boot + Hibernate và FastAPI + SQLAlchemy.**

Thiết kế này giúp nhóm dùng chung công sức phân tích, CSDL, API, giao diện và kiểm thử, đồng thời giải quyết triệt để bài toán hệ thống giáo dục hiện đại (Video + AI) mà vẫn đảm bảo tính khả thi cao.