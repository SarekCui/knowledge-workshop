package com.knowledge.lock.core;

@FunctionalInterface
public interface LockCallback<T> {
    T execute() throws Throwable;
}
