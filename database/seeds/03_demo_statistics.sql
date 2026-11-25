-- Dữ liệu mẫu để test toàn bộ luồng Thống kê giảng viên (và bài học/quiz) trên DB learningai.
-- An toàn khi chạy lại (idempotent theo dải ID 101..5012).
-- Tài khoản demo dùng chung mật khẩu: DemoPass123!

USE learningai;

-- 1. Bổ sung thêm 2 sinh viên demo (giữ nguyên các tài khoản từ 01_users.sql)
INSERT IGNORE INTO users (full_name, email, password_hash, role, is_active, email_verified_at, created_at, updated_at)
VALUES
    ('Demo Teacher A', 'teacher.a@example.test',
     '$2a$10$nM6SRz8UPgRoNNkXjcaZ/eZ5ymLM8BezDhIZD3S88I/J7mZBR/Cia',
     'TEACHER', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Demo Student A', 'student.a@example.test',
     '$2a$10$349DXKNbx4raDvwQXg//qODNCGicEEa17FWiP6em5/A/Q0RGuhTpi',
     'STUDENT', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Demo Student B', 'student.b@example.test',
     '$2a$10$lMriEJ9uXRnM.fu23pLqmeEM6RXVwMgrjde4Im7ePMkxVVzSzaOYa',
     'STUDENT', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Nguyễn Minh Anh', 'student.c@example.test',
     '$2a$10$349DXKNbx4raDvwQXg//qODNCGicEEa17FWiP6em5/A/Q0RGuhTpi',
     'STUDENT', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Trần Quốc Bảo', 'student.d@example.test',
     '$2a$10$lMriEJ9uXRnM.fu23pLqmeEM6RXVwMgrjde4Im7ePMkxVVzSzaOYa',
     'STUDENT', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT IGNORE INTO categories (name, description) VALUES
    ('Lap trinh',        'Cac khoa hoc lap trinh, cau truc du lieu va giai thuat'),
    ('Co so du lieu',    'Mo hinh du lieu, SQL, thiet ke va toi uu truy van'),
    ('Tri tue nhan tao', 'Hoc may, xu ly ngon ngu tu nhien va ung dung AI');

SET @teacher_a = (SELECT id FROM users WHERE email = 'teacher.a@example.test' LIMIT 1);
SET @student_a = (SELECT id FROM users WHERE email = 'student.a@example.test' LIMIT 1);
SET @student_b = (SELECT id FROM users WHERE email = 'student.b@example.test' LIMIT 1);
SET @student_c = (SELECT id FROM users WHERE email = 'student.c@example.test' LIMIT 1);
SET @student_d = (SELECT id FROM users WHERE email = 'student.d@example.test' LIMIT 1);

SET @cat_java = (SELECT id FROM categories WHERE name = 'Lap trinh' LIMIT 1);
SET @cat_db   = (SELECT id FROM categories WHERE name = 'Co so du lieu' LIMIT 1);
SET @cat_ai   = (SELECT id FROM categories WHERE name = 'Tri tue nhan tao' LIMIT 1);

-- Dọn lại đúng nhóm bản ghi demo nếu chạy lại script
DELETE FROM student_answers WHERE attempt_id BETWEEN 5001 AND 5020;
DELETE FROM quiz_attempts WHERE id BETWEEN 5001 AND 5020;
DELETE FROM question_options WHERE id BETWEEN 4001 AND 4050;
DELETE FROM questions WHERE id BETWEEN 3001 AND 3020;
DELETE FROM quizzes WHERE id BETWEEN 2001 AND 2010;
DELETE FROM lessons WHERE id BETWEEN 1001 AND 1010;
DELETE FROM course_members WHERE course_id IN (101, 102, 103);
DELETE FROM courses WHERE id IN (101, 102, 103);

-- 2. Tạo 3 khóa học cho Demo Teacher A để test đầy đủ 3 tình huống thống kê
INSERT INTO courses (id, teacher_id, category_id, title, description, price, is_published, archived_at)
VALUES
    (101, @teacher_a, @cat_java,
     'Lập trình Java & OOP thực chiến',
     'Khóa học miễn phí về JVM, đóng gói, kế thừa, đa hình và kiểm thử nghiệp vụ bằng quiz.',
     0.00, TRUE, NULL),
    (102, @teacher_a, @cat_db,
     'Thiết kế Cơ sở dữ liệu MySQL',
     'Khóa đã xuất bản có học viên và quiz nhưng chưa có lượt nộp (test trạng thái 0%).',
     0.00, TRUE, NULL),
    (103, @teacher_a, @cat_ai,
     'Nhập môn Trí tuệ nhân tạo (Bản nháp)',
     'Khóa bản nháp chưa có học viên ghi danh và chưa có quiz.',
     0.00, FALSE, NULL);

-- 3. Ghi danh sinh viên
INSERT INTO course_members (course_id, student_id, enrolled_at)
VALUES
    (101, @student_a, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 7 DAY)),
    (101, @student_b, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY)),
    (101, @student_c, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY)),
    (101, @student_d, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY)),
    (102, @student_a, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY)),
    (102, @student_b, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY));

