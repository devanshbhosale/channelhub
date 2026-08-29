package com.devanshbhosale.channelhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * springdoc metadata plus a bearerAuth scheme so Swagger UI shows the
 * Authorize button and sends "Authorization: Bearer x" on try-it calls.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI channelHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChannelHub API")
                        .description("Reseller order & credit portal")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
