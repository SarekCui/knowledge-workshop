package com.knowledge.marketing;

import com.knowledge.security.config.ResourceServerConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableScheduling
@EnableMethodSecurity
@Import(ResourceServerConfiguration.class)
@MapperScan({"com.knowledge.marketing.groupbuy.dao.mapper", "com.knowledge.marketing.notification.dao.mapper"})
@SpringBootApplication
public class MarketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketingApplication.class, args);
    }
}
