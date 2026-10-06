# Frontend dùng chung

Chưa chọn framework/package manager; chưa có package.json hay app. Sau chốt, cấu trúc đề xuất `src/features/` theo trải nghiệm: auth, marketplace, learning, teaching, admin; `src/shared/` cho API client, components, routing và assets. `tests/` giữ UI checks có ý nghĩa.

API client cấu hình base URL; response snake_case/ID string; không chứa business grading hoặc role approval. Quyền ẩn button chỉ phục vụ UI, backend vẫn kiểm tra. Teacher preview khác student question view để tránh lộ đáp án.

Các màn hình tại `docs/03-experience/screens-and-flows.md`; frontend có thể làm mock theo contract đã chốt trong khi backend đang triển khai. Chọn storage access/refresh tokens cùng security design; chưa mặc định lưu refresh token trong localStorage. Không đưa AI key hoặc JWT signing secret vào frontend.

Một frontend tương thích Java/Python bằng đổi cấu hình endpoint; không thêm nhánh payload theo framework.
