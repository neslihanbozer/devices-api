package dev.devices.api.controller;

import dev.devices.api.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies HTTP Basic authentication and role-based authorization end to end,
 * using the real users configured in {@link AbstractIntegrationTest}.
 */
class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String DEVICE_JSON = """
            {"name": "iPhone 16", "brand": "Apple", "state": "AVAILABLE"}
            """;

    @Autowired
    private MockMvc mockMvc;

    // ---------- Authentication (401) ----------

    @Test
    void shouldRejectRequestWithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Devices API\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        mockMvc.perform(get("/api/devices")
                        .with(httpBasic(ADMIN_USERNAME, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownUser() throws Exception {
        mockMvc.perform(get("/api/devices")
                        .with(httpBasic("unknown", "password")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- VIEWER role: read-only ----------

    @Test
    void viewerCanListDevices() throws Exception {
        mockMvc.perform(get("/api/devices")
                        .with(httpBasic(VIEWER_USERNAME, VIEWER_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void viewerCanFetchSingleDevice() throws Exception {
        mockMvc.perform(get("/api/devices/{id}", UUID.randomUUID())
                        .with(httpBasic(VIEWER_USERNAME, VIEWER_PASSWORD)))
                .andExpect(status().isNotFound()); // authorized; the device simply does not exist
    }

    @Test
    void viewerCannotCreateDevice() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .with(httpBasic(VIEWER_USERNAME, VIEWER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEVICE_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void viewerCannotUpdateDevice() throws Exception {
        mockMvc.perform(put("/api/devices/{id}", UUID.randomUUID())
                        .with(httpBasic(VIEWER_USERNAME, VIEWER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEVICE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotPatchDevice() throws Exception {
        mockMvc.perform(patch("/api/devices/{id}", UUID.randomUUID())
                        .with(httpBasic(VIEWER_USERNAME, VIEWER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\": \"IN_USE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotDeleteDevice() throws Exception {
        mockMvc.perform(delete("/api/devices/{id}", UUID.randomUUID())
                        .with(httpBasic(VIEWER_USERNAME, VIEWER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    // ---------- ADMIN role: full access ----------

    @Test
    void adminCanCreateAndReadDevice() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEVICE_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/devices")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk());
    }

    // ---------- Public endpoints ----------

    @Test
    void documentationAndHealthArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void otherEndpointsAreDenied() throws Exception {
        mockMvc.perform(get("/actuator/env")
                        .with(httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden());
    }
}
