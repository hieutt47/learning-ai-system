# Backlog chưa phân công

Không phải lịch hay bảng chia người. Mỗi mục cần tách thành issue đủ nhỏ sau khi chốt contract.

| ID | Gói đầu ra | Phụ thuộc | Yêu cầu |
|---|---|---|---|
| B01 | Chốt phạm vi, phiên bản công cụ, frontend, flashcard | Không | D06/D07/D14 |
| B02 | OpenAPI/JSON schemas machine-readable, fixture payload | B01 | Contract |
| B03 | SQL migration đầu, seed test/demo | B01/B02 | Mô hình dữ liệu |
| B04 | Java build/config/bootstrap và error handling | B01/B02 | Nền tảng |
| B05 | Auth/session/user/Teacher request/category | B03/B04 | AUTH/USER/CAT |
| B06 | Course/thumbnail/marketplace/enroll | B05 | COURSE/ENROLL |
| B07 | Lesson/video/document/transcript | B06 | LESSON |
| B08 | Quiz/question/options/publication | B07 | QUIZ |
| B09 | Attempt/submit/score/history | B08 | ATTEMPT |
| B10 | Frontend guest/student/teacher/admin | B02, tích hợp B05–B09 | UI |
| B11 | AI mock + provider adapter + preview review | B07/B08 | AI |
| B12 | Flashcard Draft/Published/view | B07/B11, D07 | CARD |
| B13 | Statistics course/quiz/system | B09 | STAT |
| B14 | Integration, quyền, retry/race, lỗi AI/file | B05–B13 | Shared scenarios |
| B15 | Báo cáo, slide, seed demo, runbook | B14 | Delivery |
| B16 | Backend Python tương thích | Java baseline ổn định | Portability |

Frontend làm layout/mock từ B02, không đợi toàn backend. Statistics có thể thiết kế query từ B03; AI mock làm trước provider thật. Chỉ gán owner/reviewer khi lead và nhóm cùng chia việc.
