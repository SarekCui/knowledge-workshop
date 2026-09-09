package com.knowledge.lock.core;

import com.knowledge.lock.enums.LockType;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

public record LockOptions(List<String> keys, LockType lockType, long waitTime, long leaseTime, TimeUnit timeUnit) {

    public LockOptions {
        Objects.requireNonNull(keys, "锁 Key 集合不能为空");
        Objects.requireNonNull(lockType, "锁类型不能为空");
        Objects.requireNonNull(timeUnit, "时间单位不能为空");
        keys = keys.stream().distinct().toList();
        if (keys.isEmpty() || keys.stream().anyMatch(key -> key == null || key.isBlank())) {
            throw new IllegalArgumentException("锁 Key 不能为空");
        }
        if (waitTime < 0 || leaseTime == 0 || leaseTime < -1) {
            throw new IllegalArgumentException("等待时间不能小于 0，租约必须为 -1 或大于 0");
        }
    }
}
