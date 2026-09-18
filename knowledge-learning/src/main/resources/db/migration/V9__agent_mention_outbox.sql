CREATE TABLE agent_mention_outbox (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NOT NULL,
    last_error VARCHAR(500),
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_agent_mention_outbox_event (event_id),
    UNIQUE KEY uk_agent_mention_outbox_comment (aggregate_id),
    INDEX idx_agent_mention_outbox_dispatch (status, next_retry_at, created_at)
);
