
package dev.devices.api.device;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    void shouldStoreDeviceName() {
        Device device = new Device();
        device.setName("iPhone 16");

        assertEquals("iPhone 16", device.getName());
    }

    @Test
    void shouldStoreDeviceBrand() {
        Device device = new Device();
        device.setBrand("Apple");

        assertEquals("Apple", device.getBrand());
    }

    @Test
    void shouldStoreDeviceState() {
        Device device = new Device();
        device.setState(DeviceState.AVAILABLE);

        assertEquals(DeviceState.AVAILABLE, device.getState());
    }

    @Test
    void shouldInitializeCreationTimeBeforePersist() {
        Device device = new Device();

        assertNull(device.getCreatedAt());

        Instant before = Instant.now();

        device.onCreate();

        Instant after = Instant.now();

        assertNotNull(device.getCreatedAt());
        assertFalse(device.getCreatedAt().isBefore(before));
        assertFalse(device.getCreatedAt().isAfter(after));
    }

    @Test
    void shouldHaveNoIdBeforePersistence() {
        Device device = new Device();

        assertNull(device.getId());
    }

    @Test
    void shouldHaveNoVersionBeforePersistence() {
        Device device = new Device();

        assertNull(device.getVersion());
    }
}
