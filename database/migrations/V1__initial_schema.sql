CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL DEFAULT 'STUDENT',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE teacher_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    decided_by BIGINT NULL,
    decision_note TEXT NULL,
    decided_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_teacher_requests_user_created (user_id, created_at),
    KEY idx_teacher_requests_status_created (status, created_at),
    CONSTRAINT fk_teacher_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_teacher_requests_decider FOREIGN KEY (decided_by) REFERENCES users (id)
);

CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    thumbnail_url VARCHAR(2048) NULL,
    thumbnail_storage_key VARCHAR(512) NULL,
    price DECIMAL(14,2) NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    KEY idx_courses_marketplace (is_published, archived_at, created_at),
    KEY idx_courses_teacher (teacher_id),
    KEY idx_courses_category (category_id),

    CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES users (id),
    CONSTRAINT fk_courses_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE course_members (
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    enrolled_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (course_id, student_id),

    KEY idx_course_members_student (student_id, enrolled_at),

    CONSTRAINT fk_course_members_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_course_members_student FOREIGN KEY (student_id) REFERENCES users (id)
);

CREATE TABLE lessons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    video_url VARCHAR(2048) NOT NULL,
    transcript_text TEXT NULL,
    order_index INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE (course_id, order_index),
    CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE TABLE lesson_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    extracted_text MEDIUMTEXT NULL,
    content_hash CHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    KEY idx_lesson_documents_lesson (lesson_id),

    CONSTRAINT fk_lesson_documents_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT fk_lesson_documents_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id)
);

CREATE TABLE ai_generations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    source_document_id BIGINT NULL,
    source_type ENUM('TRANSCRIPT', 'DOCUMENT') NOT NULL,
    generation_type ENUM('QUIZ', 'FLASHCARD') NOT NULL,
    requested_count INT NOT NULL,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NULL,
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    source_hash CHAR(64) NOT NULL,
    status ENUM('SUCCEEDED', 'FAILED') NOT NULL,
    candidate_json JSON NULL,
    error_code VARCHAR(100) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_ai_generations_lesson_created (lesson_id, created_at),
    CONSTRAINT fk_ai_generations_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT fk_ai_generations_document FOREIGN KEY (source_document_id) REFERENCES lesson_documents (id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_generations_creator FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE quizzes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    status ENUM('DRAFT', 'PUBLISHED', 'CLOSED') NOT NULL DEFAULT 'DRAFT',
    generation_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    published_at DATETIME(6) NULL,
    closed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_quizzes_lesson_status (lesson_id, status),
    KEY idx_quizzes_generation (generation_id),
    CONSTRAINT fk_quizzes_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT fk_quizzes_generation FOREIGN KEY (generation_id) REFERENCES ai_generations (id),
    CONSTRAINT fk_quizzes_creator FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    question_type ENUM('MULTIPLE_CHOICE') NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    explanation TEXT NULL,
    order_index INT NOT NULL,
    generated_by_ai BOOLEAN NOT NULL DEFAULT FALSE,
    generation_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (quiz_id, order_index),
    CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id),
    CONSTRAINT fk_questions_generation FOREIGN KEY (generation_id) REFERENCES ai_generations (id)
);

CREATE TABLE question_options (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INT NOT NULL,
    UNIQUE (question_id, order_index),
    CONSTRAINT fk_question_options_question FOREIGN KEY (question_id) REFERENCES questions (id)
);

CREATE TABLE quiz_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status ENUM('IN_PROGRESS', 'SUBMITTED') NOT NULL DEFAULT 'IN_PROGRESS',
    score DECIMAL(5,2) NULL,
    total_questions INT NOT NULL,
    correct_count INT NULL,
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    submitted_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    KEY idx_quiz_attempts_quiz_submitted (quiz_id, submitted_at),
    KEY idx_quiz_attempts_student_started (student_id, started_at),
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id),
    CONSTRAINT fk_quiz_attempts_student FOREIGN KEY (student_id) REFERENCES users (id)
);

CREATE TABLE student_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_id BIGINT NULL,
    is_correct BOOLEAN NOT NULL,
    UNIQUE (attempt_id, question_id),
    KEY idx_student_answers_question_correct (question_id, is_correct),
    CONSTRAINT fk_student_answers_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (id),
    CONSTRAINT fk_student_answers_question FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_student_answers_option FOREIGN KEY (selected_option_id) REFERENCES question_options (id)
);

CREATE TABLE flashcards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lesson_id BIGINT NOT NULL,
    front_text TEXT NOT NULL,
    back_text TEXT NOT NULL,
    status ENUM('DRAFT', 'PUBLISHED', 'CLOSED') NOT NULL DEFAULT 'DRAFT',
    generated_by_ai BOOLEAN NOT NULL DEFAULT FALSE,
    generation_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    published_at DATETIME(6) NULL,
    closed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_flashcards_lesson_status (lesson_id, status),
    KEY idx_flashcards_generation (generation_id),
    CONSTRAINT fk_flashcards_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT fk_flashcards_generation FOREIGN KEY (generation_id) REFERENCES ai_generations (id),
    CONSTRAINT fk_flashcards_creator FOREIGN KEY (created_by) REFERENCES users (id)
);
