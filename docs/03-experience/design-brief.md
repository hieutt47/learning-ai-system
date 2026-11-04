# Brief design system — Learning AI System

Bản brief để đưa vào Claude Design và thống nhất UI cho cả nhóm. Màu, font, kích thước dưới đây là **đề xuất**, nhóm chốt trước khi vẽ màn hình. Màn hình và luồng lấy từ [screens-and-flows.md](screens-and-flows.md), dữ liệu mẫu theo [payloads.md](../../contracts/payloads.md).

## 1. Bối cảnh sản phẩm

MOOC miễn phí có AI hỗ trợ giảng viên. Giảng viên tạo khóa học, bài học (video YouTube, transcript, PDF/TXT), quiz và flashcard. AI sinh **bản nháp** quiz/flashcard để giảng viên sửa và duyệt; không bao giờ tự xuất bản. Sinh viên ghi danh, học, làm quiz, xem kết quả và ôn flashcard.

- Ngôn ngữ giao diện: **tiếng Việt**.
- Thiết bị: desktop là chính (demo trên laptop). Tablet/mobile chỉ cần không vỡ layout.
- Frontend là SPA tách riêng, dùng chung cho backend Java và Python. Thiết kế chỉ phụ thuộc vào API contract.
- Ngoài phạm vi: thanh toán, chatbot, upload video, mobile app. Không vẽ nút "Mua khóa học".

## 2. Tính cách và nguyên tắc

**Tính cách:** sạch, thân thiện, tập trung vào học. Hiện đại nhưng không màu mè. Tham chiếu: Coursera, Notion, Linear. Ảnh mẫu đã chọn (hướng B): bố cục theo dashboard học tập Focotech, biểu đồ và stat card theo dashboard thống kê tông tím; không lấy màu cam và các yếu tố game hóa (điểm, streak, leaderboard, premium).

1. **Nội dung trước, trang trí sau.** Ưu tiên khoảng trắng và chữ dễ đọc, hạn chế gradient và hình minh họa nặng.
2. **Trạng thái luôn rõ ràng.** Draft, Published, Closed, AI-generated phải nhận ra ngay bằng badge, không chỉ dựa vào màu.
3. **AI là trợ lý, không phải tác giả.** Nội dung AI sinh luôn có nhãn "AI tạo", ở chế độ chỉnh sửa và cần giảng viên bấm duyệt.
4. **Mọi màn hình có 4 trạng thái:** loading, empty, error, forbidden.
5. **Phân tách vai trò:** giao diện giảng viên (công cụ, nhiều bảng/form) khác giao diện sinh viên (học tập, ít nhiễu).

## 3. Design tokens (đề xuất)

### Màu

| Token | Light | Dark | Dùng cho |
|---|---|---|---|
| `primary` | `#4F46E5` (indigo) | `#818CF8` | Nút chính, link, focus |
| `ai-accent` | `#7C3AED` (violet) | `#A78BFA` | Mọi thứ liên quan AI: nút "Sinh bằng AI", badge "AI tạo" |
| `success` | `#16A34A` | `#4ADE80` | Published, đáp án đúng, lưu thành công |
| `warning` | `#D97706` | `#FBBF24` | Draft, cảnh báo |
| `danger` | `#DC2626` | `#F87171` | Lỗi, đáp án sai, xóa, khóa tài khoản |
| `neutral-*` | Thang xám 50–900 | Đảo ngược | Nền, viền, chữ phụ, Closed/Archived |
| `bg` / `surface` | `#F5F6FA` / `#FFFFFF` | `#0B1120` / `#111827` | Nền trang / card |

Độ tương phản chữ trên nền tối thiểu 4.5:1 (WCAG AA).

### Chữ

- Font: **Inter** hoặc **Be Vietnam Pro** (bắt buộc hiển thị tốt tiếng Việt có dấu).
- Thang cỡ chữ: 12 / 14 (body) / 16 / 20 / 24 / 32. Line-height 1.5 cho body.
- Nội dung bài học, transcript, mặt flashcard: 16–18px để dễ đọc lâu.

### Khoảng cách, bo góc, đổ bóng

- Spacing theo bội số 4: 4, 8, 12, 16, 24, 32, 48.
- Bo góc: 8px (input, button), 16px (card), 999px (badge, avatar, ô tìm kiếm).
- Shadow nhẹ, chỉ 2 mức: card và modal/dropdown.

## 4. Component dùng chung