-- 4. Bài học (gồm PUBLISHED, DRAFT và ARCHIVED)
INSERT INTO lessons (id, course_id, title, video_url, transcript_text, order_index, status)
VALUES
    (1001, 101, 'JVM, JDK và kiến trúc bộ nhớ',
     'https://www.youtube.com/watch?v=eIrMbAQSU34',
     'JVM thực thi bytecode Java trên nhiều nền tảng. Bộ nhớ gồm Heap lưu object và Stack lưu frame lời gọi hàm.',
     1, 'PUBLISHED'),
    (1002, 101, 'Đóng gói, Kế thừa và Đa hình',
     'https://www.youtube.com/watch?v=pTB0EiLXUC8',
     'Đóng gói che giấu trạng thái nội bộ; kế thừa tái sử dụng hành vi; đa hình cho phép gọi qua interface.',
     2, 'PUBLISHED'),
    (1003, 101, 'Bản nháp: Generics và Collections',
     'https://www.youtube.com/watch?v=K1iu1kXkVoA',
     'Nội dung đang biên soạn cho Generics.',
     3, 'DRAFT'),
    (1004, 101, 'Bài cũ: Cài đặt Java 8',
     'https://www.youtube.com/watch?v=eIrMbAQSU34',
     'Bài hướng dẫn cũ đã được lưu trữ.',
     4, 'ARCHIVED'),
    (1005, 102, 'Chuẩn hóa bảng 1NF, 2NF, 3NF',
     'https://www.youtube.com/watch?v=UrYLYV7WSHM',
     'Thiết kế khóa chính, khóa ngoại và loại bỏ phụ thuộc bắc cầu.',
     1, 'PUBLISHED'),
    (1006, 103, 'Tổng quan Machine Learning',
     '',
     'Khái niệm học có giám sát và không giám sát.',
     1, 'PUBLISHED');

-- 5. Quizzes (PUBLISHED, CLOSED và DRAFT)
INSERT INTO quizzes (id, lesson_id, title, status, created_by, published_at, closed_at)
VALUES
    (2001, 1001, 'Kiểm tra nhanh JVM & Bytecode', 'PUBLISHED', @teacher_a,
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY), NULL),
    (2002, 1002, 'Bài tập Đóng gói & Đa hình', 'PUBLISHED', @teacher_a,
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY), NULL),
    (2003, 1001, 'Quiz cũ (Đã đóng) — Kiểu dữ liệu', 'CLOSED', @teacher_a,
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 10 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY)),
    (2004, 1002, 'Quiz nháp chưa xuất bản', 'DRAFT', @teacher_a, NULL, NULL),
    (2005, 1005, 'Kiểm tra chuẩn hóa CSDL (Chưa có lượt nộp)', 'PUBLISHED', @teacher_a,
     DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY), NULL);

