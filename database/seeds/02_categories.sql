-- Dev/demo categories. Not a Flyway migration: apply manually after V1 and 01_users.
--   mysql -u <user> -p learningai < database/seeds/02_categories.sql
--
-- Users are seeded by 01_users.sql; these rows exist because courses.category_id
-- is NOT NULL, so a fresh database cannot create a course without one.
-- Idempotent via INSERT IGNORE on the unique name, so re-running is safe.
-- Names are ASCII on purpose: the import must not depend on the client charset.

INSERT IGNORE INTO categories (name, description) VALUES
    ('Lap trinh',        'Cac khoa hoc lap trinh, cau truc du lieu va giai thuat'),
    ('Co so du lieu',    'Mo hinh du lieu, SQL, thiet ke va toi uu truy van'),
    ('Tri tue nhan tao', 'Hoc may, xu ly ngon ngu tu nhien va ung dung AI');
