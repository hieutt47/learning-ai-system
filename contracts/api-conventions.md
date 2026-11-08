# Quy ước API

Đề xuất base `/api/v1`; JSON snake_case, UTF-8. Auth `Authorization: Bearer <access_token>`. Login/refresh trả JSON chỉ gồm access_token; refresh token truyền qua Set-Cookie với tên refreshToken, HttpOnly, Path=/api/v1/auth, SameSite=Lax và Secure khi dùng HTTPS. Cookie có Max-Age=604800 giây; hạn refresh session được tính lại thành thời điểm hiện tại + 7 ngày sau mỗi login/refresh thành công. Refresh session hết hạn bị từ chối 401, không được gia hạn. Refresh/logout nhận cookie refreshToken, không nhận token trong body và không yêu cầu access token; frontend không gửi header Authorization khi gọi hai endpoint này. Logout xóa cookie cùng tên/path bằng Max-Age=0 và trả 204 không body, kể cả khi thiếu cookie hoặc không tìm thấy session. Nếu client gửi kèm Bearer token hợp lệ, còn hạn của tài khoản bị khóa, refresh/logout trả 403 ACCOUNT_INACTIVE trước khi xử lý endpoint; logout trong trường hợp này không thu hồi session và không xóa cookie. Timestamp ISO 8601 UTC; UI đổi sang giờ địa phương. BIGINT trên wire là **string** để frontend không mất độ chính xác. price là decimal string, currency VND; score/completion_rate là number thang 0–100. Enum UPPER_CASE.

Response object trực tiếp, không envelope `data` thừa. Danh sách phân trang: `{items:[],page:1,page_size:20,total:0}`; page≥1, page_size 1–100. Danh sách con có giới hạn quy mô như options nằm trong object. Sort whitelist, mặc định ổn định theo created_at desc rồi id desc. PATCH cập nhật field được gửi; nullable ghi rõ. Riêng PATCH /categories/{category_id} chỉ cập nhật name/description khác null; field thiếu hoặc null giữ nguyên, không xóa bằng null. PUT question thay toàn bộ question và options, chỉ Draft.

201 tạo mới + Location, riêng POST /auth/register, POST /teacher-requests và POST /categories trả object vừa tạo trong body và không yêu cầu header Location; 200 đọc/update hoặc thao tác lặp trả dữ liệu cũ; 204 delete/logout; 400 lỗi payload/validation; 401 chưa xác thực; 403 thiếu quyền; 404 tài nguyên không tồn tại/không được thấy; 409 conflict trạng thái/unique; 413 quá dung lượng; 415 loại file; 422 nguồn text không dùng được; 429 giới hạn; 502 output/provider AI lỗi; 504 AI timeout. Không lộ stack trace.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu không hợp lệ",
  "details": [{"field": "email", "message": "Email không hợp lệ"}]
}
```

Mã baseline: VALIDATION_ERROR, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, EMAIL_ALREADY_EXISTS, ACCOUNT_INACTIVE, INVALID_STATE, COURSE_NOT_FREE, ENROLLMENT_REQUIRED, INVALID_QUIZ, INVALID_ANSWER, FILE_TOO_LARGE, UNSUPPORTED_FILE_TYPE, SOURCE_TEXT_EMPTY, AI_OUTPUT_INVALID, AI_PROVIDER_ERROR, AI_TIMEOUT, RATE_LIMITED, RESOURCE_IN_USE.

GET không gây thay đổi. Enroll/start/submit/logout retry có semantics ở business-rules; tạo nội dung và AI generation không tự động retry bằng UI vì có thể tạo trùng/tốn phí. Trước khi bổ sung auto retry các POST này cần idempotency key. Phân quyền theo object ở service; không chỉ chặn role tại URL.

File dùng multipart, download trả bytes với Content-Type/Content-Disposition; không dùng JSON cho bytes. Public response chỉ trả thumbnail URL, không đường dẫn file nội bộ. CORS allowlist cần chốt cùng frontend trước implementation.
