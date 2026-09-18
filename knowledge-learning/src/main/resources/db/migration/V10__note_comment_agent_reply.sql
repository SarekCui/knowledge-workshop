ALTER TABLE note_comment
    ADD COLUMN author_type VARCHAR(16) NOT NULL DEFAULT 'USER' AFTER user_id,
    ADD COLUMN source_comment_id VARCHAR(64) NULL AFTER parent_comment_id,
    ADD UNIQUE KEY uk_note_comment_source_comment (source_comment_id);
