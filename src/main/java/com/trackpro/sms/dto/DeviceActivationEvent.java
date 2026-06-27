package com.trackpro.sms.dto;

public record DeviceActivationEvent(
        String imei,
        String activationStatus,
        DeviceStatusReply reply
) {}
