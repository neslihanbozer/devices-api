
package dev.devices.api.service;

import dev.devices.api.common.exception.DeviceConflictException;
import dev.devices.api.common.exception.DeviceNotFoundException;
import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DeviceDeleteServiceTest {

    private DeviceRepository repository;
    private DeviceService service;

    @BeforeEach
    void setUp() {
        repository = mock(DeviceRepository.class);
        service = new DeviceService(repository);
    }

    @Test
    void shouldDeleteAvailableDevice() {
        UUID id = UUID.randomUUID();
        Device device = device(DeviceState.AVAILABLE);

        when(repository.findById(id)).thenReturn(Optional.of(device));

        service.deleteDevice(id);

        verify(repository).delete(device);
    }

    @Test
    void shouldDeleteInactiveDevice() {
        UUID id = UUID.randomUUID();
        Device device = device(DeviceState.INACTIVE);

        when(repository.findById(id)).thenReturn(Optional.of(device));

        service.deleteDevice(id);

        verify(repository).delete(device);
    }

    @Test
    void shouldRejectDeletingDeviceInUse() {
        UUID id = UUID.randomUUID();
        Device device = device(DeviceState.IN_USE);

        when(repository.findById(id)).thenReturn(Optional.of(device));

        assertThrows(
                DeviceConflictException.class,
                () -> service.deleteDevice(id)
        );

        verify(repository, never()).delete(any(Device.class));
    }

    @Test
    void shouldReturnNotFoundForUnknownDevice() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(
                DeviceNotFoundException.class,
                () -> service.deleteDevice(id)
        );

        verify(repository, never()).delete(any(Device.class));
    }

    private Device device(DeviceState state) {
        Device device = new Device();
        device.setName("iPhone");
        device.setBrand("Apple");
        device.setState(state);
        return device;
    }
}
