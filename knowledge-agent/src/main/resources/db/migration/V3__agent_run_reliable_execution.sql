ALTER TABLE agent_run
    ADD COLUMN run_stage VARCHAR(16) NOT NULL DEFAULT 'CONTEXT' AFTER status,
    ADD COLUMN lease_owner VARCHAR(128) NULL AFTER attempt_count,
    ADD COLUMN lease_until DATETIME(3) NULL AFTER lease_owner,
    ADD COLUMN execution_version BIGINT NOT NULL DEFAULT 0 AFTER lease_until,
    ADD COLUMN next_retry_at DATETIME(3) NULL AFTER execution_version,
    ADD COLUMN answer TEXT NULL AFTER next_retry_at,
    ADD COLUMN last_error_code VARCHAR(64) NULL AFTER answer,
    ADD COLUMN context_completed_at DATETIME(3) NULL AFTER last_error,
    ADD COLUMN generated_at DATETIME(3) NULL AFTER context_completed_at,
    ADD COLUMN published_at DATETIME(3) NULL AFTER generated_at,
    ADD KEY idx_agent_run_retry (status, next_retry_at),
    ADD KEY idx_agent_run_lease (status, lease_until);

CREATE TABLE agent_execution_outbox (
    id VARCHAR(64) NOT NULL,
    run_id VARCHAR(64) NOT NULL,
    execution_version BIGINT NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NOT NULL,
    last_error VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_execution_outbox_run_version (run_id, execution_version),
    KEY idx_agent_execution_outbox_dispatch (status, next_retry_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
