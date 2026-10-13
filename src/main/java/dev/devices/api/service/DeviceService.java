package dev.devices.api.service;

import dev.devices.api.common.exception.DeviceConflictException;
import dev.devices.api.common.exception.DeviceNotFoundException;
import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import dev.devices.api.dto.CreateDeviceRequest;
import dev.devices.api.dto.DeviceResponse;
import dev.devices.api.dto.PatchDeviceRequest;
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

        return DeviceResponse.from(repository.saveAndFlush(device));
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(UUID id) {
        return DeviceResponse.from(findDevice(id));
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
        Device device = findDevice(id);

        return applyUpdate(
                device,
                request.name(),
                request.brand(),
                request.state()
        );
    }

    @Transactional
    public DeviceResponse patchDevice(UUID id, PatchDeviceRequest request) {
        Device device = findDevice(id);

        String name = request.getName() != null
                ? request.getName()
                : device.getName();

        String brand = request.getBrand() != null
                ? request.getBrand()
                : device.getBrand();

        DeviceState state = request.getState() != null
                ? request.getState()
                : device.getState();

        return applyUpdate(device, name, brand, state);
    }

    private Device findDevice(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));
    }

    private DeviceResponse applyUpdate(
            Device device,
            String name,
            String brand,
            DeviceState state) {

        validateChanges(device, name, brand);

        device.setName(name);
        device.setBrand(brand);
        device.setState(state);

        return DeviceResponse.from(repository.saveAndFlush(device));
    }

    private void validateChanges(
            Device device,
            String name,
            String brand) {

        boolean nameChanged =
                !Objects.equals(device.getName(), name);

        boolean brandChanged =
                !Objects.equals(device.getBrand(), brand);

        if (device.getState() == DeviceState.IN_USE
                && (nameChanged || brandChanged)) {

            throw new DeviceConflictException(
                    "Cannot change name or brand while device is IN_USE"
            );
        }
    }
}