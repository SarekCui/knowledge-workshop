CREATE TABLE iam_refresh_token (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    rotated_at DATETIME(3) NULL,
    revoked_at DATETIME(3) NULL,
    replaced_by_id VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_refresh_token_hash (token_hash),
    KEY idx_iam_refresh_token_family (family_id),
    KEY idx_iam_refresh_token_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
