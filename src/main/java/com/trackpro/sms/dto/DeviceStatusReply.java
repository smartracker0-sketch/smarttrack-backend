package com.trackpro.sms.dto;

public record DeviceStatusReply(
        Double latitude,
        Double longitude,
        Integer speedKmh,
        Boolean ignitionOn,
        Integer batteryPercent,
        Integer gsmSignalBars,
        Boolean gpsFixed,
        String rawText
) {}
