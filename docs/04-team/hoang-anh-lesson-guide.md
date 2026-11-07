# Hoàng Anh — hướng dẫn phần bài học và tài liệu

## Nguồn thống nhất

Theo chỉ dẫn của Hoàng Anh, lấy `Pha thiết kế.pdf` làm chuẩn khi khác tài liệu
khung cũ. File hiện có ở `/media/icnlab/Data/hoanganh/Pha thiết kế.pdf`, 79 trang.
Số trang dưới đây là số trang hiển thị trong PDF. File PDF không được chỉnh sửa
hoặc chép thêm vào repo trong bước này.

“Người 3” trong phân công thực hiện của Hoàng Anh là bài học/tài liệu và hai trang
soạn bài/học bài. Không đồng nhất với “M3” trong PDF, vốn là làm và nộp quiz.

| Nội dung | Thiết kế nhóm | Nơi đối chiếu |
|---|---|---|
| Backend | Spring Boot, Java 17 | Trang 76, mục 3.5 |
| CSDL | MySQL 8, JDBC/JPA | Trang 61 và 76 |
| Package | `vn.edu.learningai.controller`, `dto.request`, `dto.response`, `mapper`, `service`, `repository`, `entity`, `common`, `security`, `exception`, `config` | Trang 74–75, Hình 3.19 |
| Lesson | `id`, `title`, `videoUrl`, `transcriptText`, `orderIndex`, `createdAt`, `updatedAt`; quan hệ `Course`, `List<Quiz>`, `List<Flashcard>` | Trang 60–61, Bảng 3.1, Hình 3.1 |
| Quyền soạn | `LessonService.assertCanManageLesson(Long lessonId, Long teacherId): Lesson` | Trang 65, 70, Hình 3.10 |
| Nguồn AI | `LessonSourceTextProvider.getSourceText(Long lessonId, SourceType type, Long documentId): String` | Trang 65, Hình 3.10 |
| Enum nguồn | `SourceType.TRANSCRIPT`, `SourceType.DOCUMENT` | Trang 58–59 |
| Text rỗng | `SOURCE_TEXT_EMPTY` | Trang 65, 73 |
| Giới hạn nguồn gửi AI | Tối đa 20.000 ký tự, tại `AiGenerationService` | Trang 64–65 |
| Tách nguồn và phân quyền | AI gọi `assertCanManageLesson` trước `getSourceText` | Trang 70, bước 7–14 |
| Tầng giao diện | `frontend/src/features/teaching`, `features/learning`, dùng `shared` | Trang 75 |

## Cập nhật GitHub ngày 06/11/2026

Đã fetch cả hai remote, đối chiếu branch và PR trực tiếp trên GitHub:

- `origin`: fork của Hoàng Anh, `https://github.com/hoanganh1601/learning-ai-system.git`.
- `upstream`: repo nhóm, `https://github.com/hieutt47/learning-ai-system.git`.
- `origin/main` và `upstream/main`: `83e68f3`, vẫn ở bản khung ban đầu.
- `upstream/develop`: `2cf6ac1`, có backend Maven/Spring Boot, migration MySQL,
  frontend React + TypeScript + Vite và prototype. Đây là nền tạo nhánh theo
  `frontend/README.md`; không tạo lại ứng dụng từ đầu.
- PR #4 frontend đã merge vào `develop`; PR #3 AI/Flashcard còn mở, base `develop`.
- Các nhánh riêng chưa tích hợp vào develop: `feat/ai-flashcard` (`7554664`),
  `feat/giai-doan-1-persistence` (`c0df269`),
  `feat/java-backend-authentication` (`b80ab0f`), `feat/quiz-authoring` (`8ee33f2`).

Nhánh local `feat/lesson-documents` đã fast-forward từ bản khung lên `2cf6ac1`.
Các file bước đầu vẫn được giữ, chưa commit/push. Không merge toàn bộ các nhánh
cá nhân chưa được tích hợp. Sau này PR phần Lesson hướng về `hieutt47:develop`.

