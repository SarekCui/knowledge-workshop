package com.knowledge.lock.key;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SpelLockKeyResolver implements LockKeyResolver {

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Override
    public List<String> resolve(String[] expressions, Object target, Method method, Object[] arguments) {
        if (expressions == null || expressions.length == 0) {
            throw new IllegalArgumentException("分布式锁至少需要一个 Key 表达式");
        }
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                target, method, arguments, parameterNameDiscoverer);
        return Arrays.stream(expressions).map(expression -> evaluate(expression, context)).toList();
    }

    private String evaluate(String expression, MethodBasedEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("分布式锁 Key 表达式不能为空");
        }
        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("分布式锁 SpEL 结果不能为空: " + expression);
        }
        return value.toString();
    }
}
