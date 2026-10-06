# Quy ước API

Đề xuất base `/api/v1`; JSON snake_case, UTF-8. Auth `Authorization: Bearer <access_token>`. Timestamp ISO 8601 UTC; UI đổi sang giờ địa phương. BIGINT trên wire là **string** để frontend không mất độ chính xác. price là decimal string, currency VND; score/completion_rate là number thang 0–100. Enum UPPER_CASE.

Response object trực tiếp, không envelope `data` thừa. Danh sách phân trang: `{items:[],page:1,page_size:20,total:0}`; page≥1, page_size 1–100. Danh sách con có giới hạn quy mô như options nằm trong object. Sort whitelist, mặc định ổn định theo created_at desc rồi id desc. PATCH cập nhật field được gửi; nullable ghi rõ. PUT question thay toàn bộ question và options, chỉ Draft.

201 tạo mới + Location; 200 đọc/update hoặc thao tác lặp trả dữ liệu cũ; 204 delete/logout; 400 lỗi payload/validation; 401 chưa xác thực; 403 thiếu quyền; 404 tài nguyên không tồn tại/không được thấy; 409 conflict trạng thái/unique; 413 quá dung lượng; 415 loại file; 422 nguồn text không dùng được; 429 giới hạn; 502 output/provider AI lỗi; 504 AI timeout. Không lộ stack trace.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu không hợp lệ",
  "details": [{"field": "email", "message": "Email không hợp lệ"}],
  "request_id": "req-demo-01"
}
```

Mã baseline: VALIDATION_ERROR, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, EMAIL_ALREADY_EXISTS, ACCOUNT_INACTIVE, INVALID_STATE, COURSE_NOT_FREE, ENROLLMENT_REQUIRED, INVALID_QUIZ, INVALID_ANSWER, FILE_TOO_LARGE, UNSUPPORTED_FILE_TYPE, SOURCE_TEXT_EMPTY, AI_OUTPUT_INVALID, AI_PROVIDER_ERROR, AI_TIMEOUT, RATE_LIMITED, RESOURCE_IN_USE.

GET không gây thay đổi. Enroll/start/submit/logout retry có semantics ở business-rules; tạo nội dung và AI generation không tự động retry bằng UI vì có thể tạo trùng/tốn phí. Trước khi bổ sung auto retry các POST này cần idempotency key. Phân quyền theo object ở service; không chỉ chặn role tại URL.

File dùng multipart, download trả bytes với Content-Type/Content-Disposition; không dùng JSON cho bytes. Public response chỉ trả thumbnail URL, không đường dẫn file nội bộ. CORS allowlist và nơi giữ token cần chốt cùng frontend trước implementation.
