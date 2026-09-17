package com.knowledge.points;

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
@MapperScan({
        "com.knowledge.points.signin.dao.mapper",
        "com.knowledge.points.ranking.dao.mapper",
        "com.knowledge.points.season.dao.mapper"
})
@SpringBootApplication
public class PointsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointsApplication.class, args);
    }
}
