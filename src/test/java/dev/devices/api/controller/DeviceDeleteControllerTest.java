package dev.devices.api.controller;

import dev.devices.api.support.AbstractIntegrationTest;

import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(roles = "ADMIN")
class DeviceDeleteControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository repository;

    @Test
    void shouldDeleteAvailableDevice() throws Exception {
        Device device = createDevice(DeviceState.AVAILABLE);

        mockMvc.perform(delete("/api/devices/{id}", device.getId()))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(
                repository.existsById(device.getId())
        );
    }

    @Test
    void shouldDeleteInactiveDevice() throws Exception {
        Device device = createDevice(DeviceState.INACTIVE);

        mockMvc.perform(delete("/api/devices/{id}", device.getId()))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(
                repository.existsById(device.getId())
        );
    }

    @Test
    void shouldRejectDeletingDeviceInUse() throws Exception {
        Device device = createDevice(DeviceState.IN_USE);

        mockMvc.perform(delete("/api/devices/{id}", device.getId()))
                .andExpect(status().isConflict());

        org.junit.jupiter.api.Assertions.assertTrue(
                repository.existsById(device.getId())
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownDevice() throws Exception {
        mockMvc.perform(delete("/api/devices/{id}",
                        java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private Device createDevice(DeviceState state) {
        Device device = new Device("iPhone", "Apple", state);
        return repository.saveAndFlush(device);
    }
}
