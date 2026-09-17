package com.knowledge.lock.config;

import com.knowledge.lock.aspect.DistributedLockAspect;
import com.knowledge.lock.core.LockExecutionStrategy;
import com.knowledge.lock.core.LockExecutionStrategyResolver;
import com.knowledge.lock.core.OrderedMultiLockExecutionStrategy;
import com.knowledge.lock.core.RedissonLockFactory;
import com.knowledge.lock.core.SingleLockExecutionStrategy;
import com.knowledge.lock.key.LockKeyResolver;
import com.knowledge.lock.key.SpelLockKeyResolver;
import java.util.List;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnBean(RedissonClient.class)
public class DistributedLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RedissonLockFactory redissonLockFactory(RedissonClient redissonClient) {
        return new RedissonLockFactory(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean
    SingleLockExecutionStrategy singleLockExecutionStrategy(RedissonLockFactory lockFactory) {
        return new SingleLockExecutionStrategy(lockFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    OrderedMultiLockExecutionStrategy orderedMultiLockExecutionStrategy(RedissonLockFactory lockFactory) {
        return new OrderedMultiLockExecutionStrategy(lockFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    LockExecutionStrategyResolver lockExecutionStrategyResolver(List<LockExecutionStrategy> strategies) {
        return new LockExecutionStrategyResolver(strategies);
    }

    @Bean
    @ConditionalOnMissingBean
    LockKeyResolver lockKeyResolver() {
        return new SpelLockKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    DistributedLockAspect distributedLockAspect(
            LockExecutionStrategyResolver strategyResolver, LockKeyResolver lockKeyResolver) {
        return new DistributedLockAspect(strategyResolver, lockKeyResolver);
    }
}
