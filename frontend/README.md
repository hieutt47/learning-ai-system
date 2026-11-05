# Frontend dùng chung

SPA **React + TypeScript + Vite** (D06), dùng chung cho backend Java và Python: chỉ đổi địa chỉ API, không thêm nhánh payload theo backend. Thư viện: React Router (route), TanStack Query (gọi API, cache, loading/error), Ant Design (component, locale tiếng Việt).

**Giao diện làm theo [`docs/03-experience/prototype.html`](../docs/03-experience/prototype.html)** (mở bằng trình duyệt, đổi màn bằng `#route`) và [`design-brief.md`](../docs/03-experience/design-brief.md). Khung app (sidebar, header, ô tài khoản, sáng/tối, icon, màu) đã chép đúng markup/CSS/token của prototype; mỗi màn hình làm theo màn cùng tên trong prototype, dùng component Ant Design (đã chỉnh theme khớp prototype) và icon `Icon` của prototype.

## Chạy

Cần Node 20+ (đã thử với Node 24).

```bash
cd frontend
npm install
cp .env.example .env.local   # chỉnh nếu cần, .env.local không commit
npm run dev                  # http://localhost:5173
```

| Lệnh | Việc |
|---|---|
| `npm run dev` | Chạy dev, tự reload |
| `npm run typecheck` | Kiểm tra kiểu TypeScript |
| `npm run lint` | Lint (oxlint) |
| `npm run build` | Typecheck + build ra `dist/` |

Khi dev, mọi request `/api/...` đi qua proxy của Vite tới backend (`http://localhost:8080`, đổi bằng biến `VITE_DEV_BACKEND`), nên không cần cấu hình CORS. Biến môi trường trong `.env.example`:

- `VITE_API_BASE_URL`: gốc API, mặc định `/api/v1`.
- `VITE_DEV_USER_ID`: **chỉ khi dev, trong lúc auth chưa merge** — backend đang dùng stub đọc header `X-User-Id`. Bỏ khi có JWT.
- `VITE_DEV_ROLE`: vai trò để hiện menu và chặn trang (`GUEST | STUDENT | TEACHER | ADMIN`) khi chưa có `GET /users/me`; `VITE_DEV_USER_NAME`: tên ở ô tài khoản.

Vite chỉ đọc `.env.local` (không đọc `.env.example`); đổi file thì dev server tự khởi động lại.

**Trang dev `/dev/screens`** (link "Danh sách màn hình" cuối sidebar, chỉ có khi `npm run dev`): mở nhanh mọi màn trong lúc chưa có nút điều hướng, và đổi vai trò xem giao diện (Khách / Sinh viên / Giảng viên / Admin) mà không phải sửa `.env.local`.

## Cấu trúc

```text
src/
  main.tsx, App.tsx          Provider: theme sáng/tối, Ant Design, TanStack Query, Router
  index.css                  Nạp styles/tokens.css + styles/shell.css
  shared/
    api/client.ts            Gọi API: base URL, Authorization, đọc lỗi {code, message, details}, 401 → đăng nhập
    api/types.ts             Id (string), Page<T>, ApiErrorBody, mã lỗi
    api/queryClient.ts       GET thử lại 1 lần (trừ 4xx); POST/PATCH/DELETE không tự thử lại
    auth/currentUser.ts      Người đang xem (tạm đọc VITE_DEV_*), nhãn vai trò
    routing/paths.ts         Đường dẫn các trang + trang chủ theo vai trò — dùng khi điều hướng, không tự ghép URL
    routing/screens.ts       Danh sách TẤT CẢ màn hình: tiêu đề, người phụ trách, màn prototype, quyền
    routing/router.tsx       Bảng route: tiêu đề, tab, quyền truy cập
    routing/guards.tsx       Chặn trang theo vai trò (như bảng ACCESS của prototype)
    components/AppLayout.tsx Khung sidebar + header theo prototype
    components/Icon.tsx      Bộ icon của prototype: <Icon name="compass" />
    components/PageState.tsx Loading / empty / error / 403 / 404
    components/pageHeader.ts usePageHeader(): đặt tiêu đề + breadcrumb động (vd tên khóa học)
    dev/DevScreensPage.tsx   Trang /dev/screens (chỉ khi dev)
    styles/tokens.css        Biến màu sáng/tối của prototype + --ai-* (màu AI)
    styles/shell.css         CSS khung app chép từ prototype (giữ tên class)
    theme/                   Theme Ant Design khớp token; chế độ sáng/tối (useThemeMode)
  features/
    auth/  marketplace/  learning/  teaching/  admin/   Màn hình theo khu vực
```

