# Kế hoạch kiểm thử

`shared/acceptance-scenarios.md` là nguồn tình huống chung; `shared/fixtures/` dành cho fixture trung lập framework khi schema chốt. `contract/` dành cho checks dựa OpenAPI. Backend giữ unit/integration tests trong vùng riêng; frontend giữ UI checks trong frontend/tests.

Ưu tiên tests có giá trị: quyền theo object, validate AI, chấm điểm, transaction/submit đồng thời, ORM constraints và contract parity. Không viết tests chỉ kiểm tra thư mục/tài liệu tồn tại trong ứng dụng; bộ khung được kiểm tra cấu trúc/link một lần. Chưa có test runner hay kết quả kiểm thử ứng dụng.
