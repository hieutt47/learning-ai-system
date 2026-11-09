-- Deterministic fixture for integration tests. Re-created before every test.
-- Fixed ids keep assertions readable; FK order matters.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE ai_generations;
TRUNCATE TABLE quizzes;
TRUNCATE TABLE lesson_documents;
TRUNCATE TABLE lessons;
TRUNCATE TABLE course_members;
TRUNCATE TABLE courses;
TRUNCATE TABLE categories;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- Password for every account below: DemoPass123!
-- bcrypt(10) hashes are real so tests can log in through the actual auth route
-- instead of faking identity with a header.
INSERT INTO users (id, full_name, email, password_hash, role, is_active) VALUES
    (1, 'Admin',     'admin@test.local',     '$2a$10$VSsXvE5oyd2y1eYkTkf7TuWlVAaCU3JvgI6L9BF4L9zCtLjY1vPmC', 'ADMIN',   TRUE),
    (2, 'Teacher A', 'teacher.a@test.local', '$2a$10$nM6SRz8UPgRoNNkXjcaZ/eZ5ymLM8BezDhIZD3S88I/J7mZBR/Cia', 'TEACHER', TRUE),
    (3, 'Teacher B', 'teacher.b@test.local', '$2a$10$vW1KcuZGpeo63Ipw3tia0.4.aANxYyGwEwmaR/Y6LUlE/dVLD2x6y', 'TEACHER', TRUE),
    (4, 'Student A', 'student.a@test.local', '$2a$10$349DXKNbx4raDvwQXg//qODNCGicEEa17FWiP6em5/A/Q0RGuhTpi', 'STUDENT', TRUE),
    (5, 'Inactive',  'inactive@test.local',  '$2a$10$349DXKNbx4raDvwQXg//qODNCGicEEa17FWiP6em5/A/Q0RGuhTpi', 'TEACHER', FALSE),
    (6, 'Student B', 'student.b@test.local', '$2a$10$349DXKNbx4raDvwQXg//qODNCGicEEa17FWiP6em5/A/Q0RGuhTpi', 'STUDENT', TRUE);

INSERT INTO categories (id, name, description) VALUES
    (1, 'Lap trinh', 'Lap trinh co ban'),
    (2, 'Co so du lieu', 'SQL va thiet ke');

INSERT INTO courses (id, teacher_id, category_id, title, description, price, is_published, archived_at) VALUES
    (1, 2, 1, 'Khoa draft cua A',   'Mo ta', 0.00, FALSE, NULL),
    (2, 2, 1, 'Khoa publish cua A', 'Mo ta', 0.00, TRUE,  NULL),
    (3, 3, 2, 'Khoa cua B',         'Mo ta', 100000.00, FALSE, NULL),
    (4, 3, 2, 'Khoa publish cua B', 'Mo ta SQL', 200000.00, TRUE, NULL),
    -- Archived while still marked published: the marketplace must hide it, so
    -- "published" alone is not a sufficient filter.
    (5, 3, 1, 'Khoa da archive',    'Mo ta', 0.00, TRUE, CURRENT_TIMESTAMP(6));

INSERT INTO lessons (id, course_id, title, video_url, transcript_text, order_index) VALUES
    (1, 2, 'Bai 1: Gioi thieu', 'https://youtu.be/secret-a1', 'transcript bi mat 1', 1),
    (2, 2, 'Bai 2: Cai dat',    'https://youtu.be/secret-a2', 'transcript bi mat 2', 2),
    (3, 4, 'Bai 1: SELECT',     'https://youtu.be/secret-b1', NULL, 1);

INSERT INTO course_members (course_id, student_id) VALUES
    (2, 4),
    (4, 4);