## Thêm một màn hình

1. Tạo nhánh từ `develop`, ví dụ `feat/fe-quiz-editor`.
2. Tạo trang trong `src/features/<khu vực>/`, ví dụ `src/features/teaching/quiz/QuizEditorPage.tsx`.
3. Trong `src/shared/routing/router.tsx`, thay `...todo('key')` của route đó bằng `element: <TrangMoi />, handle: { title: '...' }`. Tiêu đề phụ thuộc dữ liệu (tên khóa học, tên bài) thì gọi `usePageHeader({ title, crumbs })` trong trang. Route, quyền và người phụ trách đã đặt sẵn theo prototype và design-brief mục 6; cần route mới thì thêm vào `paths.ts` trước.
4. Làm giao diện theo màn cùng tên trong `prototype.html`: dùng component Ant Design, màu qua `cssVar` trong `shared/theme/antdTheme.ts` hoặc biến CSS (`var(--primary)`, `var(--ai)`…), không viết mã màu cứng.
5. Gọi API qua `shared/api/client.ts` (`apiGet`, `apiPost`, …) bên trong `useQuery` / `useMutation`; không gọi `fetch` trực tiếp. Kiểu dữ liệu khai báo theo `contracts/payloads.md`, giữ tên field snake_case, ID là `string`.
6. Mỗi màn hình có đủ loading / empty / error / forbidden: dùng `LoadingState`, `EmptyState`, `ErrorState` trong `shared/components/PageState.tsx`. Lỗi 400 hiện dưới từng field bằng `error.fieldErrors()`.

Trang có **thanh tab** (như prototype) khai báo `handle.tabs` ở route cha, mỗi tab là một **route con** để mỗi người chỉ sửa tab của mình:

| Trang | Tab (người phụ trách) |
|---|---|
| Khóa học `/teaching/courses/:courseId` | Thông tin (2) · Bài học (3) · Học viên (2) |
| Soạn bài học `/teaching/lessons/:lessonId` | Nội dung (3) · Quiz (4) · AI & Flashcard (5), thêm `ai/preview` (5) |
| Học bài `/learn/:courseId/lessons/:lessonId` | Tài liệu (3) · Quiz (4) · Flashcard (5) |

## Màn hình và phân công

> **Quy tắc — đọc trước khi code**
> - **Mỗi người chỉ làm màn có tên mình** trong bảng dưới. Không làm, không sửa, không thay trang giữ chỗ ("Màn hình chưa làm — phụ trách: người N") của người khác, kể cả khi đang rảnh hoặc thấy dễ. Muốn giúp thì báo người đó và lead trước.
> - Bảng việc của người khác chỉ để **biết ranh giới** (ví dụ người 5 biết nút "Sinh quiz bằng AI" nằm ở màn của người 4), không phải việc của mình.
> - Dùng AI hỗ trợ code: **chỉ đưa cho AI bảng việc của mình** và nói rõ "chỉ làm các màn này, giữ nguyên các route khác".
> - Sửa file trong `src/shared/` (layout, api client, theme, router ngoài route của mình) thì nêu trong PR để cả nhóm biết.
> - Mỗi PR chỉ chứa màn của một người; route của mình là phần duy nhất được đổi trong `router.tsx`.
> - Nút chuyển màn làm **đúng bảng "Điều hướng giữa các màn"** bên dưới; cần đường đi mới thì thống nhất và sửa bảng trước.

