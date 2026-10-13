
package dev.devices.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------
    // POST /api/devices
    // -------------------------

    @Test
    void shouldCreateDevice() throws Exception {
        String request = """
                {
                    "name": "iPhone 16",
                    "brand": "Apple",
                    "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("iPhone 16"))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldRejectMissingName() throws Exception {
        String request = """
                {
                    "brand": "Apple",
                    "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        String request = """
                {
                    "name": "   ",
                    "brand": "Apple",
                    "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingBrand() throws Exception {
        String request = """
                {
                    "name": "iPhone 16",
                    "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankBrand() throws Exception {
        String request = """
                {
                    "name": "iPhone 16",
                    "brand": "",
                    "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingState() throws Exception {
        String request = """
                {
                    "name": "iPhone 16",
                    "brand": "Apple"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidState() throws Exception {
        String request = """
                {
                    "name": "iPhone 16",
                    "brand": "Apple",
                    "state": "BROKEN"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNameLongerThan255Characters() throws Exception {
        String longName = "A".repeat(256);

        String request = """
                {
                    "name": "%s",
                    "brand": "Apple",
                    "state": "AVAILABLE"
                }
                """.formatted(longName);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBrandLongerThan255Characters() throws Exception {
        String longBrand = "A".repeat(256);

        String request = """
                {
                    "name": "iPhone 16",
                    "brand": "%s",
                    "state": "AVAILABLE"
                }
                """.formatted(longBrand);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        String request = """
                {
                    "name": "iPhone 16",
                    "brand":
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    // -------------------------
    // GET /api/devices/{id}
    // -------------------------

    @Test
    void shouldGetDeviceById() throws Exception {
        String request = """
                {
                    "name": "Samsung Galaxy S25",
                    "brand": "Samsung",
                    "state": "AVAILABLE"
                }
                """;

        String response = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(response)
                .get("id")
                .asText();

        mockMvc.perform(get("/api/devices/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Samsung Galaxy S25"))
                .andExpect(jsonPath("$.brand").value("Samsung"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn404WhenDeviceNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/devices/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForInvalidDeviceId() throws Exception {
        mockMvc.perform(get("/api/devices/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }
}
