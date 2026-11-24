-- Preserve visibility of existing lessons, but fail closed for new direct SQL inserts.
ALTER TABLE lessons
    ADD COLUMN status VARCHAR(16) NULL,
    ADD KEY idx_lessons_course_status_order (course_id, status, order_index);
UPDATE lessons SET status = 'PUBLISHED';
ALTER TABLE lessons MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'DRAFT';
