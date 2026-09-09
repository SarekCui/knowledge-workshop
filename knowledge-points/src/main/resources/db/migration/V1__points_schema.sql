CREATE TABLE pt_signin_record (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    sign_date DATE NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    continuous_days INT NOT NULL,
    reward_points INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_signin_user_date (user_id, sign_date),
    UNIQUE KEY uk_signin_event (event_id)
);

CREATE TABLE pt_point_task (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NOT NULL,
    last_error VARCHAR(500),
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_point_task_event (event_id),
    KEY idx_point_task_dispatch (status, next_retry_at)
);

CREATE TABLE pt_point_account (
    user_id VARCHAR(64) PRIMARY KEY,
    total_points BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL
);

CREATE TABLE pt_season_account (
    season VARCHAR(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    points BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (season, user_id),
    KEY idx_season_points (season, points)
);

CREATE TABLE pt_season_snapshot (
    id VARCHAR(36) PRIMARY KEY,
    season VARCHAR(16) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    score BIGINT NOT NULL,
    rank_no INT NOT NULL,
    snapshot_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_snapshot_season_rank (season, rank_no),
    UNIQUE KEY uk_snapshot_season_user (season, user_id)
);

CREATE TABLE pt_season_archive (
    id VARCHAR(36) PRIMARY KEY,
    season VARCHAR(16) NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    archived_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_season_archive (season)
);

CREATE TABLE pt_job_execution (
    id VARCHAR(36) PRIMARY KEY,
    job_name VARCHAR(64) NOT NULL,
    business_key VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    finished_at DATETIME(3),
    UNIQUE KEY uk_job_business (job_name, business_key)
);

CREATE TABLE pt_point_ledger_2026_q1 (
    id VARCHAR(36) PRIMARY KEY, event_id VARCHAR(128) NOT NULL, user_id VARCHAR(64) NOT NULL,
    points INT NOT NULL, source_type VARCHAR(32) NOT NULL, source_id VARCHAR(64) NOT NULL,
    season VARCHAR(16) NOT NULL, occurred_at DATETIME(3) NOT NULL, created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_ledger_event (event_id), KEY idx_ledger_user_time (user_id, occurred_at)
);
CREATE TABLE pt_point_ledger_2026_q2 LIKE pt_point_ledger_2026_q1;
CREATE TABLE pt_point_ledger_2026_q3 LIKE pt_point_ledger_2026_q1;
CREATE TABLE pt_point_ledger_2026_q4 LIKE pt_point_ledger_2026_q1;
