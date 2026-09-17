ALTER TABLE season
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY',
    ADD COLUMN settled_at DATETIME NULL,
    ADD COLUMN snapshot_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_error VARCHAR(200) NULL;

ALTER TABLE point_task ADD COLUMN occurred_at DATETIME NULL;
UPDATE point_task
SET occurred_at = CAST(REPLACE(LEFT(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.occurredAt')), 19), 'T', ' ') AS DATETIME);
ALTER TABLE point_task MODIFY COLUMN occurred_at DATETIME NOT NULL,
    ADD INDEX idx_point_task_occurred_at (occurred_at);

CREATE TABLE rejected_point_event (
    event_id VARCHAR(128) NOT NULL,
    payload JSON NOT NULL,
    reason VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (event_id)
) COMMENT='已结算赛季晚到积分事件，保留供人工处理';
