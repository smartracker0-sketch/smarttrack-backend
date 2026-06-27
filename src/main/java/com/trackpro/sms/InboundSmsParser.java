package com.trackpro.sms;

import com.trackpro.sms.dto.DeviceStatusReply;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class InboundSmsParser {

    public DeviceStatusReply parse(String rawText, String manufacturer) {
        if (rawText == null || rawText.isBlank()) {
            return unknown(rawText);
        }
        if (manufacturer == null) {
            return parseGeneric(rawText);
        }
        return switch (manufacturer.toUpperCase().trim()) {
            case "TELTONIKA" -> parseTeltonika(rawText);
            case "CONCOX"    -> parseConcox(rawText);
            default          -> parseGeneric(rawText);
        };
    }

    // Teltonika FMB example:
    // "Latitude:6.450123 Longitude:3.391234 Speed:0 I/O:1 Bat:100% GSM:5"
    private DeviceStatusReply parseTeltonika(String text) {
        Double lat   = extractDouble(text, "Latitude[:\\s]+([-\\d.]+)");
        Double lon   = extractDouble(text, "Longitude[:\\s]+([-\\d.]+)");
        Integer spd  = extractInt(text,   "Speed[:\\s]+(\\d+)");
        Integer bat  = extractInt(text,   "Bat[:\\s]+(\\d+)");
        Integer gsm  = extractInt(text,   "GSM[:\\s]+(\\d)");
        Boolean ign  = extractBoolFlag(text, "I/O[:\\s]+1");
        boolean fix  = lat != null && lon != null;
        return new DeviceStatusReply(lat, lon, spd, ign, bat, gsm, fix, text);
    }

    // Concox GT06 example:
    // "lat:6.450123,lon:3.391234,speed:0,acc:0,oil:1,bat:4.1V,gsm:4,gps:A"
    private DeviceStatusReply parseConcox(String text) {
        Double lat   = extractDouble(text, "lat[:\\s]+([-\\d.]+)");
        Double lon   = extractDouble(text, "lon[:\\s]+([-\\d.]+)");
        Integer spd  = extractInt(text,   "speed[:\\s]+(\\d+)");
        Integer gsm  = extractInt(text,   "gsm[:\\s]+(\\d)");
        Boolean acc  = extractBoolFlag(text, "acc[:\\s]*1");
        // battery comes as voltage — convert roughly to percent (3.5–4.2V → 0–100%)
        Double batV  = extractDouble(text, "bat[:\\s]+([-\\d.]+)");
        Integer bat  = batV != null ? voltageToBatPct(batV) : null;
        boolean fix  = text.contains("gps:A") || text.contains("gps:1");
        return new DeviceStatusReply(lat, lon, spd, acc, bat, gsm, fix, text);
    }

    // Generic fallback — scan for common GPS patterns in any format
    private DeviceStatusReply parseGeneric(String text) {
        Double lat  = extractDouble(text, "(?i)lat(?:itude)?[:\\s,]+([-\\d.]+)");
        Double lon  = extractDouble(text, "(?i)lon(?:gitude)?[:\\s,]+([-\\d.]+)");
        Integer spd = extractInt(text,   "(?i)speed[:\\s,]+(\\d+)");
        Integer gsm = extractInt(text,   "(?i)gsm[:\\s,]+(\\d)");
        Integer bat = extractInt(text,   "(?i)bat(?:tery)?[:\\s,]+(\\d+)");
        boolean fix = lat != null && lon != null;
        return new DeviceStatusReply(lat, lon, spd, null, bat, gsm, fix, text);
    }

    private DeviceStatusReply unknown(String rawText) {
        return new DeviceStatusReply(null, null, null, null, null, null, false, rawText);
    }

    private Double extractDouble(String text, String pattern) {
        try {
            Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) return Double.parseDouble(m.group(1));
        } catch (Exception ignored) {}
        return null;
    }

    private Integer extractInt(String text, String pattern) {
        try {
            Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return null;
    }

    private Boolean extractBoolFlag(String text, String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    private int voltageToBatPct(double voltage) {
        double min = 3.5, max = 4.2;
        int pct = (int) (((voltage - min) / (max - min)) * 100);
        return Math.max(0, Math.min(100, pct));
    }
}
