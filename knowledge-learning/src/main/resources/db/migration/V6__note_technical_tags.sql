CREATE TABLE note_tag (
    note_id VARCHAR(64) NOT NULL,
    normalized_name VARCHAR(20) NOT NULL,
    display_name VARCHAR(20) NOT NULL,
    sort_order TINYINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (note_id, normalized_name),
    UNIQUE KEY uk_note_tag_order (note_id, sort_order),
    INDEX idx_note_tag_name (normalized_name, note_id),
    CONSTRAINT fk_note_tag_note FOREIGN KEY (note_id) REFERENCES note(id)
);
