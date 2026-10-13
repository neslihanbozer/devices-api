
package dev.devices.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI devicesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Devices API")
                        .version("1.0.0")
                        .description(
                                "REST API for managing devices, including " +
                                        "creation, retrieval, filtering, updates, " +
                                        "and deletion with state-based restrictions."
                        ));
    }
}
