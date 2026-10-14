package dev.devices.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI devicesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Devices API")
                        .version("1.0.0")
                        .description(
                                "REST API for managing devices, including " +
                                        "creation, retrieval, filtering, updates, " +
                                        "and deletion with state-based restrictions. " +
                                        "Errors are returned as RFC 9457 Problem Details."
                        ))
                // Adds the "Authorize" button to Swagger UI.
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("VIEWER: read-only (GET). ADMIN: full access.")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }

    /**
     * Documents the security responses on every device endpoint:
     * 401 for all operations, 403 for write operations (VIEWER role).
     */
    @Bean
    public OpenApiCustomizer securityResponsesCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, item) -> {
            if (!path.startsWith("/api/")) {
                return;
            }
            item.readOperationsMap().forEach((method, operation) -> {
                operation.getResponses().addApiResponse("401",
                        new ApiResponse().description("Missing or invalid credentials"));
                if (method != PathItem.HttpMethod.GET) {
                    operation.getResponses().addApiResponse("403",
                            new ApiResponse().description("Requires the ADMIN role"));
                }
            });
        });
    }
}
