package dev.devices.api.device;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class DeviceRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private DeviceRepository repository;

    @Test
    void shouldSaveDevice() {
        Device device = new Device();
        device.setName("iPhone 16");
        device.setBrand("Apple");
        device.setState(DeviceState.AVAILABLE);

        Device saved = repository.saveAndFlush(device);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getVersion());
    }

    @Test
    void shouldFindDeviceById() {
        Device device = new Device();
        device.setName("Samsung Galaxy S25");
        device.setBrand("Samsung");
        device.setState(DeviceState.AVAILABLE);

        Device saved = repository.saveAndFlush(device);

        Device found = repository.findById(saved.getId())
                .orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals("Samsung Galaxy S25", found.getName());
        assertEquals("Samsung", found.getBrand());
        assertEquals(DeviceState.AVAILABLE, found.getState());
    }

    @Test
    void shouldReturnEmptyWhenDeviceDoesNotExist() {
        UUID unknownId = UUID.randomUUID();

        Optional<Device> result = repository.findById(unknownId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindAllDevices() {
        Device iphone = new Device();
        iphone.setName("iPhone 16");
        iphone.setBrand("Apple");
        iphone.setState(DeviceState.AVAILABLE);

        Device samsung = new Device();
        samsung.setName("Galaxy S25");
        samsung.setBrand("Samsung");
        samsung.setState(DeviceState.IN_USE);

        Device savedIphone = repository.saveAndFlush(iphone);
        Device savedSamsung = repository.saveAndFlush(samsung);

        List<Device> devices = repository.findAll();

        assertTrue(devices.stream()
                .anyMatch(d -> d.getId().equals(savedIphone.getId())));

        assertTrue(devices.stream()
                .anyMatch(d -> d.getId().equals(savedSamsung.getId())));
    }

    @Test
    void shouldFindDevicesByBrand() {
        Device iphone = new Device();
        iphone.setName("iPhone 16");
        iphone.setBrand("Apple");
        iphone.setState(DeviceState.AVAILABLE);

        Device samsung = new Device();
        samsung.setName("Galaxy S25");
        samsung.setBrand("Samsung");
        samsung.setState(DeviceState.AVAILABLE);

        Device savedIphone = repository.saveAndFlush(iphone);
        repository.saveAndFlush(samsung);

        List<Device> result = repository.findByBrand("Apple");

        assertTrue(result.stream()
                .anyMatch(d -> d.getId().equals(savedIphone.getId())));

        assertTrue(result.stream()
                .allMatch(d -> d.getBrand().equals("Apple")));
    }

    @Test
    void shouldFindDevicesByState() {
        Device available = new Device();
        available.setName("iPhone 16");
        available.setBrand("Apple");
        available.setState(DeviceState.AVAILABLE);

        Device inUse = new Device();
        inUse.setName("Galaxy S25");
        inUse.setBrand("Samsung");
        inUse.setState(DeviceState.IN_USE);

        Device savedAvailable = repository.saveAndFlush(available);
        repository.saveAndFlush(inUse);

        List<Device> result = repository.findByState(DeviceState.AVAILABLE);

        assertTrue(result.stream()
                .anyMatch(d -> d.getId().equals(savedAvailable.getId())));

        assertTrue(result.stream()
                .allMatch(d -> d.getState() == DeviceState.AVAILABLE));
    }

    @Test
    void shouldFindDevicesByBrandAndState() {
        Device availableApple = new Device();
        availableApple.setName("iPhone 16");
        availableApple.setBrand("Apple");
        availableApple.setState(DeviceState.AVAILABLE);

        Device inUseApple = new Device();
        inUseApple.setName("iPad");
        inUseApple.setBrand("Apple");
        inUseApple.setState(DeviceState.IN_USE);

        Device availableSamsung = new Device();
        availableSamsung.setName("Galaxy S25");
        availableSamsung.setBrand("Samsung");
        availableSamsung.setState(DeviceState.AVAILABLE);

        Device saved = repository.saveAndFlush(availableApple);
        repository.saveAndFlush(inUseApple);
        repository.saveAndFlush(availableSamsung);

        List<Device> result = repository.findByBrandAndState(
                "Apple", DeviceState.AVAILABLE);

        assertTrue(result.stream()
                .anyMatch(d -> d.getId().equals(saved.getId())));

        assertTrue(result.stream()
                .allMatch(d -> d.getBrand().equals("Apple")
                        && d.getState() == DeviceState.AVAILABLE));
    }
}
