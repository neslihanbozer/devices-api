package dev.devices.api.domain;

import dev.devices.api.common.exception.DeviceConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    void shouldCreateDeviceWithGivenValues() {
        Device device = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        assertEquals("iPhone 16", device.getName());
        assertEquals("Apple", device.getBrand());
        assertEquals(DeviceState.AVAILABLE, device.getState());
        assertNull(device.getId());
        assertNull(device.getVersion());
        assertNull(device.getCreatedAt());
    }

    @Test
    void shouldInitializeCreationTimeBeforePersist() {
        Device device = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);
        device.onCreate();
        Instant after = Instant.now();

        assertNotNull(device.getCreatedAt());
        assertFalse(device.getCreatedAt().isBefore(before));
        assertFalse(device.getCreatedAt().isAfter(after));
    }

    @ParameterizedTest
    @EnumSource(value = DeviceState.class, names = {"AVAILABLE", "INACTIVE"})
    void shouldUpdateAllFieldsWhenNotInUse(DeviceState initialState) {
        Device device = new Device("iPhone", "Apple", initialState);

        device.update("Galaxy", "Samsung", DeviceState.IN_USE);

        assertEquals("Galaxy", device.getName());
        assertEquals("Samsung", device.getBrand());
        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    void shouldAllowStateChangeWhenInUse() {
        Device device = new Device("iPhone", "Apple", DeviceState.IN_USE);

        device.update("iPhone", "Apple", DeviceState.AVAILABLE);

        assertEquals(DeviceState.AVAILABLE, device.getState());
    }

    @Test
    void shouldRejectNameChangeWhenInUse() {
        Device device = new Device("iPhone", "Apple", DeviceState.IN_USE);

        assertThrows(DeviceConflictException.class,
                () -> device.update("iPhone Pro", "Apple", DeviceState.IN_USE));

        assertEquals("iPhone", device.getName());
    }

    @Test
    void shouldRejectBrandChangeWhenInUse() {
        Device device = new Device("iPhone", "Apple", DeviceState.IN_USE);

        assertThrows(DeviceConflictException.class,
                () -> device.update("iPhone", "Samsung", DeviceState.IN_USE));

        assertEquals("Apple", device.getBrand());
    }

    @Test
    void shouldEvaluateRuleAgainstStateBeforeUpdate() {
        Device device = new Device("iPhone", "Apple", DeviceState.IN_USE);

        // Releasing the device and renaming it in the same request is rejected.
        assertThrows(DeviceConflictException.class,
                () -> device.update("iPhone Pro", "Apple", DeviceState.AVAILABLE));

        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    void shouldTrimNameAndBrand() {
        Device device = new Device("  iPhone 16  ", " Apple ", DeviceState.AVAILABLE);

        assertEquals("iPhone 16", device.getName());
        assertEquals("Apple", device.getBrand());

        device.update(" iPhone 17 ", "  Apple", DeviceState.AVAILABLE);

        assertEquals("iPhone 17", device.getName());
        assertEquals("Apple", device.getBrand());
    }

    @Test
    void shouldTreatWhitespaceOnlyDifferenceAsNoChangeWhenInUse() {
        Device device = new Device("iPhone", "Apple", DeviceState.IN_USE);

        assertDoesNotThrow(() -> device.update(" iPhone ", "Apple  ", DeviceState.AVAILABLE));
        assertEquals(DeviceState.AVAILABLE, device.getState());
    }

    @ParameterizedTest
    @EnumSource(value = DeviceState.class, names = {"AVAILABLE", "INACTIVE"})
    void shouldBeDeletableWhenNotInUse(DeviceState state) {
        Device device = new Device("iPhone", "Apple", state);

        assertDoesNotThrow(device::ensureDeletable);
    }

    @Test
    void shouldNotBeDeletableWhenInUse() {
        Device device = new Device("iPhone", "Apple", DeviceState.IN_USE);

        assertThrows(DeviceConflictException.class, device::ensureDeletable);
    }
}
