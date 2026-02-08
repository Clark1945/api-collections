package com.example.userapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User API")
                        .description("User CRUD API with role management and audit logging")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("API Support")));
    }
}
