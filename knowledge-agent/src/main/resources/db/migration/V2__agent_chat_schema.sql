CREATE TABLE agent_conversation (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(120) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_agent_conversation_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_message (
    id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    run_id VARCHAR(64) NULL,
    role VARCHAR(16) NOT NULL,
    client_request_id VARCHAR(64) NULL,
    content TEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_message_conversation_request (conversation_id, client_request_id),
    KEY idx_agent_message_conversation_created (conversation_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE agent_run
    ADD COLUMN conversation_id VARCHAR(64) NULL AFTER event_id,
    ADD COLUMN client_request_id VARCHAR(64) NULL AFTER conversation_id,
    ADD UNIQUE KEY uk_agent_run_conversation_request (conversation_id, client_request_id),
    ADD KEY idx_agent_run_conversation_status (conversation_id, status);
