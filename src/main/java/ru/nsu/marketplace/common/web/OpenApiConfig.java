package ru.nsu.marketplace.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI marketplaceOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme));
    }

    @Bean
    OperationCustomizer documentPreAuthorizedOperations() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(PreAuthorize.class)
                    || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PreAuthorize.class)) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
            }

            return operation;
        };
    }
}
