
package dev.devices.api.service;

import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.PatchDeviceRequest;
import dev.devices.api.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DevicePatchServiceTest {

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
    void shouldUpdateOnlyName() {
        UUID id = UUID.randomUUID();
        Device device = device("iPhone", "Apple", DeviceState.AVAILABLE);

        when(repository.findById(id))
                .thenReturn(java.util.Optional.of(device));

        var request = new PatchDeviceRequest("iPhone 17", null, null);

        var response = service.patchDevice(id, request);

        assertEquals("iPhone 17", response.name());
        assertEquals("Apple", response.brand());
        assertEquals(DeviceState.AVAILABLE, response.state());
    }

    @Test
    void shouldUpdateOnlyState() {
        UUID id = UUID.randomUUID();
        Device device = device("iPhone", "Apple", DeviceState.AVAILABLE);

        when(repository.findById(id))
                .thenReturn(java.util.Optional.of(device));

        var request = new PatchDeviceRequest(null, null, DeviceState.INACTIVE);

        var response = service.patchDevice(id, request);

        assertEquals("iPhone", response.name());
        assertEquals("Apple", response.brand());
        assertEquals(DeviceState.INACTIVE, response.state());
    }

    @Test
    void shouldRejectNameChangeWhenInUse() {
        UUID id = UUID.randomUUID();
        Device device = device("iPhone", "Apple", DeviceState.IN_USE);

        when(repository.findById(id))
                .thenReturn(java.util.Optional.of(device));

        var request = new PatchDeviceRequest("New Name", null, null);

        assertThrows(
                IllegalStateException.class,
                () -> service.patchDevice(id, request)
        );

        verify(repository, never()).saveAndFlush(any(Device.class));
    }

    private Device device(String name, String brand, DeviceState state) {
        Device device = new Device();
        device.setName(name);
        device.setBrand(brand);
        device.setState(state);
        return device;
    }
}
