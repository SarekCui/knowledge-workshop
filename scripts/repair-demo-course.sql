-- Local operator repair only. Never reset accounts, formed groups, orders or entitlements.
-- Run after migrations. INSERT IGNORE preserves existing content and progress.
SET NAMES utf8mb4;
USE knowledge_learning;

INSERT IGNORE INTO course
    (id, title, summary, cover_url, price_cents, status, version, created_at, updated_at)
VALUES
    ('course-java', 'Java 微服务实战（本地联调）',
     '补齐历史拼团活动的课程关联；Oceans 章节仅用于视频联调，不是 Java 教学内容。',
     NULL, 19900, 'PUBLISHED', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));

INSERT IGNORE INTO chapter
    (id, course_id, title, sort_order, video_id, video_url, video_duration_ms,
     video_version, status, version, created_at, updated_at)
VALUES
    ('chapter-java-local-oceans', 'course-java', 'Oceans · 本地视频联调', 10,
     'video-java-local-oceans', 'http://127.0.0.1:8090/oceans.mp4', 46613,
     1, 'PUBLISHED', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));
