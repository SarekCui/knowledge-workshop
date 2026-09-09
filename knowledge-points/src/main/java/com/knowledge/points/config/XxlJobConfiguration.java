package com.knowledge.points.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "knowledge.xxl.enabled", havingValue = "true")
public class XxlJobConfiguration {
    @Bean
    XxlJobSpringExecutor xxlJobExecutor(@Value("${knowledge.xxl.admin-addresses}") String adminAddresses,
                                       @Value("${knowledge.xxl.access-token:default_token}") String token,
                                       @Value("${knowledge.xxl.app-name:knowledge-points}") String appName,
                                       @Value("${knowledge.xxl.port:9999}") int port) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(token);
        executor.setAppname(appName);
        executor.setPort(port);
        return executor;
    }
}
