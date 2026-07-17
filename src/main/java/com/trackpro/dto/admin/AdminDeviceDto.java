package com.trackpro.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminDeviceDto(
        UUID id,
        String imei,
        String name,
        String deviceType,
        String firmware,
        String simCard,
        String serialNo,
        String vehiclePlate,
        String status,
        String notes,
        UUID organisationId,
        String organisationName,
        UUID ownerId,
        String ownerName,
        String simNumber,
        String simApn,
        String manufacturer,
        String model,
        String simIccid,
        String mobileCarrier,
        boolean hasSmsCommandPassword,
        String activationStatus,
        int activationAttempts,
        Instant activationAttemptedAt,
        Instant activationConfirmedAt,
        String lastSmsReply,
        boolean serverConfigured,
        boolean apnConfigured,
        Instant createdAt
) {}
