package com.trackpro.dto.admin;

public record AdminDeviceUpdateRequest(
        String name,
        String deviceType,
        String firmware,
        String simCard,
        String serialNo,
        String vehiclePlate,
        String notes,
        String simNumber,
        String simApn,
        String manufacturer,
        String model,
        String simIccid,
        String mobileCarrier,
        String smsCommandPassword
) {}
