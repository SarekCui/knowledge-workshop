ALTER TABLE note_comment
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0 AFTER content,
    ADD CONSTRAINT chk_note_comment_like_count CHECK (like_count >= 0);

CREATE TABLE note_comment_like (
    id VARCHAR(64) PRIMARY KEY,
    comment_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_note_comment_like_user (comment_id, user_id),
    INDEX idx_note_comment_like_user_created (user_id, created_at, comment_id)
);