-- 6. Câu hỏi
INSERT INTO questions (id, quiz_id, content, question_type, explanation, order_index, generated_by_ai)
VALUES
    -- Quiz 2001 (4 câu)
    (3001, 2001, 'JVM có vai trò chính gì trong hệ sinh thái Java?', 'MULTIPLE_CHOICE',
     'JVM chịu trách nhiệm thực thi bytecode (.class) trên từng hệ điều hành.', 0, FALSE),
    (3002, 2001, 'Vùng nhớ nào trong JVM lưu trữ các đối tượng (object) được cấp phát bằng new?', 'MULTIPLE_CHOICE',
     'Tất cả đối tượng được cấp phát trên Heap; biến cục bộ nguyên thủy nằm trên Stack.', 1, TRUE),
    (3003, 2001, 'File mã nguồn .java sau khi biên dịch bằng javac tạo ra định dạng gì?', 'MULTIPLE_CHOICE',
     'Trình biên dịch javac chuyển mã nguồn .java thành bytecode .class.', 2, TRUE),
    (3004, 2001, 'Thành phần nào tự động thu hồi bộ nhớ của các đối tượng không còn tham chiếu?', 'MULTIPLE_CHOICE',
     'Garbage Collector (GC) tự động dọn dẹp các đối tượng không còn reachable trên Heap.', 3, FALSE),

    -- Quiz 2002 (3 câu)
    (3005, 2002, 'Nguyên tắc đóng gói (Encapsulation) thường được thể hiện như thế nào?', 'MULTIPLE_CHOICE',
     'Đặt thuộc tính private và kiểm soát truy cập qua các phương thức nghiệp vụ hợp lệ.', 0, FALSE),
    (3006, 2002, 'Đa hình lúc chạy (Runtime Polymorphism) trong Java dựa trên cơ chế nào?', 'MULTIPLE_CHOICE',
     'Ghi đè phương thức (Method Overriding) kết hợp dynamic dispatch qua tham chiếu lớp cha/interface.', 1, TRUE),
    (3007, 2002, 'Nguyên tắc Dependency Inversion (DIP) khuyến nghị service nên phụ thuộc vào gì?', 'MULTIPLE_CHOICE',
     'Module cấp cao nên phụ thuộc vào interface/abstraction thay vì lớp cài đặt cụ thể.', 2, FALSE),

    -- Quiz 2003 (2 câu - đã đóng)
    (3008, 2003, 'Kiểu dữ liệu nào phù hợp nhất để lưu số tiền chính xác trong Java?', 'MULTIPLE_CHOICE',
     'BigDecimal tránh sai số dấu phẩy động của float/double.', 0, FALSE),
    (3009, 2003, 'Kiểu String trong Java có tính chất nào sau đây?', 'MULTIPLE_CHOICE',
     'String là bất biến (immutable).', 1, FALSE),

    -- Quiz 2005 (2 câu - khóa 102, chưa có lượt nộp)
    (3010, 2005, 'Dạng chuẩn 3NF yêu cầu loại bỏ loại phụ thuộc hàm nào?', 'MULTIPLE_CHOICE',
     'Loại bỏ phụ thuộc bắc cầu giữa các thuộc tính không khóa.', 0, FALSE),
    (3011, 2005, 'Ràng buộc nào đảm bảo giá trị khóa ngoại tồn tại ở bảng cha?', 'MULTIPLE_CHOICE',
     'FOREIGN KEY đảm bảo toàn vẹn tham chiếu.', 1, FALSE);

