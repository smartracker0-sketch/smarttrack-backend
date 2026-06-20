package com.trackpro.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]{10,20}$")
        String imei,
        @NotBlank
        @Size(max = 120)
        String name,
        String deviceType,
        String firmware,
        String simCard,
        String serialNo,
        String vehiclePlate,
        String notes
) {
}
