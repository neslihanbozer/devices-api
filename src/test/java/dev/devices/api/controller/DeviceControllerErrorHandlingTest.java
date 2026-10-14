package dev.devices.api.controller;

import dev.devices.api.common.exception.DeviceConflictException;
import dev.devices.api.config.SecurityConfig;
import dev.devices.api.domain.Device;
import dev.devices.api.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer slice test (no database): verifies how exceptions thrown by the
 * service are translated into HTTP responses.
 */
@WebMvcTest(DeviceController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "devices.security.admin.username=admin",
        "devices.security.admin.password=admin-password",
        "devices.security.viewer.username=viewer",
        "devices.security.viewer.password=viewer-password"
})
@WithMockUser(roles = "ADMIN")
class DeviceControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService service;

    @Test
    void shouldReturn409WhenUpdateHitsConcurrentModification() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.updateDevice(eq(id), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Device.class, id));

        mockMvc.perform(put("/api/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "iPhone", "brand": "Apple", "state": "AVAILABLE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Concurrent modification"));
    }

    @Test
    void shouldReturn409WhenDeleteHitsConcurrentModification() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new ObjectOptimisticLockingFailureException(Device.class, id))
                .when(service).deleteDevice(id);

        mockMvc.perform(delete("/api/devices/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent modification"));
    }

    @Test
    void shouldReturn409ForBusinessRuleViolation() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new DeviceConflictException("Cannot delete a device while it is IN_USE"))
                .when(service).deleteDevice(id);

        mockMvc.perform(delete("/api/devices/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Business rule violation"))
                .andExpect(jsonPath("$.detail").value("Cannot delete a device while it is IN_USE"));
    }
}
