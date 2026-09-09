CREATE TABLE mk_group_activity (
    id VARCHAR(64) PRIMARY KEY,
    course_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NOT NULL,
    target_count INT NOT NULL,
    max_join_per_user INT NOT NULL,
    price_cents BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    INDEX idx_activity_course_status (course_id, status)
);

CREATE TABLE mk_group_order (
    id VARCHAR(64) PRIMARY KEY,
    activity_id VARCHAR(64) NOT NULL,
    owner_user_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    target_count INT NOT NULL,
    confirmed_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME(3) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    INDEX idx_group_activity_status (activity_id, status)
);

CREATE TABLE mk_group_participant (
    id VARCHAR(64) PRIMARY KEY,
    activity_id VARCHAR(64) NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_participant_request (request_id),
    UNIQUE KEY uk_participant_group_user (group_id, user_id),
    INDEX idx_participant_activity_user (activity_id, user_id, status)
);

CREATE TABLE mk_trade_order (
    id VARCHAR(64) PRIMARY KEY,
    business_request_id VARCHAR(128) NOT NULL,
    payment_trade_no VARCHAR(128),
    user_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    activity_id VARCHAR(64) NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    amount_cents BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3),
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_order_request (business_request_id),
    UNIQUE KEY uk_order_payment_trade (payment_trade_no),
    INDEX idx_order_group_status (group_id, status)
);

CREATE TABLE mk_notification_task (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL,
    task_type VARCHAR(40) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NOT NULL,
    last_error VARCHAR(500),
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_notification_event (event_id),
    INDEX idx_notification_dispatch (status, next_retry_at)
);
