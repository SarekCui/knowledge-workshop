USE knowledge_iam;

INSERT INTO iam_user_account
    (id, username, password_hash, status, created_at, updated_at)
VALUES
    ('user-demo', 'demo', '$2y$10$qN7NO4fUsdqGctuBbL1cmunHxshfCOIOnou/dLnAAzkTS7swkE.ge',
     'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), status = VALUES(status), updated_at = VALUES(updated_at);

INSERT INTO iam_role (id, code, name, status, created_at, updated_at)
VALUES ('role-learner', 'LEARNER', '学习者', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status), updated_at = VALUES(updated_at);

INSERT INTO iam_user_role (id, user_id, role_id, created_at)
VALUES ('user-role-demo-learner', 'user-demo', 'role-learner', UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

USE knowledge_marketing;

INSERT INTO mk_group_activity
    (id, course_id, status, start_time, end_time, target_count, max_join_per_user,
     price_cents, version, created_at, updated_at)
VALUES
    ('activity-demo', 'course-java', 'ACTIVE', '2026-01-01 00:00:00.000',
     '2030-01-01 00:00:00.000', 3, 2, 9900, 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

USE knowledge_learning;

INSERT INTO lr_course
    (id, title, summary, cover_url, price_cents, status, version, created_at, updated_at)
VALUES
    ('course-java', 'Java 微服务实战', '从业务闭环出发学习 Spring Boot 微服务开发',
     NULL, 19900, 'PUBLISHED', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE title = VALUES(title), summary = VALUES(summary), updated_at = VALUES(updated_at);

INSERT INTO lr_chapter
    (id, course_id, title, sort_order, video_id, video_url, video_duration_ms,
     video_version, status, version, created_at, updated_at)
VALUES
    ('chapter-java-1', 'course-java', '微服务业务边界', 1, 'video-java-1',
     'https://example.invalid/videos/java-1.m3u8', 600000, 1, 'PUBLISHED', 0,
     UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE title = VALUES(title), video_duration_ms = VALUES(video_duration_ms),
                        updated_at = VALUES(updated_at);

USE knowledge_marketing;

INSERT INTO mk_group_order
    (id, activity_id, owner_user_id, status, target_count, confirmed_count,
     expires_at, version, created_at, updated_at)
VALUES
    ('group-demo', 'activity-demo', 'owner-demo', 'FORMING', 3, 0,
     '2030-01-01 00:00:00.000', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);
