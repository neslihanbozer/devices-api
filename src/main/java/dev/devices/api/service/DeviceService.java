package dev.devices.api.service;

import dev.devices.api.common.exception.DeviceNotFoundException;
import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.CreateDeviceRequest;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.dto.PatchDeviceRequest;
import dev.devices.api.dto.UpdateDeviceRequest;
import dev.devices.api.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DeviceResponse createDevice(CreateDeviceRequest request) {
        Device device = new Device(request.name(), request.brand(), request.state());

        Device saved = repository.saveAndFlush(device);
        log.info("Created device {}", saved.getId());

        return DeviceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(UUID id) {
        return DeviceResponse.from(findDevice(id));
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevices(String rawBrand, DeviceState state) {
        // Brands are stored trimmed; a blank brand (e.g. "?brand=") means "no brand filter".
        String brand = Device.normalize(rawBrand);
        boolean hasBrand = StringUtils.hasText(brand);

        List<Device> devices;
        if (hasBrand && state != null) {
            devices = repository.findByBrandAndState(brand, state);
        } else if (hasBrand) {
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
        Device device = findDevice(id);

        device.update(request.name(), request.brand(), request.state());

        return save(device);
    }

    @Transactional
    public DeviceResponse patchDevice(UUID id, PatchDeviceRequest request) {
        Device device = findDevice(id);

        device.update(
                request.getName() != null ? request.getName() : device.getName(),
                request.getBrand() != null ? request.getBrand() : device.getBrand(),
                request.getState() != null ? request.getState() : device.getState()
        );

        return save(device);
    }

    @Transactional
    public void deleteDevice(UUID id) {
        Device device = findDevice(id);

        device.ensureDeletable();

        repository.delete(device);
        // Flush here so a concurrent modification surfaces inside this method
        // as an OptimisticLockingFailureException (mapped to HTTP 409).
        repository.flush();
        log.info("Deleted device {}", id);
    }

    private Device findDevice(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));
    }

    private DeviceResponse save(Device device) {
        Device saved = repository.saveAndFlush(device);
        log.info("Updated device {}", saved.getId());
        return DeviceResponse.from(saved);
    }
}
