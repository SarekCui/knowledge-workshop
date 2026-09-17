CREATE TABLE course_category (
    id VARCHAR(64) NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_category_name (name),
    INDEX idx_course_category_status_sort (status, sort_order, id)
);

INSERT INTO course_category(id, name, sort_order, status, created_at, updated_at) VALUES
    ('general', '综合技术', 0, 'ACTIVE', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('backend', '后端开发', 10, 'ACTIVE', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('frontend', '前端开发', 20, 'ACTIVE', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('database', '数据库', 30, 'ACTIVE', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('distributed', '分布式系统', 40, 'ACTIVE', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));

ALTER TABLE course ADD COLUMN category_id VARCHAR(64) NULL AFTER id;
UPDATE course SET category_id = 'general' WHERE category_id IS NULL;
UPDATE course SET category_id = 'backend' WHERE id = 'course-java';
ALTER TABLE course MODIFY category_id VARCHAR(64) NOT NULL DEFAULT 'general';
ALTER TABLE course ADD INDEX idx_course_category_status_updated (category_id, status, updated_at);
ALTER TABLE course ADD CONSTRAINT fk_course_category FOREIGN KEY (category_id) REFERENCES course_category(id);
