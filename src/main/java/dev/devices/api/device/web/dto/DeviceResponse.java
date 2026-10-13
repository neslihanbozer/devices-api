
package dev.devices.api.device.web.dto;

import dev.devices.api.device.domain.Device;
import dev.devices.api.device.domain.DeviceState;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String name,
        String brand,
        DeviceState state,
        Instant createdAt
) {

    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getBrand(),
                device.getState(),
                device.getCreatedAt()
        );
    }
}
