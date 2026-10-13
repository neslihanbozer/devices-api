
package dev.devices.api.common.exception;

import java.util.UUID;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(UUID id) {
        super("Device not found with id: " + id);
    }
}
