package dev.devices.api.service;

import dev.devices.api.common.exception.DeviceConflictException;
import dev.devices.api.common.exception.DeviceNotFoundException;
import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.dto.UpdateDeviceRequest;
import dev.devices.api.repository.DeviceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceUpdateServiceTest {

    private DeviceRepository repository;
    private DeviceService service;

    @BeforeEach
    void setUp() {
        repository = mock(DeviceRepository.class);
        service = new DeviceService(repository);

        when(repository.saveAndFlush(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldUpdateAvailableDevice() {
        UUID id = UUID.randomUUID();
        Device device = device("Old Name", "Apple", DeviceState.AVAILABLE);

        when(repository.findById(id)).thenReturn(Optional.of(device));

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "New Name", "Samsung", DeviceState.INACTIVE
        );

        DeviceResponse response = service.updateDevice(id, request);

        assertEquals("New Name", response.name());
        assertEquals("Samsung", response.brand());
        assertEquals(DeviceState.INACTIVE, response.state());
    }

    @Test
    void shouldUpdateInactiveDevice() {
        UUID id = UUID.randomUUID();
        Device device = device("Old Name", "Apple", DeviceState.INACTIVE);

        when(repository.findById(id)).thenReturn(Optional.of(device));

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "New Name", "Samsung", DeviceState.AVAILABLE
        );

        DeviceResponse response = service.updateDevice(id, request);

        assertEquals("New Name", response.name());
        assertEquals("Samsung", response.brand());
        assertEquals(DeviceState.AVAILABLE, response.state());
    }

    @Test
    void shouldRejectNameChangeWhenDeviceInUse() {
        UUID id = UUID.randomUUID();
        Device device = device("iPhone", "Apple", DeviceState.IN_USE);

        when(repository.findById(id)).thenReturn(Optional.of(device));

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "New iPhone", "Apple", DeviceState.IN_USE
        );

        assertThrows(
                DeviceConflictException.class,
                () -> service.updateDevice(id, request)
        );

        assertEquals("iPhone", device.getName());
    }

    @Test
    void shouldRejectBrandChangeWhenDeviceInUse() {
        UUID id = UUID.randomUUID();
        Device device = device("iPhone", "Apple", DeviceState.IN_USE);

        when(repository.findById(id)).thenReturn(Optional.of(device));

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "iPhone", "Samsung", DeviceState.AVAILABLE
        );

        assertThrows(
                DeviceConflictException.class,
                () -> service.updateDevice(id, request)
        );

        assertEquals("Apple", device.getBrand());
        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    void shouldAllowStateChangeWhenDeviceInUse() {
        UUID id = UUID.randomUUID();
        Device device = device("iPhone", "Apple", DeviceState.IN_USE);

        when(repository.findById(id)).thenReturn(Optional.of(device));

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        DeviceResponse response = service.updateDevice(id, request);

        assertEquals("iPhone", response.name());
        assertEquals("Apple", response.brand());
        assertEquals(DeviceState.AVAILABLE, response.state());
    }

    @Test
    void shouldReturnNotFoundForUnknownDevice() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        assertThrows(
                DeviceNotFoundException.class,
                () -> service.updateDevice(id, request)
        );
    }

    private Device device(String name, String brand, DeviceState state) {
        Device device = new Device(name, brand, state);
        return device;
    }
}
