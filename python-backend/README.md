# Backend Python — triển khai sau

Stack theo README: FastAPI, Pydantic, SQLAlchemy và PostgreSQL. Chưa có main.py, __init__.py, pyproject/requirements, dependency hay runtime. Thư mục là khung để giữ thiết kế tương ứng Java.

`app/modules/<module>/` có `api/` (router/Pydantic request/response), `application/` (services/ports), `domain/` (rules), `infrastructure/` (SQLAlchemy/repository/integration adapters). `app/shared/` dành config/auth/error/DB connection tối thiểu; `tests/` dành Python unit/integration.

Không triển khai Python song song giai đoạn Java nếu chưa có yêu cầu môn. Khi port, đọc `docs/05-delivery/python-migration.md`, dùng cùng contract/SQL migration/fixtures. Override validation/error FastAPI để cùng status/code Java. Domain không phụ thuộc FastAPI/SQLAlchemy; ORM model không phải response schema.

Chốt version và dependency manager ở giai đoạn Python; không tạo dependencies dự đoán từ hôm nay. Không tự dùng create_all/Alembic tạo schema lệch nguồn SQL chung.
