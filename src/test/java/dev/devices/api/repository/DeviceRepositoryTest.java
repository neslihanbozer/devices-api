package dev.devices.api.repository;

import dev.devices.api.support.AbstractIntegrationTest;

import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class DeviceRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private DeviceRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveDevice() {
        Device device = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Device saved = repository.saveAndFlush(device);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getVersion());
    }

    @Test
    void shouldFindDeviceById() {
        Device device = new Device("Samsung Galaxy S25", "Samsung", DeviceState.AVAILABLE);

        Device saved = repository.saveAndFlush(device);
        UUID deviceId = saved.getId();

        entityManager.clear();

        Device found = repository.findById(deviceId)
                .orElseThrow();

        assertEquals(deviceId, found.getId());
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
        Device iphone = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Device samsung = new Device("Galaxy S25", "Samsung", DeviceState.IN_USE);

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
        Device iphone = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Device samsung = new Device("Galaxy S25", "Samsung", DeviceState.AVAILABLE);

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
        Device available = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Device inUse = new Device("Galaxy S25", "Samsung", DeviceState.IN_USE);

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
        Device availableApple = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Device inUseApple = new Device("iPad", "Apple", DeviceState.IN_USE);

        Device availableSamsung = new Device("Galaxy S25", "Samsung", DeviceState.AVAILABLE);

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

    @Test
    void shouldUpdateExistingDevice() {
        Device device = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Device saved = repository.saveAndFlush(device);
        UUID originalId = saved.getId();

        // Read the original timestamp from PostgreSQL.
        entityManager.clear();

        Device persisted = repository.findById(originalId)
                .orElseThrow();

        Instant originalCreatedAt = persisted.getCreatedAt();
        Long originalVersion = persisted.getVersion();

        persisted.update("iPhone 16 Pro", "Apple", DeviceState.IN_USE);

        repository.saveAndFlush(persisted);

        // Force a fresh read from PostgreSQL.
        entityManager.clear();

        Device updated = repository.findById(originalId)
                .orElseThrow();

        assertEquals("iPhone 16 Pro", updated.getName());
        assertEquals("Apple", updated.getBrand());
        assertEquals(DeviceState.IN_USE, updated.getState());
        assertEquals(originalId, updated.getId());
        assertEquals(originalCreatedAt, updated.getCreatedAt());

        // Optimistic locking version must increase.
        assertEquals(
                originalVersion.longValue() + 1,
                updated.getVersion().longValue()
        );
    }

    @Test
    void shouldDeleteExistingDevice() {
        Device device = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Device saved = repository.saveAndFlush(device);
        UUID deviceId = saved.getId();

        assertTrue(repository.existsById(deviceId));

        repository.delete(saved);
        repository.flush();
        entityManager.clear();

        assertFalse(repository.existsById(deviceId));
    }

    @Test
    void shouldRejectDeviceWithoutName() {
        Device device = new Device(null, "Apple", DeviceState.AVAILABLE);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(device)
        );
    }

    @Test
    void shouldRejectDeviceWithoutBrand() {
        Device device = new Device("iPhone 16", null, DeviceState.AVAILABLE);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(device)
        );
    }

    @Test
    void shouldRejectDeviceWithoutState() {
        Device device = new Device("iPhone 16", "Apple", null);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(device)
        );
    }

    @Test
    void shouldRejectConcurrentUpdateWithStaleVersion() {
        Device device = new Device("iPhone 16", "Apple", DeviceState.AVAILABLE);

        Device saved = repository.saveAndFlush(device);
        UUID deviceId = saved.getId();

        entityManager.clear();

        // First transaction's view of the device
        Device firstCopy = repository.findById(deviceId).orElseThrow();
        entityManager.detach(firstCopy);

        // Second transaction's view of the same device
        Device secondCopy = repository.findById(deviceId).orElseThrow();
        entityManager.detach(secondCopy);

        // First update succeeds and increments version
        firstCopy.update("iPhone 16 Pro", "Apple", DeviceState.AVAILABLE);
        repository.saveAndFlush(firstCopy);
        entityManager.flush();
        entityManager.clear();

        // Second update still carries the old version
        secondCopy.update("iPhone 16 Plus", "Apple", DeviceState.AVAILABLE);

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> repository.saveAndFlush(secondCopy)
        );
    }
}
