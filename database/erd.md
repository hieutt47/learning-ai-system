# ERD logic

Tên bảng/field chính theo `data-dictionary.md`. Đây là sơ đồ thiết kế, chưa sinh SQL.

```mermaid
erDiagram
  users ||--o{ auth_sessions : has
  users ||--o{ teacher_requests : requests
  users ||--o{ courses : owns
  categories ||--o{ courses : groups
  users ||--o{ course_members : enrolls
  courses ||--o{ course_members : contains
  courses ||--o{ lessons : contains
  lessons ||--o{ lesson_documents : attaches
  lessons ||--o{ ai_generations : sources
  users ||--o{ ai_generations : requests
  lessons ||--o{ quizzes : contains
  ai_generations o|--o{ quizzes : provenance
  quizzes ||--o{ questions : contains
  ai_generations o|--o{ questions : provenance
  questions ||--o{ question_options : contains
  quizzes ||--o{ quiz_attempts : receives
  users ||--o{ quiz_attempts : submits
  quiz_attempts ||--o{ student_answers : contains
  questions ||--o{ student_answers : answers
  question_options o|--o{ student_answers : selected
  lessons ||--o{ flashcards : contains
  ai_generations o|--o{ flashcards : provenance
```

Các FK audit (created_by, decided_by, uploaded_by) và nguồn document nullable được mô tả trong từ điển để sơ đồ dễ đọc. `course_members` có PK kép. `student_answers.selected_option_id` nullable biểu diễn bỏ trống. User dùng một role, không có bảng multi-role ở MVP.
