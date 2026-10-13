
package dev.devices.api.service;

import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.CreateDeviceRequest;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.repository.DeviceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.devices.api.common.exception.DeviceNotFoundException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeviceServiceTest {

    private DeviceRepository repository;
    private DeviceService service;

    @BeforeEach
    void setUp() {
        repository = mock(DeviceRepository.class);
        service = new DeviceService(repository);
    }

    @Test
    void shouldCreateDevice() {
        CreateDeviceRequest request = new CreateDeviceRequest(
                "iPhone 16",
                "Apple",
                DeviceState.AVAILABLE
        );

        when(repository.saveAndFlush(any(Device.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeviceResponse response = service.createDevice(request);

        ArgumentCaptor<Device> captor =
                ArgumentCaptor.forClass(Device.class);

        verify(repository).saveAndFlush(captor.capture());

        Device savedDevice = captor.getValue();

        assertAll(
                () -> assertEquals("iPhone 16", savedDevice.getName()),
                () -> assertEquals("Apple", savedDevice.getBrand()),
                () -> assertEquals(DeviceState.AVAILABLE, savedDevice.getState()),
                () -> assertEquals("iPhone 16", response.name()),
                () -> assertEquals("Apple", response.brand()),
                () -> assertEquals(DeviceState.AVAILABLE, response.state())
        );

        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldPropagateRepositoryFailure() {
        CreateDeviceRequest request = new CreateDeviceRequest(
                "iPhone 16",
                "Apple",
                DeviceState.AVAILABLE
        );

        when(repository.saveAndFlush(any(Device.class)))
                .thenThrow(
                        new IllegalStateException("Database unavailable")
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createDevice(request)
        );

        assertEquals("Database unavailable", exception.getMessage());

        verify(repository).saveAndFlush(any(Device.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldGetDeviceById() {
        UUID id = UUID.randomUUID();

        Device device = new Device();
        device.setName("iPhone 16");
        device.setBrand("Apple");
        device.setState(DeviceState.AVAILABLE);

        when(repository.findById(id))
                .thenReturn(Optional.of(device));

        DeviceResponse response = service.getDeviceById(id);

        assertEquals("iPhone 16", response.name());
        assertEquals("Apple", response.brand());
        assertEquals(DeviceState.AVAILABLE, response.state());

        verify(repository).findById(id);
    }

    @Test
    void shouldThrowExceptionWhenDeviceNotFound() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                DeviceNotFoundException.class,
                () -> service.getDeviceById(id)
        );

        verify(repository).findById(id);
    }
}
