# Hợp đồng candidate AI

Nguồn phải thuộc lesson, quyền owner/admin. source_type chọn TRANSCRIPT hoặc DOCUMENT; document_id bắt buộc chỉ với DOCUMENT. Không tự lấy transcript từ YouTube. Quiz count 1–10, difficulty EASY/MEDIUM/HARD; card count 1–10, không cần difficulty.

Candidate quiz ví dụ (provider chưa biết ID DB):

```json
{
  "questions": [{
    "content": "JVM có vai trò gì?",
    "options": [{"content":"Chạy bytecode Java","is_correct":true},{"content":"Lưu video","is_correct":false}],
    "explanation": "JVM thực thi bytecode."
  }]
}
```

Candidate flashcard:

```json
{"flashcards":[{"front_text":"JVM là gì?","back_text":"Máy ảo thực thi bytecode Java."}]}
```

API trả wrapper `{generation_id, lesson_id, candidate}`; server ghi provenance. Provider response phải đúng object/type/required fields, không null hay content rỗng, đúng count, không trùng câu sau normalize, không quá length/option giới hạn; đúng một is_correct=true/câu. Từ chối field lạ và output markdown không parse được; không silently lưu bản lỗi. Structured output nếu provider hỗ trợ là adapter detail, không thay contract.

LLM call ngoài transaction; text length/token budget hữu hạn; timeout cấu hình; retry thủ công MVP. Không trả stack trace/prompt secret. Mock adapter cung cấp candidate cùng cấu trúc để demo offline. Tài liệu nguồn gửi provider cần được nhóm chấp thuận trước tích hợp thật.

Generation record: type, lesson/source, requested_count, difficulty nullable, provider/model, prompt_version, source_hash, status, error_code, created_by, created_at. Candidate thành công lưu có giới hạn để server xác minh provenance; không lưu token/API key. Giới hạn retention candidate theo môi trường.

Teacher sửa candidate trong UI; tạo Quiz Draft và các Question Draft hoặc Card Draft qua API thường. generation_id phải của owner/lesson, type phù hợp, status SUCCEEDED. generated_by_ai do server suy ra khi generation_id hợp lệ, client không tự đặt. Publish sau một thao tác riêng có validate.
