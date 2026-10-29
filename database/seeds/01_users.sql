
-- Password: DemoPass123!



INSERT INTO users (full_name, email, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('Demo Admin', 'admin.demo@example.test',
     '$2a$10$VSsXvE5oyd2y1eYkTkf7TuWlVAaCU3JvgI6L9BF4L9zCtLjY1vPmC',
     'ADMIN', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Demo Teacher A', 'teacher.a@example.test',
     '$2a$10$nM6SRz8UPgRoNNkXjcaZ/eZ5ymLM8BezDhIZD3S88I/J7mZBR/Cia',
     'TEACHER', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Demo Teacher B', 'teacher.b@example.test',
     '$2a$10$vW1KcuZGpeo63Ipw3tia0.4.aANxYyGwEwmaR/Y6LUlE/dVLD2x6y',
     'TEACHER', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Demo Student A', 'student.a@example.test',
     '$2a$10$349DXKNbx4raDvwQXg//qODNCGicEEa17FWiP6em5/A/Q0RGuhTpi',
     'STUDENT', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Demo Student B', 'student.b@example.test',
     '$2a$10$lMriEJ9uXRnM.fu23pLqmeEM6RXVwMgrjde4Im7ePMkxVVzSzaOYa',
     'STUDENT', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Demo Disabled Student', 'student.disabled@example.test',
     '$2a$10$bFg0Uq5.UygBCKwsScucYucZDUyw0V9REwur9S2nCBC0xhUrzuMqa',
     'STUDENT', FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))

