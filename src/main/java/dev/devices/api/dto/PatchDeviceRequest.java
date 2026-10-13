package dev.devices.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import dev.devices.api.domain.DeviceState;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatchDeviceRequest {

    @Size(max = 255)
    @Pattern(regexp = "(?s).*\\S.*", message = "Name must not be blank")
    @JsonSetter(nulls = Nulls.FAIL)
    private String name;

    @Size(max = 255)
    @Pattern(regexp = "(?s).*\\S.*", message = "Brand must not be blank")
    @JsonSetter(nulls = Nulls.FAIL)
    private String brand;

    @JsonSetter(nulls = Nulls.FAIL)
    private DeviceState state;

    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        throw new IllegalArgumentException("Unknown field: " + field);
    }
}