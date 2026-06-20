package com.trackpro.telemetry;

import java.time.Instant;

/**
 * Normalised telemetry frame produced by any protocol decoder (GT06, MQTT JSON, etc.)
 * before it reaches TelemetryService.
 */
public record DeviceFrame(
        String imei,
        Instant eventTime,
        Double latitude,
        Double longitude,
        Double altitudeM,
        Double speedKph,
        Double headingDeg,
        Double accuracyM,
        Integer satellites,
        Boolean ignition,
        Integer voltageMv,
        Integer gsmSignal,
        Long odometerM,
        String rawPayload
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String imei;
        private Instant eventTime;
        private Double latitude;
        private Double longitude;
        private Double altitudeM;
        private Double speedKph;
        private Double headingDeg;
        private Double accuracyM;
        private Integer satellites;
        private Boolean ignition;
        private Integer voltageMv;
        private Integer gsmSignal;
        private Long odometerM;
        private String rawPayload;

        public Builder imei(String v)         { this.imei = v; return this; }
        public Builder eventTime(Instant v)   { this.eventTime = v; return this; }
        public Builder latitude(Double v)     { this.latitude = v; return this; }
        public Builder longitude(Double v)    { this.longitude = v; return this; }
        public Builder altitudeM(Double v)    { this.altitudeM = v; return this; }
        public Builder speedKph(Double v)     { this.speedKph = v; return this; }
        public Builder headingDeg(Double v)   { this.headingDeg = v; return this; }
        public Builder accuracyM(Double v)    { this.accuracyM = v; return this; }
        public Builder satellites(Integer v)  { this.satellites = v; return this; }
        public Builder ignition(Boolean v)    { this.ignition = v; return this; }
        public Builder voltageMv(Integer v)   { this.voltageMv = v; return this; }
        public Builder gsmSignal(Integer v)   { this.gsmSignal = v; return this; }
        public Builder odometerM(Long v)      { this.odometerM = v; return this; }
        public Builder rawPayload(String v)   { this.rawPayload = v; return this; }

        public DeviceFrame build() {
            return new DeviceFrame(imei, eventTime, latitude, longitude, altitudeM,
                    speedKph, headingDeg, accuracyM, satellites, ignition,
                    voltageMv, gsmSignal, odometerM, rawPayload);
        }
    }
}