Ba hợp đồng nhỏ bên dưới lấy nguyên nội dung từ nhánh AI tại `7554664`, gồm cả
package `common.enums`, để tương thích người gọi và tránh thêm hai enum khác nhau.
Đây là dùng lại hợp đồng nội bộ; chưa đưa chức năng AI hoặc stub sang nhánh Lesson.

## Phạm vi của Hoàng Anh

- Bài học: tạo, đọc, cập nhật, xóa có điều kiện; thứ tự bài trong khóa học.
- Video: kiểm tra và chuẩn hóa YouTube URL; không nhận HTML iframe.
- Transcript: giáo viên nhập và lưu `transcriptText`.
- Tài liệu: upload/download PDF/TXT và trích xuất nguồn text cho AI.
- Khung trang Teacher: thêm/soạn bài, danh sách bài; vị trí tích hợp Quiz và AI/Flashcard.
- Khung trang Student: video, tài liệu, vị trí tích hợp Quiz và Flashcard.

Không triển khai thay phần đăng nhập, tạo/xuất bản khóa học, ghi danh, chấm quiz
hoặc sinh AI. Tái sử dụng đối tượng và dịch vụ của các thành viên phụ trách.

## Bước 1 đã chuẩn bị: hợp đồng Java

Ba file hợp đồng dùng chung, khớp bản người làm AI đã triển khai:

```text
java-backend/src/main/java/vn/edu/learningai/
  common/enums/SourceType.java
  service/LessonAccessPolicy.java
  service/LessonSourceTextProvider.java
```

Chưa có implementation/controller/entity của phần Lesson. Nền backend/frontend
chung đã có trên develop; chưa xác nhận toàn bộ ứng dụng chạy được trên máy này.
Interface không tự kiểm tra quyền hoặc truy vấn DB; cần cài đặt ở bước tiếp theo.

### Làm bằng VS Code trên máy này

1. Mở thư mục `/media/icnlab/Data/hoanganh/learning-ai-system` bằng File → Open Folder.
2. Kiểm tra góc trái dưới đang là nhánh `feat/lesson-documents`.
3. Cài **Extension Pack for Java** của Microsoft (`vscjava.vscode-java-pack`) và
   **Spring Boot Extension Pack** của VMware (`vmware.vscode-boot-dev-pack`). Nếu
   dùng Remote SSH, cài tại SSH host đang chứa repo. Chưa tự cài extension trong bước này.
4. Để Java extension nhập project từ `java-backend/pom.xml`; không tạo Maven project mới.
5. Dùng Command Palette → `Java: Configure Java Runtime` để cấu hình runtime dự án
   theo Java 17. Máy hiện có JDK 21 trên PATH; chưa thấy JDK 17 trong `/usr/lib/jvm`.
6. Terminal frontend chạy trong `frontend`; backend dùng Maven Wrapper trong `java-backend`.

Tài liệu VS Code: https://code.visualstudio.com/docs/java/extensions và
https://code.visualstudio.com/docs/java/java-project.

Biên dịch bằng `javac --release 17` chỉ xác nhận tương thích mức ngôn ngữ/API Java 17,
không thay thế việc chạy ứng dụng và kiểm thử trên JDK 17.

Điểm OOP: module AI phụ thuộc interface thay vì biết cách truy vấn transcript/tài
liệu. Implementation của phần Lesson có thể thay đổi mà không đổi người gọi.

## Bước 2: nền tảng chung và đối tượng phụ thuộc

Trước khi viết `LessonService`, ghép nền tảng dùng chung:

1. Dùng `java-backend/pom.xml` và `mvnw` đã có (Java 17, Spring Boot 4.1.1).
   Cấu hình DB theo các biến `DBMS_CONNECTION`, `DBMS_USERNAME`, `DBMS_PASSWORD`;
   không commit thông tin đăng nhập. Không cần cài Maven hệ thống nếu dùng Wrapper.
2. Các lớp `User`, `Course`, `CourseMember` từ người phụ trách; không tự tạo bản sao.
3. Danh tính người dùng đã xác thực, kiểm tra tài khoản active, xử lý lỗi chung.
4. Xác nhận tên dịch vụ kiểm tra course owner/enrollment nếu nhóm đã cài đặt.

