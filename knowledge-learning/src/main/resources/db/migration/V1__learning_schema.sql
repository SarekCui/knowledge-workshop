CREATE TABLE lr_course (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    cover_url VARCHAR(500),
    price_cents BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    INDEX idx_course_status_updated (status, updated_at)
);

CREATE TABLE lr_chapter (
    id VARCHAR(64) PRIMARY KEY,
    course_id VARCHAR(64) NOT NULL,
    title VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL,
    video_id VARCHAR(64) NOT NULL,
    video_url VARCHAR(500) NOT NULL,
    video_duration_ms BIGINT NOT NULL,
    video_version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_chapter_video_version (video_id, video_version),
    INDEX idx_chapter_course_order (course_id, sort_order)
);

CREATE TABLE lr_note (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    chapter_id VARCHAR(64),
    client_request_id VARCHAR(128) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    video_position_ms BIGINT,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_note_user_request (user_id, client_request_id),
    INDEX idx_note_user_course_updated (user_id, course_id, updated_at),
    INDEX idx_note_chapter (chapter_id, deleted)
);

CREATE TABLE lr_course_entitlement (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    effective_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3),
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_entitlement_source_user_course (source_type, source_id, user_id, course_id),
    INDEX idx_entitlement_user_course_status (user_id, course_id, status, expires_at)
);

CREATE TABLE lr_message_inbox (
    id VARCHAR(64) PRIMARY KEY,
    consumer_name VARCHAR(80) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    processed_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_inbox_consumer_event (consumer_name, event_id)
);

CREATE TABLE lr_video_progress (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    chapter_id VARCHAR(64) NOT NULL,
    video_id VARCHAR(64) NOT NULL,
    video_version INT NOT NULL,
    resume_position_ms BIGINT NOT NULL DEFAULT 0,
    max_position_ms BIGINT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL,
    watched_seconds BIGINT NOT NULL DEFAULT 0,
    completion_rate INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    last_session_epoch BIGINT NOT NULL DEFAULT 0,
    last_sequence BIGINT NOT NULL DEFAULT 0,
    last_event_id VARCHAR(128),
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_progress_user_video_version (user_id, video_id, video_version),
    INDEX idx_progress_user_updated (user_id, updated_at)
);

CREATE TABLE lr_watched_segment (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    video_id VARCHAR(64) NOT NULL,
    video_version INT NOT NULL,
    segment_index INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_watched_segment (user_id, video_id, video_version, segment_index)
);

CREATE TABLE lr_progress_event_inbox (
    id VARCHAR(64) PRIMARY KEY,
    consumer_name VARCHAR(80) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    processed_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_progress_inbox_consumer_event (consumer_name, event_id)
);