Mỗi màn làm theo màn cùng tên trong `prototype.html` (cột Prototype: mở prototype, gõ `#<tên>` sau địa chỉ). API theo `contracts/api-catalog.md` (bỏ tiền tố `/api/v1`). Mọi màn có đủ loading / empty / error / forbidden.

### Người 1 — Tài khoản & quản trị

| Màn | Route | Prototype | Việc cần làm |
|---|---|---|---|
| Đăng nhập | `/login` | `#login` | Form email + mật khẩu, bố cục chia đôi như prototype. `POST /auth/login`, lưu token theo thiết kế auth rồi nối `setAccessTokenProvider`; xong quay về `?redirect=`. Lỗi sai mật khẩu, `ACCOUNT_INACTIVE`. |
| Đăng ký | `/register` | `#register` | Họ tên, email, mật khẩu 10–128 ký tự. `POST /auth/register`; lỗi `EMAIL_ALREADY_EXISTS` hiện dưới field. |
| Hồ sơ | `/profile` | `#profile` | Khối Thông tin cá nhân (`PATCH /users/me`, Hủy/Lưu), Trở thành giảng viên (chỉ sinh viên, `POST /teacher-requests`), Lịch sử yêu cầu (`GET /teacher-requests/me`). |
| Người dùng | `/admin/users` | `#admusers` | Bảng Họ tên / Vai trò / Trạng thái / Ngày tạo, tìm và lọc, phân trang. Khóa / Mở khóa (`PATCH /users/{id}/status`) có modal xác nhận "Khóa tài khoản?". `GET /users`. |
| Duyệt giảng viên | `/admin/teacher-requests` | `#admreq` | Danh sách yêu cầu (Người gửi, Lý do, Ngày gửi) + khung Chi tiết yêu cầu; Duyệt / Từ chối (bắt buộc ghi chú khi từ chối). `GET /teacher-requests`, `PATCH /teacher-requests/{id}/decision`. |
| Danh mục | `/admin/categories` | `#admcat` | Bảng Tên / Mô tả / Số khóa học đang dùng; modal Thêm/Sửa; xóa có xác nhận; `RESOURCE_IN_USE` → modal "Không thể xóa danh mục". `GET/POST/PATCH/DELETE /categories`. |
| Phần chung về auth | — | — | Thay `useCurrentUser` (đang đọc `VITE_DEV_*`) bằng `GET /users/me`; nút Đăng xuất trong menu tài khoản gọi `POST /auth/logout`; bỏ header `X-User-Id` trong `api/client.ts`. |

### Người 2 — Khóa học & ghi danh

| Màn | Route | Prototype | Việc cần làm |
|---|---|---|---|
| Khám phá khóa học | `/explore` | `#explore` | Khối "Học gì hôm nay?", lọc theo danh mục, tìm kiếm (đọc `?q=` từ ô tìm trên header), lưới thẻ khóa học, phân trang, empty "Không tìm thấy khóa học" + Xóa bộ lọc. `GET /courses`, `GET /categories`. |
| Chi tiết khóa học | `/courses/:courseId` | `#course` | Breadcrumb, Giới thiệu khóa học, Đề cương (outline); nút Ghi danh miễn phí (khách → `/login?redirect=`) hoặc Vào học. `GET /courses/{id}`, `POST /courses/{id}/enroll`. |
| Khóa học của tôi | `/my-courses` | `#mycourses` | Danh sách đã ghi danh, nút Vào học → `/learn/:courseId/lessons/:lessonId`; 2 empty state ("Bạn chưa ghi danh khóa học nào" + Khám phá, "Không có khóa học khớp"). `GET /users/me/courses`. |
| Khóa học giảng dạy (màn tổng quan của giảng viên) | `/teaching/courses` | `#teach` | Khối **Tổng quan** (thẻ số liệu), bảng **Khóa học của tôi** (Trạng thái, Bài học, Học viên, Cập nhật, thao tác Xuất bản / Gỡ xuất bản / Lưu trữ / Khôi phục thành Draft có modal xác nhận), khối **Học viên mới ghi danh**, nút Tạo khóa học. `GET /teaching/courses`, `POST /courses`, `PATCH /courses/{id}/publication`, `PATCH /courses/{id}/archive`. |
| Khóa học · Thông tin | `/teaching/courses/:courseId` | `#tcourse` | Thông tin cơ bản (tên, mô tả, danh mục, giá), Ảnh bìa (`POST /courses/{id}/thumbnail`), Điều kiện xuất bản (checklist), Trạng thái. `GET /teaching/courses/{id}`, `PATCH /courses/{id}`. Đặt tên khóa học + breadcrumb lên header bằng `usePageHeader` cho cả 3 tab. |
| Khóa học · Học viên | `/teaching/courses/:courseId/students` | `#tstudents` | Bảng học viên đã ghi danh (Họ tên, ngày ghi danh), sắp xếp. `GET /courses/{id}/members`. |
| Khóa học (quản trị) | `/admin/courses` | `#admcourses` | Bảng tất cả khóa học (Giảng viên, Trạng thái, Học viên, Cập nhật), nút Xem, Gỡ xuất bản có xác nhận. `GET /teaching/courses` (admin thấy toàn bộ). |

