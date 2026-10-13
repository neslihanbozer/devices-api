
package dev.devices.api.dto;

import dev.devices.api.domain.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDeviceRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String brand,
        @NotNull DeviceState state
) {
}
