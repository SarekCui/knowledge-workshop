ALTER TABLE note
    ADD like_count BIGINT NOT NULL DEFAULT 0,
    ADD favorite_count BIGINT NOT NULL DEFAULT 0,
    ADD comment_count BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_note_like_count CHECK (like_count >= 0),
    ADD CONSTRAINT chk_note_favorite_count CHECK (favorite_count >= 0),
    ADD CONSTRAINT chk_note_comment_count CHECK (comment_count >= 0),
    ADD INDEX idx_note_public_hot (status, deleted, favorite_count, like_count, comment_count, published_at, id);

CREATE TABLE note_like (
    id VARCHAR(64) PRIMARY KEY,
    note_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_note_like_user (note_id, user_id),
    INDEX idx_note_like_user_created (user_id, created_at, note_id)
);

CREATE TABLE note_favorite (
    id VARCHAR(64) PRIMARY KEY,
    note_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_note_favorite_user (note_id, user_id),
    INDEX idx_note_favorite_user_created (user_id, created_at, note_id)
);

CREATE TABLE note_comment (
    id VARCHAR(64) PRIMARY KEY,
    note_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    parent_comment_id VARCHAR(64),
    client_request_id VARCHAR(128) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_note_comment_user_request (user_id, client_request_id),
    INDEX idx_note_comment_note_created (note_id, deleted, created_at, id),
    INDEX idx_note_comment_parent (parent_comment_id, deleted)
);
