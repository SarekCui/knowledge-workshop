CREATE TABLE agent_event_inbox (
    id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    consumer VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_event_inbox_event_consumer (event_id, consumer)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_run (
    id VARCHAR(64) NOT NULL,
    run_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    event_id VARCHAR(64) NULL,
    source_comment_id VARCHAR(64) NULL,
    note_id VARCHAR(64) NULL,
    requester_id VARCHAR(64) NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_run_source_comment (source_comment_id),
    KEY idx_agent_run_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