PDF không ghi phiên bản Spring Boot hoặc framework frontend; code develop hiện đã
chốt Maven Wrapper và React + TypeScript + Vite (D06). Migration MySQL
`database/migrations/V1__initial_schema.sql` đã có cả `lessons` và `lesson_documents`.
Không tạo lại V1 hoặc đổi migration đã dùng; nếu cần thay schema, thêm migration mới.

## Bước 3: một luồng thêm bài học hoàn chỉnh

Theo UC-02 trang 26–28 và mục 3.3.2 trang 62:

```text
LessonAddPage
  → POST /api/v1/courses/{courseId}/lessons
  → LessonController
  → LessonService
  → LessonRepository
  → Lesson / bảng lessons
```

Đầu vào: tiêu đề, link video, nội dung bài giảng, thứ tự. Course và danh tính giáo
viên lấy từ đường dẫn/người đã xác thực; không nhận teacherId tùy ý từ form.

Kiểm tra quyền khóa học trước khi ghi. Bài thuộc đúng khóa; tiêu đề hợp lệ;
video hợp lệ; thứ tự không trùng trong khóa. Luồng thêm bài trong PDF thực hiện
trên khóa nháp (trang 21); không tự mở rộng quy tắc sửa cấu trúc khóa đã xuất bản
khi chưa đối chiếu dịch vụ khóa học chung.

Entity dùng thuộc tính private, quan hệ JPA theo thiết kế; DTO tách khỏi entity;
controller nhận/trả DTO, service điều phối nghiệp vụ và transaction; repository truy vấn.
Không đưa `List<Quiz>`/`List<Flashcard>` vào entity trước khi ghép được các lớp thật
của người phụ trách; không tạo entity quiz/card giả để qua biên dịch.

Nghiệm thu: owner thêm thành công; người khác bị từ chối; dữ liệu sai không ghi;
danh sách trả đúng thứ tự; lỗi lưu không báo thành công.

## Bước 4: quyền xem và nguồn AI

Cài đặt đúng hàm PDF trước:

```java
Lesson assertCanManageLesson(Long lessonId, Long teacherId);
```

Hàm tìm bài và khóa chứa bài, kiểm tra chủ sở hữu (và quyền admin theo chính sách
security chung), không chỉ kiểm tra role TEACHER. Khi gọi từ REST, teacherId lấy từ
người dùng đã xác thực. Mở rộng admin không được khiến mọi teacher đều có quyền.

Nhánh AI đã định nghĩa hợp đồng gọi chung:

```java
void LessonAccessPolicy.assertCanManageLesson(Long lessonId, Long userId);
LessonViewAccess LessonAccessPolicy.assertCanViewLesson(Long lessonId, Long userId);
```

`LessonViewAccess` là `MANAGER` hoặc `ENROLLED_STUDENT`. Dùng adapter để nối
`LessonAccessPolicy` sang `LessonService`: hàm service trả `Lesson` theo PDF,
hàm manage của policy trả `void` theo hợp đồng AI. Không ép một class cài cả hai
chữ ký khác kiểu trả về; không đổi interface khiến code AI bị hỏng.
`ApiException` đã có trên develop. Các mã lỗi bổ sung như `SOURCE_TEXT_EMPTY` đang
ở PR AI; khi tích hợp phải dùng enum lỗi chung, không tạo hệ exception thứ hai.

Cài đặt `LessonSourceTextProvider`:

- TRANSCRIPT: đọc `lessons.transcript_text`.
- DOCUMENT: bắt buộc có documentId; truy vấn theo cả documentId và lessonId.
- Không có nguồn phù hợp: dùng lỗi không tìm thấy của nhóm; text null/rỗng/chỉ
  khoảng trắng: `SOURCE_TEXT_EMPTY` (HTTP 422 theo API conventions hiện có).