| Nhóm | Component | Biến thể và trạng thái bắt buộc |
|---|---|---|
| Hành động | Button | primary / secondary / ghost / danger / **ai**; size sm/md; loading (spinner + disable), disabled |
| Form | Input, Textarea, Select, Checkbox, Radio, File upload | default, focus, error (kèm message dưới field), disabled; upload có progress, lỗi `FILE_TOO_LARGE` / `UNSUPPORTED_FILE_TYPE` |
| Trạng thái | Badge | Draft, Published, Closed, Archived, AI tạo, Admin/Teacher/Student, Bị khóa |
| Hiển thị | Course card | thumbnail (có placeholder khi null), title, category, teacher, số học viên, "Miễn phí" |
| Hiển thị | Table | header, phân trang, sort, hàng rỗng, hàng đang tải (skeleton) |
| Hiển thị | Stat card, biểu đồ cột/đường đơn giản | Dùng cho thống kê course/quiz/system |
| Điều hướng | Header, Sidebar theo vai trò, Breadcrumb, Tabs, Pagination | active, hover |
| Phản hồi | Toast, Inline alert, Modal xác nhận | success / warning / error; modal xác nhận cho xóa, publish, khóa tài khoản |
| Trạng thái trang | Skeleton, Empty state, Error state, Forbidden (403), Not found (404) | Empty có lời giải thích + nút hành động tiếp theo |
| Học tập | Video player (YouTube embed), Document list, Quiz question, Flashcard | Xem mục 6 |

## 5. Layout theo vai trò

- **Mọi vai trò dùng sidebar trái** (thu gọn được) + header gồm tiêu đề/breadcrumb, ô tìm kiếm khóa học, avatar. Không có chuông thông báo hay tin nhắn.
  - Guest: Khám phá khóa học; nút Đăng nhập/Đăng ký ở cuối sidebar.
  - Student: Khám phá khóa học, Khóa học của tôi, Lịch sử làm bài, Hồ sơ.
  - Teacher: Khóa học giảng dạy, Thống kê, Hồ sơ.
  - Admin: Người dùng, Duyệt giảng viên, Danh mục, Khóa học, Thống kê hệ thống, Hồ sơ.
- Trang học bài thu gọn sidebar thành dải icon để dành chỗ cho video.
- **Trang soạn bài học (Teacher)** và **trang học bài (Student)** là hai trang dùng chung giữa nhiều người. Thiết kế dạng **tabs** để mỗi module gắn phần của mình:
  - Soạn bài học: `Nội dung` (video, transcript, tài liệu) · `Quiz` · `AI & Flashcard`
  - Học bài: video bên trái/trên, panel bên phải gồm `Tài liệu` · `Quiz` · `Flashcard`

## 6. Danh sách màn hình theo người phụ trách

| Người | Màn hình |
|---|---|
| 1. Tài khoản & quản trị | Đăng ký, Đăng nhập, Hồ sơ + gửi yêu cầu làm giảng viên, Admin: danh sách user (khóa/mở), duyệt yêu cầu Teacher (approve/reject + ghi chú), CRUD danh mục |
| 2. Khóa học & ghi danh | Marketplace (lọc danh mục, tìm kiếm), Chi tiết khóa (outline, nút Ghi danh), Khóa học của tôi, Teacher: dashboard khóa học, form khóa học + upload ảnh bìa + publish/archive |
| 3. Bài học & tài liệu | Teacher: danh sách bài (kéo thả thứ tự), soạn bài học (YouTube URL, transcript, upload PDF/TXT). Student: trang học bài (video + tài liệu), có link dự phòng khi video lỗi |
| 4. Quiz & kết quả | Teacher: soạn quiz (câu hỏi, 2–6 đáp án, chọn đúng 1 đáp án đúng, giải thích), publish/close, thống kê quiz. Student: làm bài, kết quả + xem lại đáp án, lịch sử làm bài |
| 5. AI & flashcard | Xem mục 7 |

Thống kê course/system: người 4 hoặc người được lead giao.

## 7. Chi tiết màn hình AI & Flashcard

### 7.1 Panel "Sinh bằng AI" (Teacher, trong tab AI & Flashcard)
- Chọn loại: Quiz / Flashcard.
- Chọn nguồn: Transcript / Tài liệu (dropdown danh sách tài liệu của bài). Nguồn rỗng thì disable kèm giải thích (`SOURCE_TEXT_EMPTY`).
- Số lượng 1–10. Độ khó Dễ / Trung bình / Khó (chỉ với Quiz).
- Nút `ai` "Sinh bản nháp". Khi đang chạy: progress + chữ "AI đang soạn… có thể mất tới 30 giây", không cho bấm lại.
- Lỗi: `AI_TIMEOUT`, `AI_PROVIDER_ERROR`, `AI_OUTPUT_INVALID`, `RATE_LIMITED` → alert rõ lý do + nút "Thử lại". Luôn có link "Soạn thủ công".

