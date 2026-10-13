
package dev.devices.api.service;

import dev.devices.api.common.exception.DeviceConflictException;
import dev.devices.api.common.exception.DeviceNotFoundException;
import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.CreateDeviceRequest;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.dto.UpdateDeviceRequest;
import dev.devices.api.repository.DeviceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DeviceResponse createDevice(CreateDeviceRequest request) {
        Device device = new Device();
        device.setName(request.name());
        device.setBrand(request.brand());
        device.setState(request.state());

        Device saved = repository.saveAndFlush(device);
        return DeviceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(UUID id) {
        Device device = repository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));

        return DeviceResponse.from(device);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevices(String brand, DeviceState state) {
        List<Device> devices;

        if (brand != null && state != null) {
            devices = repository.findByBrandAndState(brand, state);
        } else if (brand != null) {
            devices = repository.findByBrand(brand);
        } else if (state != null) {
            devices = repository.findByState(state);
        } else {
            devices = repository.findAll();
        }

        return devices.stream()
                .map(DeviceResponse::from)
                .toList();
    }

    @Transactional
    public DeviceResponse updateDevice(UUID id, UpdateDeviceRequest request) {
        Device device = repository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));

        boolean nameChanged = !Objects.equals(device.getName(), request.name());
        boolean brandChanged = !Objects.equals(device.getBrand(), request.brand());

        if (device.getState() == DeviceState.IN_USE
                && (nameChanged || brandChanged)) {
            throw new DeviceConflictException(
                    "Cannot change name or brand while device is IN_USE"
            );
        }

        device.setName(request.name());
        device.setBrand(request.brand());
        device.setState(request.state());

        Device saved = repository.saveAndFlush(device);
        return DeviceResponse.from(saved);
    }
}
