-- Extra rows for ENROLL-01 tests only, on top of test-fixtures.sql.
-- Kept out of the shared fixture so marketplace/course totals stay stable.
-- Written to be re-runnable: Spring applies class-level @Sql on both the base
-- and the subclass, so this script can execute more than once per test.
DELETE FROM course_members WHERE course_id IN (6, 7);
DELETE FROM courses WHERE id IN (6, 7);

INSERT INTO courses (id, teacher_id, category_id, title, description, price, is_published, archived_at) VALUES
    -- Published, free, joined by nobody: a fresh enrollment.
    (6, 2, 1, 'Khoa free chua ai hoc', 'Mo ta', 0.00, TRUE, NULL),
    -- Published but priced: enrolling must fail with COURSE_NOT_FREE.
    (7, 2, 2, 'Khoa tra phi', 'Mo ta', 150000.00, TRUE, NULL);
