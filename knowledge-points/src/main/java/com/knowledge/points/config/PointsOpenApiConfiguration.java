package com.knowledge.points.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PointsOpenApiConfiguration {

    @Bean
    OpenAPI pointsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Knowledge Workshop Points API")
                .description("知识工坊签到、积分与赛季排行榜接口")
                .version("1.0.0")
                .contact(new Contact().name("Knowledge Workshop")));
    }

    @Bean
    OpenApiCustomizer pointsRequestIdHeader() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name("X-Request-Id")
                        .required(false)
                        .description("可选请求追踪 ID；缺失、空白或超过 64 字符时由服务端生成")
                        .example("request-20260903-001"))));
    }
}