### 7.2 Xem trước candidate (chưa lưu)
- Banner tím: "Bản nháp do AI tạo — chưa được lưu. Hãy kiểm tra trước khi lưu."
- Quiz: danh sách câu hỏi, mỗi câu sửa được nội dung/đáp án/giải thích, đánh dấu đáp án đúng, xóa câu. Câu không hợp lệ (0 hoặc 2 đáp án đúng, thiếu nội dung) viền đỏ, chặn lưu.
- Flashcard: lưới card hai mặt (Mặt trước / Mặt sau) sửa trực tiếp, xóa từng card.
- Nút: "Lưu thành bản nháp" (primary), "Sinh lại" (secondary), "Hủy" (ghost). Lưu xong chuyển sang danh sách quiz/flashcard dạng Draft.
- Rời trang khi chưa lưu → modal cảnh báo mất dữ liệu.

### 7.3 Quản lý flashcard (Teacher)
- Bảng/lưới card: mặt trước, mặt sau (rút gọn), badge Draft/Published, badge "AI tạo", ngày tạo.
- Sửa/xóa chỉ khi Draft. Publish từng card hoặc chọn nhiều để publish. Modal xác nhận publish.
- Empty state: "Chưa có flashcard" + nút "Tạo thủ công" và "Sinh bằng AI".

### 7.4 Học flashcard (Student)
- Một card lớn ở giữa, bấm hoặc nhấn Space để lật (hiệu ứng lật 3D nhẹ). Nút Trước/Sau, phím ←/→, chỉ số "3 / 12".
- Chỉ hiện card Published. Empty state: "Giảng viên chưa xuất bản flashcard cho bài này".
- Không có spaced repetition trong MVP.

## 8. Trạng thái lỗi chung

| Mã / HTTP | Cách hiển thị |
|---|---|
| 401 `UNAUTHENTICATED` | Chuyển về Đăng nhập, giữ trang đích để quay lại |
| 403 `FORBIDDEN`, `ENROLLMENT_REQUIRED` | Trang/alert "Bạn không có quyền" + hành động gợi ý (ví dụ nút Ghi danh) |
| 404 | Trang "Không tìm thấy" + về trang chủ |
| 409 `INVALID_STATE`, `RESOURCE_IN_USE` | Alert giải thích xung đột, tải lại dữ liệu |
| 400 `VALIDATION_ERROR` | Lỗi dưới từng field, giữ nguyên dữ liệu đã nhập |
| Lỗi mạng | Toast + nút thử lại; quiz đang làm không mất câu trả lời |

## 9. Dữ liệu mẫu cho bản thiết kế

Dùng dữ liệu thật thay cho lorem ipsum:

- Khóa học: "Lập trình Java cơ bản" (danh mục Lập trình, GV Nguyễn Văn An, 128 học viên, Miễn phí), "Cấu trúc dữ liệu và giải thuật", "Nhập môn Cơ sở dữ liệu".
- Bài học: "Bài 1 — JVM và bytecode", "Bài 2 — Biến và kiểu dữ liệu".
- Câu hỏi: "JVM có vai trò gì?" — Chạy bytecode Java ✓ / Lưu video / Biên dịch HTML / Quản lý CSDL.
- Flashcard: "JVM là gì?" → "Máy ảo thực thi bytecode Java."
- Kết quả: 8/10 câu đúng, 80 điểm, nộp lúc 09:05 06/10/2026.

## 10. Prompt mẫu cho Claude Design

Bộ prompt đầy đủ 8 lượt theo phong cách đã chọn nằm ở [claude-design-prompts.md](claude-design-prompts.md); đoạn dưới chỉ là bản tóm tắt.

> Thiết kế design system và giao diện web cho "Learning AI System", một nền tảng MOOC miễn phí tiếng Việt có AI hỗ trợ giảng viên soạn quiz và flashcard. Phong cách sạch, thân thiện, tập trung vào học, tham chiếu Coursera và Notion. Màu chính indigo `#4F46E5`, màu riêng cho tính năng AI là violet `#7C3AED`, font Be Vietnam Pro, có light và dark mode. Bắt đầu bằng design system (tokens, button, form, badge Draft/Published/AI tạo, table, card, toast, modal, empty/error/loading state). Sau đó thiết kế: [tên màn hình]. Dùng dữ liệu mẫu tiếng Việt [dán mục 9]. Mỗi màn hình cần có trạng thái loading, rỗng và lỗi.

Đính kèm ảnh tham khảo đã chọn (moodboard) khi gửi prompt.
