package com.knowledge.agent.mention.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.knowledge.api.iam.client.IamOAuthFeignClient;
import com.knowledge.api.iam.dto.OAuth2AccessTokenDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ServiceAccessTokenServiceTest {

    @Test
    void reusesValidClientCredentialsTokenInsteadOfCallingIamForEveryRequest() {
        AtomicInteger requests = new AtomicInteger();
        IamOAuthFeignClient client = (authorization, formBody) -> {
            assertThat(authorization).isEqualTo("Basic a25vd2xlZGdlLWFnZW50OmFnZW50LXNlY3JldA==");
            assertThat(formBody).isEqualTo(
                    "grant_type=client_credentials&scope=learning.comment.read+learning.comment.write");
            requests.incrementAndGet();
            return new OAuth2AccessTokenDTO("iam-token", "Bearer", 300,
                    "learning.comment.read learning.comment.write");
        };
        ServiceAccessTokenService service = new ServiceAccessTokenService();
        ReflectionTestUtils.setField(service, "iamOAuthFeignClient", client);
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "clientId", "knowledge-agent");
        ReflectionTestUtils.setField(service, "clientSecret", "agent-secret");

        assertThat(service.accessToken()).isEqualTo("iam-token");
        assertThat(service.accessToken()).isEqualTo("iam-token");

        assertThat(requests).hasValue(1);
    }
}
