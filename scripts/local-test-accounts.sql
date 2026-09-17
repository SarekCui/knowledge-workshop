-- Local development only: dedicated test identities, no updates to existing credentials/privileges.
USE knowledge_iam;
START TRANSACTION;

INSERT INTO user_account (id, username, password_hash, status, created_at, updated_at)
VALUES
('user-local-test-admin', 'local-test-admin',
 '$2y$10$qN7NO4fUsdqGctuBbL1cmunHxshfCOIOnou/dLnAAzkTS7swkE.ge', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
('user-local-test-peer', 'local-test-peer',
 '$2y$10$qN7NO4fUsdqGctuBbL1cmunHxshfCOIOnou/dLnAAzkTS7swkE.ge', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO access_role (id, code, name, status, created_at, updated_at)
VALUES ('role-local-test-admin', 'ADMIN', '本地测试管理员', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE id = id;

INSERT IGNORE INTO user_role (id, user_id, role_id, created_at)
SELECT 'local-test-admin-role', u.id, r.id, UTC_TIMESTAMP(3)
FROM user_account u JOIN access_role r ON r.code = 'ADMIN'
WHERE u.id = 'user-local-test-admin' AND u.username = 'local-test-admin';

INSERT IGNORE INTO user_role (id, user_id, role_id, created_at)
SELECT 'local-test-peer-role', u.id, r.id, UTC_TIMESTAMP(3)
FROM user_account u JOIN access_role r ON r.code = 'LEARNER'
WHERE u.id = 'user-local-test-peer' AND u.username = 'local-test-peer';
COMMIT;