-- 7. Đáp án (đúng 1 đáp án đúng mỗi câu)
INSERT INTO question_options (id, question_id, content, is_correct, order_index)
VALUES
    -- Q3001
    (4001, 3001, 'Thực thi bytecode Java độc lập nền tảng', TRUE, 0),
    (4002, 3001, 'Thiết kế giao diện HTML/CSS', FALSE, 1),
    (4003, 3001, 'Quản lý bảng cơ sở dữ liệu MySQL', FALSE, 2),
    (4004, 3001, 'Nén tệp video YouTube', FALSE, 3),
    -- Q3002
    (4005, 3002, 'Stack frame cục bộ', FALSE, 0),
    (4006, 3002, 'Heap memory', TRUE, 1),
    (4007, 3002, 'Program Counter Register', FALSE, 2),
    (4008, 3002, 'CPU L1 Cache', FALSE, 3),
    -- Q3003
    (4009, 3003, 'Tệp .exe chạy trực tiếp trên Windows', FALSE, 0),
    (4010, 3003, 'Tệp .sql kịch bản cơ sở dữ liệu', FALSE, 1),
    (4011, 3003, 'Tệp bytecode .class', TRUE, 2),
    (4012, 3003, 'Tệp cấu hình .yaml', FALSE, 3),
    -- Q3004
    (4013, 3004, 'ClassLoader', FALSE, 0),
    (4014, 3004, 'Bytecode Verifier', FALSE, 1),
    (4015, 3004, 'JDBC Driver', FALSE, 2),
    (4016, 3004, 'Garbage Collector', TRUE, 3),

    -- Q3005
    (4017, 3005, 'Ẩn dữ liệu bằng private và mở hành vi hợp lệ qua phương thức', TRUE, 0),
    (4018, 3005, 'Đặt mọi biến thành public static', FALSE, 1),
    (4019, 3005, 'Gộp toàn bộ code vào một hàm main', FALSE, 2),
    -- Q3006
    (4020, 3006, 'Nạp chồng toán tử (Operator Overloading)', FALSE, 0),
    (4021, 3006, 'Biến toàn cục static', FALSE, 1),
    (4022, 3006, 'Ghi đè phương thức (Method Overriding)', TRUE, 2),
    -- Q3007
    (4023, 3007, 'Phụ thuộc trực tiếp vào câu lệnh SQL nội bộ', FALSE, 0),
    (4024, 3007, 'Phụ thuộc vào lớp tiện ích static', FALSE, 1),
    (4025, 3007, 'Không dùng interface', FALSE, 2),
    (4026, 3007, 'Phụ thuộc vào interface / abstraction', TRUE, 3),

    -- Q3008
    (4029, 3008, 'BigDecimal', TRUE, 0),
    (4030, 3008, 'float', FALSE, 1),
    (4031, 3008, 'boolean', FALSE, 2),
    -- Q3009
    (4033, 3009, 'Có thể thay đổi ký tự trực tiếp tại chỗ', FALSE, 0),
    (4034, 3009, 'Bất biến (Immutable)', TRUE, 1),

    -- Q3010
    (4035, 3010, 'Phụ thuộc bắc cầu giữa thuộc tính không khóa', TRUE, 0),
    (4036, 3010, 'Khóa chính tự tăng', FALSE, 1),
    -- Q3011
    (4037, 3011, 'FOREIGN KEY', TRUE, 0),
    (4038, 3011, 'ORDER BY', FALSE, 1);

-- 8. Lượt làm bài (SUBMITTED + 1 lượt IN_PROGRESS để kiểm chứng không bị tính vào thống kê)
INSERT INTO quiz_attempts (id, quiz_id, student_id, status, score, total_questions, correct_count, started_at, submitted_at)
VALUES
    -- Quiz 2001 (6 lượt SUBMITTED)
    (5001, 2001, @student_a, 'SUBMITTED', 50.00,  4, 2, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 96 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 95 HOUR)),
    (5002, 2001, @student_a, 'SUBMITTED', 100.00, 4, 4, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 72 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 71 HOUR)),
    (5003, 2001, @student_b, 'SUBMITTED', 75.00,  4, 3, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 59 HOUR)),
    (5004, 2001, @student_c, 'SUBMITTED', 25.00,  4, 1, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 48 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 47 HOUR)),
    (5005, 2001, @student_c, 'SUBMITTED', 75.00,  4, 3, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 30 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 29 HOUR)),
    (5006, 2001, @student_d, 'SUBMITTED', 50.00,  4, 2, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 11 HOUR)),

    -- Quiz 2002 (3 lượt SUBMITTED + 1 lượt IN_PROGRESS của student_d => tỷ lệ hoàn thành khóa = 3/4 = 75%)
    (5007, 2002, @student_a, 'SUBMITTED', 100.00, 3, 3, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 50 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 49 HOUR)),
    (5008, 2002, @student_b, 'SUBMITTED', 66.67,  3, 2, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 24 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 23 HOUR)),
    (5009, 2002, @student_c, 'SUBMITTED', 33.33,  3, 1, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 HOUR),  DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 HOUR)),
    (5010, 2002, @student_d, 'IN_PROGRESS', NULL, 3, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 HOUR), NULL),

    -- Quiz 2003 (CLOSED, 2 lượt SUBMITTED lịch sử)
    (5011, 2003, @student_a, 'SUBMITTED', 100.00, 2, 2, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 120 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 119 HOUR)),
    (5012, 2003, @student_b, 'SUBMITTED', 50.00,  2, 1, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 110 HOUR), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 109 HOUR));

