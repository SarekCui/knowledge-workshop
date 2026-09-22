package com.knowledge.agent;

import com.knowledge.agent.mention.config.AgentLearningFeignConfiguration;
import com.knowledge.api.iam.client.IamOAuthFeignClient;
import com.knowledge.api.learning.client.LearningCommentFeignClient;
import com.knowledge.security.config.ResourceServerConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableScheduling
@EnableMethodSecurity
@EnableFeignClients(clients = IamOAuthFeignClient.class)
@Import(ResourceServerConfiguration.class)
@MapperScan({"com.knowledge.agent.mention.dao.mapper", "com.knowledge.agent.conversation.dao.mapper"})
@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @EnableFeignClients(
            clients = LearningCommentFeignClient.class,
            defaultConfiguration = AgentLearningFeignConfiguration.class)
    static class LearningFeignClientRegistration {
    }
}
