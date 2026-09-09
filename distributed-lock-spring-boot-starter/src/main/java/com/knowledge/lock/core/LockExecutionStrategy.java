package com.knowledge.lock.core;

public interface LockExecutionStrategy {

    boolean supports(LockOptions options);

    <T> T execute(LockOptions options, LockCallback<T> callback) throws Throwable;
}
