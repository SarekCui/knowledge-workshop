CREATE TABLE user_profile (
    user_id VARCHAR(64) NOT NULL,
    nickname VARCHAR(32) NOT NULL,
    avatar_object_key VARCHAR(255) NULL,
    bio VARCHAR(200) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profile_account FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO user_profile (user_id, nickname, avatar_object_key, bio, version, created_at, updated_at)
SELECT id, username, NULL, NULL, 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM user_account;
