package com.trackpro.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceUpdateRequest(
        @NotBlank
        @Size(max = 120)
        String name
) {
}
