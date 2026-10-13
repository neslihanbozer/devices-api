
package dev.devices.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OpenApiDocumentationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;


    @Test
    void shouldExposeOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Devices API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.paths['/api/devices'].post").exists())
                .andExpect(jsonPath("$.paths['/api/devices'].get").exists())
                .andExpect(jsonPath("$.paths['/api/devices/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/devices/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/devices/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/devices/{id}'].delete").exists());
    }

    @Test
    void shouldDocumentDeleteResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/devices/{id}'].delete.responses['204']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/devices/{id}'].delete.responses['404']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/devices/{id}'].delete.responses['409']"
                ).exists());
    }

    @Test
    void shouldDocumentUpdateResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/devices/{id}'].put.responses['409']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/devices/{id}'].patch.responses['409']"
                ).exists());
    }
}
