# Môi trường và vận hành — kế hoạch

Chưa có Docker/Compose, CI YAML, deployment hay cloud resource. Tạo cấu hình thực sau chốt toolchain và nhu cầu chạy nhóm; không cần hosting để hoàn thiện khung.

| Cấu hình dự kiến | Ý nghĩa | Secret |
|---|---|---|
| APP_ENV, PORT | Profile/port backend | Không |
| DATABASE_HOST/PORT/NAME/USER/PASSWORD | PostgreSQL; map theo từng framework | Password |
| JWT_SIGNING_KEY, ACCESS_TTL, REFRESH_TTL | Auth/session config | Signing key |
| FRONTEND_ORIGIN, API_BASE_URL | CORS allowlist/backend endpoint | Không |
| STORAGE_ROOT, MAX_DOCUMENT_BYTES, MAX_IMAGE_BYTES | File adapter và giới hạn | Không |
| AI_PROVIDER, AI_MODEL, AI_API_KEY, AI_TIMEOUT_SECONDS | Chọn mock/live provider | API key |

Chưa tạo .env.example vì key thực phụ thuộc cách bootstrap; khi tạo chỉ placeholder, không secret thật. Secret để .env hoặc secret manager, file env bị gitignore. Logs có request_id, không token/password/full transcript. DB user chỉ có quyền cần; file tải qua endpoint có authorization.

Dev/test/demo có DB riêng. Backup trước migration dữ liệu nhóm; dữ liệu demo reset có chủ đích. Health/readiness và run commands bổ sung khi app có thật. Cấu hình migration runner duy nhất trước startup.

CI tương lai: validate contract → build/lint phù hợp stack → relevant checks/DB integration → smoke luồng demo. Java làm trước; Python chỉ bật khi có implementation. Không để CI chạy một backend chưa tồn tại.
