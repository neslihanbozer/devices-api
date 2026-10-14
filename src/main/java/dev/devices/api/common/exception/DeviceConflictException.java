package dev.devices.api.common.exception;

/**
 * Thrown when a request violates a device business rule (mapped to HTTP 409).
 */
public class DeviceConflictException extends RuntimeException {

    public DeviceConflictException(String message) {
        super(message);
    }
}
