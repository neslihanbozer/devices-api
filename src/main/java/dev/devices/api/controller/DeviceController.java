
package dev.devices.api.controller;

import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.CreateDeviceRequest;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.dto.PatchDeviceRequest;
import dev.devices.api.dto.UpdateDeviceRequest;
import dev.devices.api.service.DeviceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "Device management operations")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @Operation(
            summary = "Create a device",
            description = "Creates a new device with a name, brand, and initial state."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Device created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid device data")
    })
    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(
            @Valid @RequestBody CreateDeviceRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createDevice(request));
    }

    @Operation(
            summary = "Get a device by ID",
            description = "Retrieves a device using its UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device found"),
            @ApiResponse(responseCode = "400", description = "Invalid device ID"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(service.getDeviceById(id));
    }

    @Operation(
            summary = "List and filter devices",
            description = "Retrieves all devices, optionally filtered by brand, state, or both. "
                    + "Brand matching is exact and case-sensitive."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devices retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid state filter")
    })
    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getDevices(
            @Parameter(description = "Exact brand name", example = "Apple")
            @RequestParam(required = false) String brand,
            @Parameter(description = "Device state")
            @RequestParam(required = false) DeviceState state) {

        return ResponseEntity.ok(service.getDevices(brand, state));
    }

    @Operation(
            summary = "Update a device",
            description = "Replaces the device's editable fields. Name and brand cannot be changed while the device is IN_USE. The creation timestamp remains unchanged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid device data"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "409", description = "Cannot change name or brand while device is IN_USE")
    })
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeviceRequest request) {

        return ResponseEntity.ok(service.updateDevice(id, request));
    }

    @Operation(
            summary = "Partially update a device",
            description = "Updates only the provided fields. Name and brand cannot be changed while the device is IN_USE. Explicit null values and unknown fields are rejected."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid patch data"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "409", description = "Cannot change name or brand while device is IN_USE")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<DeviceResponse> patchDevice(
            @PathVariable UUID id,
            @Valid @RequestBody PatchDeviceRequest request) {

        return ResponseEntity.ok(service.patchDevice(id, request));
    }

    @Operation(
            summary = "Delete a device",
            description = "Deletes a device unless its current state is IN_USE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete a device while it is IN_USE")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
        service.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
