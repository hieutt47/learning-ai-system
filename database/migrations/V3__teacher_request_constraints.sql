ALTER TABLE teacher_requests
    ADD CONSTRAINT ck_teacher_requests_decision CHECK (
        (status = 'PENDING' AND decided_by IS NULL AND decision_note IS NULL AND decided_at IS NULL)
        OR (status IN ('APPROVED', 'REJECTED') AND decided_by IS NOT NULL AND decided_at IS NOT NULL)
    ),
    ADD CONSTRAINT ck_teacher_requests_no_self_approval CHECK (
        status <> 'APPROVED' OR decided_by <> user_id
    );
