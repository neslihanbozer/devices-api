
package dev.devices.api.controller;

import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.CreateDeviceRequest;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.dto.UpdateDeviceRequest;
import dev.devices.api.service.DeviceService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
        service.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