- Không tự trích xuất lại mỗi lần AI gọi; không sửa hoặc cắt text đã lưu.
- Người gọi phải kiểm tra quyền bằng service trước; không tạo endpoint public
  trả raw transcript/extracted text.

Nghiệm thu quan trọng: nguồn từ bài khác bị chặn; text rỗng không gọi AI;
student không truy cập nguồn AI chỉ bằng cách đổi ID.

## Bước 5: upload và trích xuất tài liệu

PDF định nghĩa nguồn DOCUMENT nhưng chưa đặc tả đầy đủ `LessonDocument`, giới
hạn upload hoặc luồng trích xuất. Dùng các contract/tài liệu file hiện có làm đề xuất
bổ sung; không gọi chúng là quyết định đã chốt trong PDF.

Các điểm cần cụ thể hóa trước implementation:

- Map entity vào bảng `lesson_documents` đã có; dùng FK và tên cột của migration.
  `extracted_text` hiện là MEDIUMTEXT, `transcript_text` là TEXT; không coi là vô hạn.
- PDF có text/TXT tối đa 10 MiB theo frontend README mới; chốt encoding TXT,
  giới hạn tài nguyên extractor và giới hạn text phù hợp kiểu cột.
- Trích ngay lúc upload hay bất đồng bộ; nếu trích ngay, chỉ báo thành công khi
  file và metadata/text đã được xử lý thành công.
- Hành vi với PDF scan, PDF hỏng/mã hóa và text trống.
- Lưu file ngoài transaction DB; dọn file nếu lưu DB thất bại.
- Download qua API kiểm tra quyền, không trả đường dẫn storage nội bộ.

Đa hình khi cài đặt: interface trích text và hai implementation PDF/TXT;
interface storage với implementation local. Không thêm implementation giả không dùng.

## Bước 6: hai trang và vị trí ghép component

Theo phân công của Hoàng Anh và hình giao diện trang 63:

- Teacher: Nội dung/Tài liệu, Quiz, AI / Flashcard; giữ lessonId để truyền component.
- Student: Bài học/Tài liệu, Quiz, Flashcard; không hiện chức năng sinh AI hoặc nháp.
- Student chỉ vào bài của khóa đã ghi danh; backend vẫn kiểm tra quyền dù UI ẩn nút.
- Có loading/empty/error/forbidden; giữ form khi validation lỗi.

Theo `frontend/README.md`, chỉ làm ba màn của Người 3, bám prototype đã có:

| Màn | Route | Prototype |
|---|---|---|
| Khóa học · Bài học | `/teaching/courses/:courseId/lessons` | `#tlessons` |
| Soạn bài học · Nội dung | `/teaching/lessons/:lessonId` | `#tlesson` |
| Học bài · Tài liệu | `/learn/:courseId/lessons/:lessonId` | `#learn` |

Trang học phải dùng layout route cha có `<Outlet />` để Quiz/Flashcard của người
khác hiển thị bên trong. Tabs đã có route con; không tạo bộ tabs cạnh tranh.
Dùng React Router `paths`, TanStack Query, API client, Ant Design và PageState
chung; không làm lại layout hoặc thay placeholder của người khác.

## Checklist nghiệp vụ để bảo vệ

1. Teacher B không sửa/upload vào bài của Teacher A.
2. Student chưa ghi danh không đọc video/tài liệu bằng URL trực tiếp.
3. Tài liệu truyền cho AI phải thuộc đúng bài đang thao tác.
4. Không có text: SOURCE_TEXT_EMPTY, không sinh nội dung bịa từ nguồn rỗng.
5. Trùng thứ tự/reorder: transaction và ràng buộc DB bảo vệ nhất quán.
6. Xóa bài không làm mất quiz, kết quả hoặc lịch sử; dùng chính sách xóa của nhóm.
7. Không lộ entity graph, storage key hoặc đáp án quiz trên màn hình học.
8. Tích hợp lỗi: AI không khả dụng không được làm mất nội dung bài đã lưu.

Hiện mới hoàn thành bước 1. Các mục sau là trình tự học và triển khai, chưa phải
chức năng đã có hoặc kết quả đã kiểm thử.
