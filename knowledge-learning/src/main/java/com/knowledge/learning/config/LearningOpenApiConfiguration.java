package com.knowledge.learning.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LearningOpenApiConfiguration {

    @Bean
    OpenAPI learningOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Knowledge Workshop Learning API")
                .description("课程、章节、Note、课程权益与视频学习进度接口")
                .version("1.0.0"));
    }

    @Bean
    OpenApiCustomizer learningRequestIdHeader() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name("X-Request-Id")
                        .required(false)
                        .description("可选请求追踪 ID；非法或缺失时由服务端生成"))));
    }
}
