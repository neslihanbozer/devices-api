
package dev.devices.api.controller;

import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DeviceDeleteControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

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
        Device device = new Device();
        device.setName("iPhone");
        device.setBrand("Apple");
        device.setState(state);
        return repository.saveAndFlush(device);
    }
}
