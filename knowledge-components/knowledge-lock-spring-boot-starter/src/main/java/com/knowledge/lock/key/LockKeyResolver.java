package com.knowledge.lock.key;

import java.lang.reflect.Method;
import java.util.List;

public interface LockKeyResolver {

    List<String> resolve(String[] expressions, Object target, Method method, Object[] arguments);
}
