ALTER TABLE note
    MODIFY course_id VARCHAR(64) NULL,
    ADD status VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
    ADD published_at DATETIME(3) NULL,
    ADD CONSTRAINT chk_note_status CHECK (status IN ('DRAFT', 'PRIVATE', 'PUBLIC')),
    ADD INDEX idx_note_public_latest (status, deleted, published_at, id),
    ADD INDEX idx_note_course_public (course_id, status, deleted, published_at, id),
    ADD INDEX idx_note_owner_updated (user_id, deleted, updated_at, id);
