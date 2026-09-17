-- Repair only the exact mojibake rows created by the local operator import.
-- Version and original-byte guards avoid overwriting later user edits.
SET NAMES utf8mb4;
USE knowledge_learning;
START TRANSACTION;

UPDATE course
SET title = 'Java 微服务实战（本地联调）',
    summary = '补齐历史拼团活动的课程关联；Oceans 章节仅用于视频联调，不是 Java 教学内容。',
    version = version + 1, updated_at = UTC_TIMESTAMP(3)
WHERE id = 'course-java' AND version = 0
  AND HEX(title) = '4A61766120C3A5C2BEC2AEC3A6C593C28DC3A5C5A0C2A1C3A5C2AEC5BEC3A6CB86CB9CC3AFC2BCCB86C3A6C593C2ACC3A5C593C2B0C3A8C281E2809DC3A8C2B0C692C3AFC2BCE280B0';
SELECT ROW_COUNT() AS course_text_repaired;

UPDATE chapter
SET title = 'Oceans · 本地视频联调', version = version + 1, updated_at = UTC_TIMESTAMP(3)
WHERE id = 'chapter-java-local-oceans' AND version = 0
  AND HEX(title) = '4F6365616E7320C382C2B720C3A6C593C2ACC3A5C593C2B0C3A8C2A7E280A0C3A9C2A2E28098C3A8C281E2809DC3A8C2B0C692';
SELECT ROW_COUNT() AS chapter_text_repaired;
COMMIT;
