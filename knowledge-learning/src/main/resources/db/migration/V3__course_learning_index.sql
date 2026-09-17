ALTER TABLE video_progress ADD INDEX idx_progress_user_course_updated (user_id, course_id, updated_at);
