
package dev.devices.api.repository;

import dev.devices.api.domain.Device;
import dev.devices.api.domain.DeviceState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    List<Device> findByBrand(String brand);

    List<Device> findByState(DeviceState state);

    List<Device> findByBrandAndState(String brand, DeviceState state);
}
