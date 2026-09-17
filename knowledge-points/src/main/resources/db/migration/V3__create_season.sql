CREATE TABLE season (
    season VARCHAR(16) NOT NULL COMMENT '自然季度赛季标识',
    name VARCHAR(100) NOT NULL COMMENT '展示名称',
    starts_at DATETIME NOT NULL COMMENT 'UTC 开始时间，包含',
    ends_at DATETIME NOT NULL COMMENT 'UTC 结束时间，不包含',
    created_at DATETIME NOT NULL,
    PRIMARY KEY (season),
    KEY idx_season_starts_at (starts_at)
) COMMENT='积分赛季配置';
