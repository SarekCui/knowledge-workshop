package com.knowledge.agent.mention.config;

import com.knowledge.agent.mention.service.ServiceAccessTokenService;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/** Caller-side policy for the Agent's outbound service calls. */
public class AgentLearningFeignConfiguration {

    @Bean
    public RequestInterceptor learningAgentAuthorization(ServiceAccessTokenService tokenService) {
        return template -> {
            template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.accessToken());
        };
    }

    @Bean
    public Retryer agentFeignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
