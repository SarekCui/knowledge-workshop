-- Align Agent persistence names with their business semantics. Earlier migrations remain immutable.
ALTER TABLE agent_run
    RENAME COLUMN client_request_id TO idempotency_key,
    RENAME COLUMN requester_id TO user_id;

ALTER TABLE agent_message
    RENAME COLUMN client_request_id TO idempotency_key;
