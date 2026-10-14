package dev.devices.api.controller;

import dev.devices.api.support.AbstractIntegrationTest;

import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.DeviceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(roles = "ADMIN")
class DevicePatchControllerTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void shouldPatchOnlyName() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"iPhone 17"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.name").value("iPhone 17"))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdAt")
                        .value(original.createdAt().toString()));
    }

    @Test
    void shouldPatchOnlyState() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone"))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.state").value("INACTIVE"));
    }

    @Test
    void shouldRejectNameChangeWhenInUse() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.IN_USE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Name"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAllowStateChangeWhenInUse() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.IN_USE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"AVAILABLE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone"))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"));
    }

    @Test
    void shouldReturn404ForUnknownDevice() throws Exception {
        mockMvc.perform(patch("/api/devices/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAcceptEmptyPatchWithoutChangingDevice() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.name").value("iPhone"))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdAt")
                        .value(original.createdAt().toString()));
    }

    @Test
    void shouldRejectExplicitNullName() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidState() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"BROKEN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBrandChangeWhenInUse() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.IN_USE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"brand":"Samsung"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectExplicitNullBrand() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"brand":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectExplicitNullState() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnknownField() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"createdAt":"2020-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectSimultaneousNameAndStateChangeWhenInUse()
            throws Exception {

        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.IN_USE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"New Name",
                                  "state":"AVAILABLE"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectNameLongerThan255Characters() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        String longName = "A".repeat(256);

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(longName)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBrandLongerThan255Characters() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        String longBrand = "B".repeat(256);

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"brand":"%s"}
                                """.formatted(longBrand)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPersistPatchedValues() throws Exception {
        DeviceResponse original = createDevice(
                "iPhone", "Apple", DeviceState.AVAILABLE
        );

        mockMvc.perform(patch("/api/devices/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Updated iPhone",
                                  "state":"INACTIVE"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/devices/{id}", original.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.name").value("Updated iPhone"))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.state").value("INACTIVE"))
                .andExpect(jsonPath("$.createdAt")
                        .value(original.createdAt().toString()));
    }

    private DeviceResponse createDevice(
            String name,
            String brand,
            DeviceState state
    ) throws Exception {

        String body = objectMapper.writeValueAsString(
                new CreateRequest(name, brand, state)
        );

        String response = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, DeviceResponse.class);
    }

    private record CreateRequest(
            String name,
            String brand,
            DeviceState state
    ) {
    }
}
