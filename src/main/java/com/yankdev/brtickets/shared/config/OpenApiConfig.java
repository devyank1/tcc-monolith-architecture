package com.yankdev.brtickets.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI api = new OpenAPI();

        api.info(new Info()
                .title("BRTickets Monolith API")
                .version("1.0.0")
                .description("API documentation for BRTickets Monolith Application TCC"));

        api.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(
                new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
        );

        return api;
    }
}
