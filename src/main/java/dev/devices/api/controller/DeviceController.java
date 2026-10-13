
package dev.devices.api.controller;

import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.CreateDeviceRequest;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.dto.PatchDeviceRequest;
import dev.devices.api.dto.UpdateDeviceRequest;
import dev.devices.api.service.DeviceService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(
            @Valid @RequestBody CreateDeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createDevice(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getDeviceById(id));
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getDevices(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) DeviceState state) {
        return ResponseEntity.ok(service.getDevices(brand, state));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeviceRequest request) {
        return ResponseEntity.ok(service.updateDevice(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DeviceResponse> patchDevice(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> fields) {

        for (String field : fields.keySet()) {
            if (!field.equals("name")
                    && !field.equals("brand")
                    && !field.equals("state")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown field: " + field
                );
            }
        }

        String name = optionalText(fields, "name");
        String brand = optionalText(fields, "brand");

        DeviceState state = null;

        if (fields.containsKey("state")) {
            Object value = fields.get("state");

            if (!(value instanceof String stateValue)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "state must be a valid string"
                );
            }

            try {
                state = DeviceState.valueOf(stateValue);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid device state: " + stateValue
                );
            }
        }

        PatchDeviceRequest request =
                new PatchDeviceRequest(name, brand, state);

        return ResponseEntity.ok(service.patchDevice(id, request));
    }

    private String optionalText(Map<String, Object> fields, String field) {
        if (!fields.containsKey(field)) {
            return null;
        }

        Object value = fields.get(field);

        if (!(value instanceof String text)
                || text.isBlank()
                || text.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " must be a non-blank string of at most 255 characters"
            );
        }

        return text;
    }
}
