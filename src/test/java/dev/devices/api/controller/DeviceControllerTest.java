package dev.devices.api.controller;

import dev.devices.api.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(roles = "ADMIN")
class DeviceControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ========================================
    // POST /api/devices
    // ========================================

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

    // ========================================
    // GET /api/devices/{id}
    // ========================================

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

    // ========================================
    // GET /api/devices - LIST & FILTER
    // ========================================

    @Test
    void shouldListAllDevices() throws Exception {
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldFilterDevicesByBrand() throws Exception {
        String brand = "Brand-" + UUID.randomUUID();
        createDeviceThroughApi("BrandFilterPhone", brand, "AVAILABLE");

        mockMvc.perform(get("/api/devices")
                        .param("brand", brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("BrandFilterPhone"))
                .andExpect(jsonPath("$[0].brand").value(brand));
    }

    @Test
    void shouldFilterDevicesByState() throws Exception {
        String brand = "StateTestBrand-" + UUID.randomUUID();
        createDeviceThroughApi("StateFilterPhone", brand, "INACTIVE");

        mockMvc.perform(get("/api/devices")
                        .param("state", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.state != 'INACTIVE')]").isEmpty())
                .andExpect(jsonPath("$[?(@.brand == '" + brand + "')]").isNotEmpty());
    }

    @Test
    void shouldFilterDevicesByBrandAndState() throws Exception {
        String brand = "CombinedBrand-" + UUID.randomUUID();

        createDeviceThroughApi("CombinedFilterPhone", brand, "AVAILABLE");
        createDeviceThroughApi("OtherStatePhone", brand, "INACTIVE");

        mockMvc.perform(get("/api/devices")
                        .param("brand", brand)
                        .param("state", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("CombinedFilterPhone"))
                .andExpect(jsonPath("$[0].state").value("AVAILABLE"));
    }

    @Test
    void shouldReturnEmptyListWhenNoDevicesMatch() throws Exception {
        String unknownBrand = "UnknownBrand-" + UUID.randomUUID();

        mockMvc.perform(get("/api/devices")
                        .param("brand", unknownBrand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturn400ForInvalidStateFilter() throws Exception {
        mockMvc.perform(get("/api/devices")
                        .param("state", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ========================================
    // ERROR FORMAT (RFC 9457 Problem Details)
    // ========================================

    @Test
    void shouldReturnFieldErrorsAsProblemDetail() throws Exception {
        String request = """
                {
                    "name": "",
                    "brand": "Apple",
                    "state": "AVAILABLE"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.instance").value("/api/devices"));
    }

    @Test
    void shouldReturnNotFoundAsProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/devices/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Device not found with id: " + id));
    }

    @Test
    void shouldNameFieldWhenPatchSendsExplicitNull() throws Exception {
        String id = createDeviceAndGetId("NullPatchPhone", "Brand-" + UUID.randomUUID());

        mockMvc.perform(patch("/api/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("must not be null"));
    }

    @Test
    void shouldNameFieldWhenPatchSendsReadOnlyField() throws Exception {
        String id = createDeviceAndGetId("ReadOnlyPatchPhone", "Brand-" + UUID.randomUUID());

        mockMvc.perform(patch("/api/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"createdAt\": \"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.createdAt").value("unknown or read-only field"));
    }

    @Test
    void shouldListAllowedValuesForInvalidState() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "iPhone", "brand": "Apple", "state": "BROKEN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.state").value("must be one of [AVAILABLE, IN_USE, INACTIVE]"));
    }

    @Test
    void shouldReportMalformedJson() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    // ========================================
    // INPUT NORMALIZATION
    // ========================================

    @Test
    void shouldTrimNameAndBrandAndMatchTrimmedBrandFilter() throws Exception {
        String brand = "TrimBrand-" + UUID.randomUUID();

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  Spaced Phone  ", "brand": "  %s  ", "state": "AVAILABLE"}
                                """.formatted(brand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spaced Phone"))
                .andExpect(jsonPath("$.brand").value(brand));

        mockMvc.perform(get("/api/devices")
                        .param("brand", " " + brand + " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ========================================
    // TEST HELPER
    // ========================================

    private String createDeviceAndGetId(String name, String brand) throws Exception {
        String response = mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "brand": "%s", "state": "AVAILABLE"}
                                """.formatted(name, brand)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private void createDeviceThroughApi(
            String name, String brand, String state) throws Exception {

        String request = """
                {
                    "name": "%s",
                    "brand": "%s",
                    "state": "%s"
                }
                """.formatted(name, brand, state);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }
}