### Người 3 — Bài học & tài liệu

| Màn | Route | Prototype | Việc cần làm |
|---|---|---|---|
| Khóa học · Bài học | `/teaching/courses/:courseId/lessons` | `#tlessons` | Bảng bài học (#, Tên bài, Transcript, Tài liệu, Quiz, Flashcard, Thao tác), kéo thả hoặc ↑/↓ đổi thứ tự, modal Thêm bài học, xác nhận "Xóa bài học?". `GET/POST /courses/{id}/lessons`, `PATCH/DELETE /lessons/{id}`. |
| Soạn bài học · Nội dung | `/teaching/lessons/:lessonId` | `#tlesson` | Khối Video bài học (YouTube URL + xem trước), Transcript, Tài liệu (upload PDF/TXT ≤ 10 MiB, lỗi `FILE_TOO_LARGE` / `UNSUPPORTED_FILE_TYPE`, xóa). `GET/PATCH /lessons/{id}`, `GET/POST /lessons/{id}/documents`, `DELETE /documents/{id}`. Đặt tên bài + breadcrumb (khóa học › bài) lên header bằng `usePageHeader` cho cả 3 tab. |
| Học bài · Tài liệu | `/learn/:courseId/lessons/:lessonId` | `#learn` | **Phần chung của trang học bài**: video YouTube (link "Mở trên YouTube" khi lỗi), danh sách bài bên cạnh, chưa ghi danh → empty "Bạn chưa ghi danh khóa học này" + Ghi danh. Làm thành `element` của route cha `/learn/:courseId/lessons/:lessonId` có `<Outlet />` để tab Quiz (người 4), Flashcard (người 5) hiện bên trong. Tab Tài liệu: danh sách + Tải xuống. `GET /lessons/{id}`, `GET /courses/{id}/lessons`, `GET /lessons/{id}/documents`, `GET /documents/{id}/content`. |

### Người 4 — Quiz, kết quả & thống kê

| Màn | Route | Prototype | Việc cần làm |
|---|---|---|---|
| Soạn bài học · Quiz | `/teaching/lessons/:lessonId/quizzes` | `#tquizzes` | Bảng quiz (Tên, Trạng thái, Số câu, Ngày tạo, Thao tác), Tạo quiz, nút "Sinh quiz bằng AI" chuyển sang tab AI & Flashcard (`paths.teachingLessonAi`), Đóng quiz / Xóa quiz nháp có xác nhận. `GET/POST /lessons/{id}/quizzes`, `PATCH /quizzes/{id}/publication`, `DELETE /quizzes/{id}`. |
| Soạn quiz | `/teaching/quizzes/:quizId/edit` | `#tquiz` | Câu hỏi, 2–6 đáp án, đúng 1 đáp án đúng, giải thích; Thêm câu / Thêm đáp án; Lưu nháp; Điều kiện xuất bản + Xuất bản (xác nhận "Xuất bản quiz?"); quiz đã xuất bản → "Tạo bản nháp mới". `GET/PATCH /quizzes/{id}`, `POST /quizzes/{id}/questions`, `PUT/DELETE /questions/{id}`, `PATCH /quizzes/{id}/publication`. |
| Học bài · Quiz | `/learn/:courseId/lessons/:lessonId/quiz` | `#learn` (tab Quiz) | Danh sách quiz đã xuất bản + điểm cao nhất, nút Bắt đầu → `/quizzes/:quizId`; empty "Bài học chưa có quiz". `GET /lessons/{id}/quizzes`. |
| Làm bài quiz | `/quizzes/:quizId` | `#quiz` | Từng câu, Câu trước / Câu sau, ô điều hướng câu, Nộp bài + modal "Nộp bài?" (báo câu bỏ trống); lỗi mạng không mất câu đã chọn. `POST /quizzes/{id}/attempts`, `POST /attempts/{id}/submit`. |
| Kết quả | `/results/:attemptId` | `#result` | Điểm, số câu đúng, Xem lại bài làm (lọc câu sai, empty "Không có câu sai"), Làm lại, Về bài học. `GET /attempts/{id}`. |
| Lịch sử làm bài | `/attempts` | `#history` | Bảng Bài kiểm tra / Khóa học / Trạng thái / Điểm / Bắt đầu / Nộp lúc / Thao tác; empty "Chưa có lần làm bài nào". `GET /users/me/attempts`. |
| Thống kê khóa học | `/teaching/statistics` | `#stats` | Chọn khóa học, thẻ số liệu, Tỷ lệ hoàn thành, bảng Quiz của khóa (Lượt nộp, Điểm trung bình) → Thống kê quiz. `GET /statistics/courses/{id}`. |
| Thống kê quiz | `/teaching/statistics/quizzes/:quizId` | `#statsquiz` | Câu bị sai nhiều nhất, Điểm cao nhất theo học viên; empty "Chưa có lượt nộp". `GET /statistics/quizzes/{id}`. |
| Thống kê hệ thống | `/admin/statistics` | `#admstats` | Thẻ số liệu, Giảng viên / Sinh viên, Cách tính số liệu. `GET /statistics/system`. (Design brief: người 4 hoặc người lead giao.) |

### Người 5 — AI & flashcard

| Màn | Route | Prototype | Việc cần làm |
|---|---|---|---|
| Soạn bài học · AI & Flashcard | `/teaching/lessons/:lessonId/ai` | `#tai` | Panel **Sinh bằng AI** (loại Quiz/Flashcard, nguồn Transcript/Tài liệu, số lượng 1–10, độ khó cho quiz, nút "Sinh bản nháp" màu AI, trạng thái "AI đang soạn…", lỗi `AI_TIMEOUT` / `AI_PROVIDER_ERROR` / `AI_OUTPUT_INVALID` / `SOURCE_TEXT_EMPTY` + Thử lại, link Soạn thủ công). Bảng **Flashcard của bài** (Mặt trước, Mặt sau, Trạng thái, badge "AI tạo", Ngày tạo; chọn nhiều → Xuất bản đã chọn; Xuất bản / Đóng từng thẻ; modal Tạo thẻ thủ công / Sửa; xác nhận "Xóa thẻ nháp?"). `POST /ai/generate-quiz`, `POST /ai/generate-flashcards`, `GET/POST /lessons/{id}/flashcards`, `PATCH/DELETE /flashcards/{id}`, `PATCH /flashcards/{id}/publication`. |
| Xem trước kết quả AI | `/teaching/lessons/:lessonId/ai/preview` | `#taipreview` | Banner tím "Bản nháp do AI tạo — chưa được lưu"; cảnh báo khi `source_truncated`. Quiz: sửa câu/đáp án/giải thích, câu không hợp lệ viền đỏ chặn lưu, Lưu thành bản nháp gọi API quiz của người 4 kèm `generation_id`. Flashcard: lưới thẻ sửa trực tiếp, lưu `POST /lessons/{id}/flashcards` kèm `generation_id`. Sinh lại, Hủy; rời trang khi chưa lưu → modal "Rời trang xem trước?". |
| Học bài · Flashcard | `/learn/:courseId/lessons/:lessonId/flashcards` | `#learn` (tab Flashcard) | Một thẻ lớn, bấm hoặc Space để lật, Trước / Sau và phím ←/→, chỉ số "3 / 12"; chỉ thẻ đã xuất bản; empty "Chưa có flashcard". `GET /lessons/{id}/flashcards`. |
| Khung frontend (đã xong) | — | — | Layout, router, api client, theme, icon, trang trạng thái, `/dev/screens` — PR `chore/frontend-scaffold`. |

## Điều hướng giữa các màn

Bản đồ dưới đây chép từ các nút và thao tác trong `prototype.html` (mọi `href(...)` / `go(...)`). Đây là **thỏa thuận chung** về việc màn nào dẫn sang màn nào:

- **Người làm màn nguồn làm nút**, dẫn tới đúng màn đích trong bảng. Màn đích của người khác thì chỉ dẫn tới, không làm hộ.
- **Luôn điều hướng bằng hàm trong `src/shared/routing/paths.ts`** (`navigate(paths.teachingLessonAi(lessonId))`, `<Link to={paths.courseDetail(id)}>`), không tự gõ chuỗi URL. Tham số của hàm (`courseId`, `lessonId`…) là "hợp đồng" giữa hai màn; đổi chữ ký hàm thì phải báo người làm màn còn lại.
- **Dữ liệu giữa hai màn đi qua URL** (ID trên đường dẫn, `?q=`, `?redirect=`); màn đích tự gọi API lấy dữ liệu, không truyền object qua state toàn cục. Ngoại lệ duy nhất: kết quả AI từ tab AI sang trang xem trước (cùng người 5).
- **Nút dẫn tới đâu mà không có trong bảng thì không tự thêm.** Hỏi người làm màn đích và lead, thống nhất xong thì cập nhật bảng này trước rồi mới code.

Sidebar, logo, tab trong trang, menu tài khoản, chuyển về `/login?redirect=` khi gặp 401 và chặn trang theo vai trò đã có sẵn trong khung, không ai phải làm lại.

| Màn nguồn (người làm) | Nút / thao tác | Dẫn tới | Hàm `paths` |
|---|---|---|---|
| Đăng nhập (1) | Link "Đăng ký" | Đăng ký | `paths.register` |
| Đăng nhập (1) | Đăng nhập thành công | `?redirect=` nếu có, không thì trang chủ theo vai trò | `homePathFor(role)` |
| Đăng ký (1) | Link "Đăng nhập"; đăng ký thành công | Đăng nhập | `paths.login` |
| Menu tài khoản (1) | Đăng xuất | Khám phá khóa học | `paths.explore` |
| Khám phá (2) | Bấm thẻ khóa học | Chi tiết khóa học | `paths.courseDetail(courseId)` |
| Chi tiết khóa học (2) | "Ghi danh miễn phí" khi là khách | Đăng nhập, quay lại sau | `` `${paths.login}?redirect=…` `` |
| Chi tiết khóa học (2) | "Vào học" / ghi danh xong | Học bài, bài đầu tiên | `paths.learnLesson(courseId, lessonId)` |
| Khóa học của tôi (2) | "Vào học" | Học bài | `paths.learnLesson(courseId, lessonId)` |
| Khóa học của tôi (2) | Empty → "Khám phá khóa học" | Khám phá | `paths.explore` |
| Khóa học giảng dạy (2) | "Tạo khóa học" (tạo xong) / bấm tên khóa | Khóa học · Thông tin | `paths.teachingCourse(courseId)` |
| Khóa học giảng dạy (2) | Số bài học của khóa | Khóa học · Bài học | `paths.teachingCourseLessons(courseId)` |
| Khóa học giảng dạy (2) | "Học viên mới ghi danh" → "Xem tất cả" | Khóa học · Học viên | `paths.teachingCourseStudents(courseId)` |
| Trang khóa học, 3 tab (2) | Breadcrumb "Khóa học giảng dạy" | Khóa học giảng dạy | `paths.teachingCourses` |
| Khóa học (quản trị) (2) | "Xem" | Chi tiết khóa học | `paths.courseDetail(courseId)` |
| Khóa học · Bài học (3) | Bấm tên bài / "Sửa"; "Thêm bài học" (tạo xong) | Soạn bài học · Nội dung | `paths.teachingLesson(lessonId)` |
| Soạn bài học, 3 tab (3) | Breadcrumb tên khóa học | Khóa học · Bài học | `paths.teachingCourseLessons(courseId)` |
| Học bài (3) | Bấm bài khác trong danh sách bên cạnh | Học bài của bài đó | `paths.learnLesson(courseId, lessonId)` |
| Học bài (3) | Chưa ghi danh → "Ghi danh miễn phí" | Chi tiết khóa học | `paths.courseDetail(courseId)` |
| Soạn bài học · Quiz (4) | "Tạo quiz" (tạo xong) / bấm tên quiz | Soạn quiz | `paths.quizEditor(quizId)` |
| Soạn bài học · Quiz (4) | "Sinh quiz bằng AI" | Soạn bài học · AI & Flashcard | `paths.teachingLessonAi(lessonId)` |
| Soạn quiz (4) | Breadcrumb khóa học › bài học | Khóa học · Bài học / Soạn bài học · Quiz | `paths.teachingCourseLessons(courseId)`, `paths.teachingLessonQuizzes(lessonId)` |
| Soạn quiz (4) | "Tạo bản nháp mới" (quiz đã xuất bản) | Soạn quiz của bản nháp mới | `paths.quizEditor(newQuizId)` |
| Học bài · Quiz (4) | "Bắt đầu" / "Làm lại" | Làm bài quiz | `paths.takeQuiz(quizId)` |
| Làm bài quiz (4) | Nộp bài xong | Kết quả | `paths.attemptResult(attemptId)` |
| Kết quả (4) | "Làm lại" | Làm bài quiz | `paths.takeQuiz(quizId)` |
| Kết quả (4) | "Về bài học" | Học bài · Quiz | `paths.learnLessonQuiz(courseId, lessonId)` |
| Lịch sử làm bài (4) | "Xem kết quả" (đã nộp) / "Tiếp tục" (đang làm) | Kết quả / Làm bài quiz | `paths.attemptResult(attemptId)` / `paths.takeQuiz(quizId)` |
| Lịch sử làm bài (4) | Empty → "Đến khóa học của tôi" | Khóa học của tôi | `paths.myCourses` |
| Thống kê khóa học (4) | "Xem chi tiết" ở một quiz | Thống kê quiz | `paths.quizStatistics(quizId)` |
| Thống kê quiz (4) | Quay lại | Thống kê khóa học | `paths.teachingStatistics` |
| AI & Flashcard (5) | "Sinh bản nháp" thành công | Xem trước kết quả AI | `paths.teachingLessonAiPreview(lessonId)` |
| Xem trước kết quả AI (5) | "Lưu thành bản nháp" — quiz | Soạn quiz (màn người 4) | `paths.quizEditor(quizId)` |
| Xem trước kết quả AI (5) | "Lưu thành bản nháp" — flashcard; "Hủy" | AI & Flashcard | `paths.teachingLessonAi(lessonId)` |

## Quy tắc

- Ẩn nút theo quyền chỉ phục vụ UI; backend vẫn kiểm tra quyền.
- Màn giảng viên xem trước câu hỏi khác màn học viên làm bài để không lộ đáp án.
- Không đưa AI key, JWT signing secret hay secret nào vào frontend (mọi biến `VITE_*` đều lộ ra trình duyệt).
- Chưa mặc định lưu refresh token trong localStorage; cách lưu token chốt cùng phần auth, gắn vào client bằng `setAccessTokenProvider`.
- File trong `src/shared/` dùng chung cả nhóm: sửa thì nêu trong PR.
