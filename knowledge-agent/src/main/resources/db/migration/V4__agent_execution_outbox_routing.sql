ALTER TABLE agent_execution_outbox
    ADD COLUMN routing_key VARCHAR(128) NOT NULL DEFAULT 'agent.run.requested.v1' AFTER execution_version;
