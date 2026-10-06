# Danh mục endpoint — thiết kế MVP

Mọi route có prefix `/api/v1`. A=ADMIN, T=TEACHER, S=STUDENT, U=user active đăng nhập, O=owner của course hoặc admin, E=student đã enroll. Guest chỉ route Public. Group trong cột cuối nối `payloads.md`; errors dùng conventions. Route đọc list phân trang trừ object có mảng con giới hạn.

| Nhóm | Method/path | Quyền | Payload / response |
|---|---|---|---|
| AUTH-01 | POST /auth/register | Public | Register → User (201) |
| AUTH-01 | POST /auth/login | Public | Login → TokenPair |
| AUTH-01 | POST /auth/refresh | Refresh credential | Refresh → TokenPair |
| AUTH-01 | POST /auth/logout | Session credential | Logout → 204; retry 204 |
| AUTH-02 | GET /users/me | U | User |
| AUTH-02 | PATCH /users/me | U | Profile → User; không nhận role/status |
| AUTH-02 | GET /users | A | Page<User> |
| AUTH-02 | PATCH /users/{user_id}/status | A | UserStatus → User |
| USER-01 | POST /teacher-requests | S | TeacherRequest → Request (201) |
| USER-01 | GET /teacher-requests/me | U | Page<Request> |
| USER-01 | GET /teacher-requests | A | Page<Request> |
| USER-01 | PATCH /teacher-requests/{request_id}/decision | A | Decision → Request |
| CAT-01 | GET /categories | Public | Page<Category> |
| CAT-01 | POST /categories | A | CategoryWrite → Category (201) |
| CAT-01 | PATCH /categories/{category_id} | A | CategoryWrite → Category |
| CAT-01 | DELETE /categories/{category_id} | A | 204 hoặc RESOURCE_IN_USE |
| COURSE-02 | GET /courses | Public | q/category_id/page → Page<CoursePublic> |
| COURSE-02 | GET /courses/{course_id} | Public | Published CoursePublic + outline |
| COURSE-01 | GET /teaching/courses | T/A | Page<CourseOwner>; A thấy toàn bộ |
| COURSE-01 | GET /teaching/courses/{course_id} | O | CourseOwner |
| COURSE-01 | POST /courses | T/A | CourseWrite → CourseOwner (201) |
| COURSE-01 | PATCH /courses/{course_id} | O | CourseWrite → CourseOwner |
| COURSE-01 | POST /courses/{course_id}/thumbnail | O | multipart image → CourseOwner |
| COURSE-01 | PATCH /courses/{course_id}/publication | O | Publication → CourseOwner |
| COURSE-01 | PATCH /courses/{course_id}/archive | O | Archive → CourseOwner |
| COURSE-01 | DELETE /courses/{course_id} | O | 204; chỉ draft chưa sử dụng |
| ENROLL-01 | POST /courses/{course_id}/enroll | S | không body → Enrollment (201/200) |
| ENROLL-01 | GET /users/me/courses | S | Page<CourseEnrollment> |
| ENROLL-01 | GET /courses/{course_id}/members | O | Page<Member> |
| LESSON-01 | GET /courses/{course_id}/lessons | O/E | Page<Lesson>; private |
| LESSON-01 | POST /courses/{course_id}/lessons | O | LessonWrite → Lesson (201) |
| LESSON-01 | GET /lessons/{lesson_id} | O/E | Lesson; transcript chỉ O |
| LESSON-01 | PATCH /lessons/{lesson_id} | O | LessonWrite → Lesson |
| LESSON-01 | DELETE /lessons/{lesson_id} | O | 204; chưa có lịch sử |
| LESSON-01 | POST /lessons/{lesson_id}/documents | O | multipart PDF/TXT → Document (201) |
| LESSON-01 | GET /lessons/{lesson_id}/documents | O/E | Page<Document> |
| LESSON-01 | GET /documents/{document_id}/content | O/E | file bytes |
| LESSON-01 | DELETE /documents/{document_id} | O | 204; generation giữ source hash |
| QUIZ-01 | GET /lessons/{lesson_id}/quizzes | O/E | Page<QuizSummary>; E chỉ Published |
| QUIZ-01 | POST /lessons/{lesson_id}/quizzes | O | QuizWrite → QuizAuthor (201) |
| QUIZ-01 | GET /quizzes/{quiz_id} | O/E | O: QuizAuthor; E: QuizStudent |
| QUIZ-01 | PATCH /quizzes/{quiz_id} | O | QuizWrite → QuizAuthor; Draft |
| QUIZ-01 | DELETE /quizzes/{quiz_id} | O | 204; Draft |
| QUIZ-01 | POST /quizzes/{quiz_id}/questions | O | QuestionWrite → QuestionAuthor (201) |
| QUIZ-01 | PUT /questions/{question_id} | O | QuestionWrite → QuestionAuthor; Draft |
| QUIZ-01 | DELETE /questions/{question_id} | O | 204; Draft |
| QUIZ-01 | PATCH /quizzes/{quiz_id}/publication | O | Status → QuizAuthor |
| ATTEMPT-01 | POST /quizzes/{quiz_id}/attempts | S/E | không body → Attempt (201/200) |
| ATTEMPT-01 | POST /attempts/{attempt_id}/submit | Chủ attempt | Submit → Result |
| ATTEMPT-01 | GET /attempts/{attempt_id} | Chủ/O | Attempt hoặc Result tùy status |
| ATTEMPT-01 | GET /users/me/attempts | S | quiz_id/course_id → Page<AttemptSummary> |
| AI-01 | POST /ai/generate-quiz | O của lesson | GenerationRequest → QuizCandidate |
| AI-01 | POST /ai/generate-flashcards | O của lesson | GenerationRequest → CardCandidate |
| CARD-01 | GET /lessons/{lesson_id}/flashcards | O/E | Page<Card>; E chỉ Published |
| CARD-01 | POST /lessons/{lesson_id}/flashcards | O | CardWrite → Card (201) |
| CARD-01 | PATCH /flashcards/{card_id} | O | CardWrite → Card; Draft |
| CARD-01 | DELETE /flashcards/{card_id} | O | 204; Draft |
| CARD-01 | PATCH /flashcards/{card_id}/publication | O | Status → Card |
| STAT-01 | GET /statistics/courses/{course_id} | O | CourseStatistics |
| STAT-01 | GET /statistics/quizzes/{quiz_id} | O | QuizStatistics |
| STAT-01 | GET /statistics/system | A | SystemStatistics |

Public detail draft trả 404; owner đọc qua teaching route. Các route có lesson/quiz/document/question/card ID phải lần ra course và kiểm tra owner/enrollment, không chỉ kiểm tra role. Logout chấp nhận refresh credential nếu access hết hạn; xác minh token và thu hồi cùng session, không yêu cầu access còn sống. Không có endpoint đổi role trực tiếp hoặc mua khóa trong MVP.
