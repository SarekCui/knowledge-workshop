package com.knowledge.learning;

import com.knowledge.security.config.ResourceServerConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableMethodSecurity
@EnableScheduling
@Import(ResourceServerConfiguration.class)
@MapperScan({
        "com.knowledge.learning.course.dao.mapper",
        "com.knowledge.learning.note.dao.mapper",
        "com.knowledge.learning.progress.dao.mapper",
        "com.knowledge.learning.entitlement.dao.mapper"
})
@SpringBootApplication
public class LearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningApplication.class, args);
    }
}