-- 9. Chi tiết câu trả lời (gồm cả câu bỏ trống selected_option_id = NULL)
INSERT INTO student_answers (attempt_id, question_id, selected_option_id, is_correct)
VALUES
    -- Attempt 5001 (student_a, 2/4 = 50): sai Q3002, bỏ trống Q3004
    (5001, 3001, 4001, TRUE),
    (5001, 3002, 4005, FALSE),
    (5001, 3003, 4011, TRUE),
    (5001, 3004, NULL, FALSE),
    -- Attempt 5002 (student_a, 4/4 = 100)
    (5002, 3001, 4001, TRUE),
    (5002, 3002, 4006, TRUE),
    (5002, 3003, 4011, TRUE),
    (5002, 3004, 4016, TRUE),
    -- Attempt 5003 (student_b, 3/4 = 75): sai Q3002
    (5003, 3001, 4001, TRUE),
    (5003, 3002, 4007, FALSE),
    (5003, 3003, 4011, TRUE),
    (5003, 3004, 4016, TRUE),
    -- Attempt 5004 (student_c, 1/4 = 25): sai Q3002, Q3003, bỏ trống Q3004
    (5004, 3001, 4001, TRUE),
    (5004, 3002, 4005, FALSE),
    (5004, 3003, 4009, FALSE),
    (5004, 3004, NULL, FALSE),
    -- Attempt 5005 (student_c, 3/4 = 75): sai Q3002
    (5005, 3001, 4001, TRUE),
    (5005, 3002, 4008, FALSE),
    (5005, 3003, 4011, TRUE),
    (5005, 3004, 4016, TRUE),
    -- Attempt 5006 (student_d, 2/4 = 50): sai Q3002, Q3003
    (5006, 3001, 4001, TRUE),
    (5006, 3002, 4005, FALSE),
    (5006, 3003, 4010, FALSE),
    (5006, 3004, 4016, TRUE),

    -- Attempt 5007 (student_a, 3/3 = 100)
    (5007, 3005, 4017, TRUE),
    (5007, 3006, 4022, TRUE),
    (5007, 3007, 4026, TRUE),
    -- Attempt 5008 (student_b, 2/3 = 66.67): sai Q3006
    (5008, 3005, 4017, TRUE),
    (5008, 3006, 4020, FALSE),
    (5008, 3007, 4026, TRUE),
    -- Attempt 5009 (student_c, 1/3 = 33.33): bỏ trống Q3005, sai Q3006
    (5009, 3005, NULL, FALSE),
    (5009, 3006, 4021, FALSE),
    (5009, 3007, 4026, TRUE),

    -- Attempt 5011 (student_a, 2/2 = 100)
    (5011, 3008, 4029, TRUE),
    (5011, 3009, 4034, TRUE),
    -- Attempt 5012 (student_b, 1/2 = 50): sai Q3009
    (5012, 3008, 4029, TRUE),
    (5012, 3009, 4033, FALSE);
