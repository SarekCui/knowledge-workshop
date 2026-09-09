package com.knowledge.lock.core;

import java.util.List;

public class LockExecutionStrategyResolver {

    private final List<LockExecutionStrategy> strategies;

    public LockExecutionStrategyResolver(List<LockExecutionStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public LockExecutionStrategy resolve(LockOptions options) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(options))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("没有匹配的分布式锁策略"));
    }
}
