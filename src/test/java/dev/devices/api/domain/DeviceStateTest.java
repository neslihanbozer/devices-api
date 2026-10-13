package dev.devices.api.domain;

import dev.devices.api.domain.DeviceState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceStateTest {

    @Test
    void shouldSupportAvailableState() {
        DeviceState state = DeviceState.AVAILABLE;

        assertEquals("AVAILABLE", state.name());
    }
}