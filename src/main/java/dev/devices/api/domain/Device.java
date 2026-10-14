package dev.devices.api.domain;

import dev.devices.api.common.exception.DeviceConflictException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Device aggregate. Business rules live here so that every write path
 * (PUT, PATCH, future endpoints) enforces them the same way.
 */
@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceState state;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Device(String name, String brand, DeviceState state) {
        this.name = normalize(name);
        this.brand = normalize(brand);
        this.state = state;
    }

    /**
     * Applies a full update. Name and brand cannot change while the device
     * is IN_USE; the rule is evaluated against the state before the update.
     */
    public void update(String newName, String newBrand, DeviceState newState) {
        String normalizedName = normalize(newName);
        String normalizedBrand = normalize(newBrand);

        boolean nameChanged = !Objects.equals(name, normalizedName);
        boolean brandChanged = !Objects.equals(brand, normalizedBrand);

        if (state == DeviceState.IN_USE && (nameChanged || brandChanged)) {
            throw new DeviceConflictException(
                    "Cannot change name or brand while device is IN_USE");
        }

        this.name = normalizedName;
        this.brand = normalizedBrand;
        this.state = newState;
    }

    /**
     * Removes leading and trailing whitespace so that " Apple " and "Apple"
     * are stored, compared and filtered as the same value.
     */
    public static String normalize(String value) {
        return value == null ? null : value.strip();
    }

    public void ensureDeletable() {
        if (state == DeviceState.IN_USE) {
            throw new DeviceConflictException(
                    "Cannot delete a device while it is IN_USE");
        }
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
