package com.trackpro.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record AdminBulkDeviceRequest(
        @NotEmpty List<String> imeis,
        String deviceType,
        String firmware,
        String simCard,
        String serialNo,
        UUID organisationId,
        String vehiclePlate,
        String notes,
        String simNumber,
        String simApn,
        String manufacturer
) {}
