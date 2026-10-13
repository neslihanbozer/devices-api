
package dev.devices.api.controller;

import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.CreateDeviceRequest;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.dto.UpdateDeviceRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DeviceUpdateControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldUpdateDevice() throws Exception {
        DeviceResponse original = createDevice(
                "Old Name", "Apple", DeviceState.AVAILABLE
        );

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "New Name", "Samsung", DeviceState.INACTIVE
        );

        mockMvc.perform(put("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.brand").value("Samsung"))
                .andExpect(jsonPath("$.state").value("INACTIVE"));

        mockMvc.perform(get("/api/devices/{id}", original.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.brand").value("Samsung"))
                .andExpect(jsonPath("$.state").value("INACTIVE"));
    }

    @Test
    void shouldRejectNameChangeWhenInUse() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.IN_USE
        );

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "New iPhone", "Apple", DeviceState.IN_USE
        );

        mockMvc.perform(put("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/devices/{id}", original.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone"));
    }

    @Test
    void shouldRejectBrandChangeWhenInUse() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.IN_USE
        );

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "iPhone", "Samsung", DeviceState.AVAILABLE
        );

        mockMvc.perform(put("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/devices/{id}", original.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.state").value("IN_USE"));
    }

    @Test
    void shouldAllowStateChangeWhenInUse() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.IN_USE
        );

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(put("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("AVAILABLE"));
    }

    @Test
    void shouldReturn404ForUnknownDevice() throws Exception {
        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(put("/api/devices/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectMissingName() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        String request = """
                {
                  "brand": "Apple",
                  "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(put("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingBrand() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        String request = """
                {
                  "name": "iPhone",
                  "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(put("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingState() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        String request = """
                {
                  "name": "iPhone",
                  "brand": "Apple"
                }
                """;

        mockMvc.perform(put("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidState() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        String request = """
                {
                  "name": "iPhone",
                  "brand": "Apple",
                  "state": "BROKEN"
                }
                """;

        mockMvc.perform(put("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidDeviceId() throws Exception {
        String request = """
                {
                  "name": "iPhone",
                  "brand": "Apple",
                  "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(put("/api/devices/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPreserveCreatedAt() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        UpdateDeviceRequest request = new UpdateDeviceRequest(
                "Updated iPhone", "Apple", DeviceState.INACTIVE
        );

        String response = mockMvc.perform(
                        put("/api/devices/{id}", original.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        DeviceResponse updated =
                objectMapper.readValue(response, DeviceResponse.class);

        assertEquals(original.id(), updated.id());
        assertEquals(original.createdAt(), updated.createdAt());
    }

    private DeviceResponse createDevice(
            String name,
            String brand,
            DeviceState state
    ) throws Exception {

        CreateDeviceRequest request =
                new CreateDeviceRequest(name, brand, state);

        String response = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, DeviceResponse.class);
    }
}
