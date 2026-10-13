
package dev.devices.api.dto;

import dev.devices.api.domain.DeviceState;

public record PatchDeviceRequest(
        String name,
        String brand,
        DeviceState state
) {
}
