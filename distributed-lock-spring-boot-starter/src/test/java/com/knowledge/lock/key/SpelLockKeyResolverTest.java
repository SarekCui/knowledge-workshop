package com.knowledge.lock.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class SpelLockKeyResolverTest {

    private final SpelLockKeyResolver resolver = new SpelLockKeyResolver();

    @Test
    void resolvesMultipleMethodArgumentExpressions() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("execute", String.class, Long.class);

        assertThat(resolver.resolve(
                new String[] {"'kw:marketing:activity:' + #activityId", "'kw:user:' + #userId"},
                new SampleService(), method, new Object[] {"activity-1", 9L}))
                .containsExactly("kw:marketing:activity:activity-1", "kw:user:9");
    }

    @Test
    void rejectsMissingExpressions() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("execute", String.class, Long.class);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolver.resolve(new String[0], new SampleService(), method,
                        new Object[] {"activity-1", 9L}));
    }

    private static final class SampleService {

        @SuppressWarnings("unused")
        void execute(String activityId, Long userId) {
        }
    }
}
