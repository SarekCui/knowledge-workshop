package com.knowledge.lock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.lock.enums.LockType;
import com.knowledge.lock.exception.LockAcquisitionException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

class LockExecutionStrategiesTest {

    @Test
    void singleLockReleasesAfterBusinessFailure() throws InterruptedException {
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(client.getLock("order:1")).thenReturn(lock);
        when(lock.tryLock(1, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        SingleLockExecutionStrategy strategy = new SingleLockExecutionStrategy(new RedissonLockFactory(client));
        LockOptions options = new LockOptions(List.of("order:1"), LockType.REENTRANT, 1, 5, TimeUnit.SECONDS);

        assertThatThrownBy(() -> strategy.execute(options, () -> { throw new IllegalStateException("boom"); }))
                .isInstanceOf(IllegalStateException.class);
        verify(lock).unlock();
    }

    @Test
    void multiLockOrdersKeysAndRollsBackFirstLockWhenSecondFails() throws InterruptedException {
        RedissonClient client = mock(RedissonClient.class);
        RLock first = mock(RLock.class);
        RLock second = mock(RLock.class);
        when(client.getLock("activity:1")).thenReturn(first);
        when(client.getLock("user:9")).thenReturn(second);
        when(first.tryLock(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq(TimeUnit.NANOSECONDS))).thenReturn(true);
        when(second.tryLock(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq(TimeUnit.NANOSECONDS))).thenReturn(false);
        when(first.isHeldByCurrentThread()).thenReturn(true);

        OrderedMultiLockExecutionStrategy strategy =
                new OrderedMultiLockExecutionStrategy(new RedissonLockFactory(client));
        LockOptions options = new LockOptions(List.of("user:9", "activity:1"), LockType.REENTRANT,
                1, 5, TimeUnit.SECONDS);

        assertThatThrownBy(() -> strategy.execute(options, () -> "never"))
                .isInstanceOf(LockAcquisitionException.class);
        InOrder order = inOrder(client);
        order.verify(client).getLock("activity:1");
        order.verify(client).getLock("user:9");
        verify(first).unlock();
    }

    @Test
    void resolverSelectsByNumberOfKeys() {
        LockExecutionStrategy single = mock(LockExecutionStrategy.class);
        LockExecutionStrategy multi = mock(LockExecutionStrategy.class);
        LockOptions options = new LockOptions(List.of("a", "b"), LockType.FAIR, 1, 5, TimeUnit.SECONDS);
        when(single.supports(options)).thenReturn(false);
        when(multi.supports(options)).thenReturn(true);

        assertThat(new LockExecutionStrategyResolver(List.of(single, multi)).resolve(options)).isSameAs(multi);
    }

    @Test
    void interruptedAcquisitionRestoresInterruptFlag() throws InterruptedException {
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(client.getLock("order:2")).thenReturn(lock);
        when(lock.tryLock(1, 5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("stop"));
        SingleLockExecutionStrategy strategy = new SingleLockExecutionStrategy(new RedissonLockFactory(client));
        LockOptions options = new LockOptions(List.of("order:2"), LockType.REENTRANT, 1, 5, TimeUnit.SECONDS);

        try {
            assertThatThrownBy(() -> strategy.execute(options, () -> "never"))
                    .isInstanceOf(LockAcquisitionException.class)
                    .extracting(exception -> ((LockAcquisitionException) exception).getReason())
                    .isEqualTo(LockAcquisitionException.Reason.INTERRUPTED);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void unlockFailureDoesNotHideBusinessFailure() throws InterruptedException {
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(client.getLock("order:3")).thenReturn(lock);
        when(lock.tryLock(1, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("unlock failed")).when(lock).unlock();
        SingleLockExecutionStrategy strategy = new SingleLockExecutionStrategy(new RedissonLockFactory(client));
        LockOptions options = new LockOptions(List.of("order:3"), LockType.REENTRANT, 1, 5, TimeUnit.SECONDS);

        assertThatThrownBy(() -> strategy.execute(options, () -> { throw new IllegalArgumentException("business"); }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("business")
                .satisfies(exception -> assertThat(exception.getSuppressed())
                        .singleElement()
                        .isInstanceOf(IllegalStateException.class));
    }
}
