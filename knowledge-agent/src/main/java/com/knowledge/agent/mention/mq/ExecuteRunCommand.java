package com.knowledge.agent.mention.mq;

import java.time.Instant;

/** Durable command payload. It deliberately contains no prompt or user content. */
public record ExecuteRunCommand(
        String commandId,
        Instant createdAt,
        String runId,
        String runType) {
}
