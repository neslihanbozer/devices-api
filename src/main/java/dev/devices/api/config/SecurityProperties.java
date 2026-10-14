package dev.devices.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credentials of the two API users, bound from {@code devices.security.*}.
 * Validation makes the application fail fast if a credential is missing.
 */
@Validated
@ConfigurationProperties(prefix = "devices.security")
public record SecurityProperties(
        @Valid @NotNull User admin,
        @Valid @NotNull User viewer
) {

    public record User(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }
}
