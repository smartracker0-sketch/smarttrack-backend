package com.trackpro.telemetry.netty;

import com.trackpro.telemetry.DeviceFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes GT06/GT06N binary protocol frames over TCP.
 *
 * Frame layout (simplified):
 *   0x78 0x78  — start bytes
 *   1 byte     — packet length
 *   1 byte     — protocol number (0x01=login, 0x10=GPS, 0x13=heartbeat, 0x16=alarm)
 *   N bytes    — data
 *   2 bytes    — serial number
 *   2 bytes    — CRC-16
 *   0x0D 0x0A  — end bytes
 */
public class Gt06Decoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(Gt06Decoder.class);

    private static final byte START1 = 0x78;
    private static final byte START2 = 0x78;
    private static final byte END1   = 0x0D;
    private static final byte END2   = 0x0A;

    // Per-channel IMEI stored after login packet
    static final io.netty.util.AttributeKey<String> IMEI_KEY =
            io.netty.util.AttributeKey.valueOf("gt06.imei");

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.readableBytes() >= 10) {
            in.markReaderIndex();

            if (in.readByte() != START1 || in.readByte() != START2) {
                in.resetReaderIndex();
                in.skipBytes(1);
                continue;
            }

            int packetLen = in.readUnsignedByte();
            if (in.readableBytes() < packetLen + 2) {  // +2 for end bytes
                in.resetReaderIndex();
                return;
            }

            int protocolNum = in.readUnsignedByte();
            int dataLen     = packetLen - 5;  // protocol(1) + serial(2) + crc(2)

            byte[] data = new byte[Math.max(dataLen, 0)];
            if (dataLen > 0) in.readBytes(data);

            int serialNo = in.readUnsignedShort();
            int crc      = in.readUnsignedShort();
            byte e1      = in.readByte();
            byte e2      = in.readByte();

            if (e1 != END1 || e2 != END2) {
                log.warn("GT06 frame end bytes mismatch, dropping frame");
                continue;
            }

            switch (protocolNum) {
                case 0x01 -> handleLogin(ctx, data, serialNo);
                case 0x10 -> handleGps(ctx, data, out);
                case 0x13 -> handleHeartbeat(ctx, serialNo);
                case 0x16 -> handleAlarm(ctx, data, out);
                default   -> log.debug("GT06 unknown protocol 0x{}", Integer.toHexString(protocolNum));
            }
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, byte[] data, int serialNo) {
        if (data.length < 8) return;
        String imeiStr = decodeImei(data);
        if (imeiStr == null) return;
        ctx.channel().attr(IMEI_KEY).set(imeiStr);
        log.info("GT06 device login IMEI={}", imeiStr);
        sendResponse(ctx, (byte) 0x01, serialNo);
    }

    static String decodeImei(byte[] data) {
        if (data == null || data.length < 8) return null;
        StringBuilder digits = new StringBuilder(16);
        for (int i = 0; i < 8; i++) {
            int value = data[i] & 0xFF;
            int high = value >>> 4;
            int low = value & 0x0F;
            if (high > 9 || low > 9) return null;
            digits.append(high).append(low);
        }
        if (digits.charAt(0) == '0') digits.deleteCharAt(0);
        return digits.length() >= 15 ? digits.substring(0, 15) : null;
    }

    private void handleGps(ChannelHandlerContext ctx, byte[] data, List<Object> out) {
        if (data.length < 12) return;
        String imei = ctx.channel().attr(IMEI_KEY).get();
        if (imei == null) { log.warn("GT06 GPS frame received before login"); return; }

        int year   = (data[0] & 0xFF);
        int month  = (data[1] & 0xFF);
        int day    = (data[2] & 0xFF);
        int hour   = (data[3] & 0xFF);
        int minute = (data[4] & 0xFF);
        int second = (data[5] & 0xFF);
        Instant eventTime = ZonedDateTime.of(2000 + year, month, day, hour, minute, second, 0, ZoneOffset.UTC).toInstant();

        int gpsInfo     = data[6] & 0xFF;
        int satellites  = gpsInfo & 0x0F;

        long latRaw  = ((data[7]  & 0xFFL) << 24) | ((data[8]  & 0xFFL) << 16) | ((data[9]  & 0xFFL) << 8) | (data[10] & 0xFFL);
        long lonRaw  = ((data[11] & 0xFFL) << 24) | ((data[12] & 0xFFL) << 16) | ((data[13] & 0xFFL) << 8) | (data[14] & 0xFFL);
        double lat   = latRaw / 1800000.0;
        double lon   = lonRaw / 1800000.0;

        int speedKph = data[15] & 0xFF;

        int flags = 0;
        double heading = 0;
        if (data.length > 17) {
            flags   = ((data[16] & 0xFF) << 8) | (data[17] & 0xFF);
            heading = flags & 0x03FF;
            if ((flags & 0x0400) == 0) lat  = -lat;   // south (bit 10 clear)
            if ((flags & 0x0800) != 0) lon  = -lon;   // west (bit 11 set)
        }

        boolean ignition = (flags & 0x8000) != 0;

        DeviceFrame frame = DeviceFrame.builder()
                .imei(imei)
                .eventTime(eventTime)
                .latitude(lat)
                .longitude(lon)
                .speedKph((double) speedKph)
                .headingDeg(heading)
                .satellites(satellites)
                .ignition(ignition)
                .rawPayload(HexFormat.of().formatHex(data))
                .build();

        out.add(frame);
    }

    private void handleAlarm(ChannelHandlerContext ctx, byte[] data, List<Object> out) {
        // Alarm shares the same GPS data prefix as 0x10 — reuse GPS handler
        handleGps(ctx, data, out);
        log.warn("GT06 alarm frame from IMEI={}", ctx.channel().attr(IMEI_KEY).get());
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, int serialNo) {
        sendResponse(ctx, (byte) 0x13, serialNo);
    }

    private void sendResponse(ChannelHandlerContext ctx, byte protocolNum, int serialNo) {
        ByteBuf resp = ctx.alloc().buffer(10);
        resp.writeByte(0x78);
        resp.writeByte(0x78);
        resp.writeByte(0x05);       // length
        resp.writeByte(protocolNum);
        resp.writeShort(serialNo);
        resp.writeShort(crc16(new byte[]{0x05, protocolNum,
                (byte)(serialNo >> 8), (byte)(serialNo & 0xFF)}));
        resp.writeByte(0x0D);
        resp.writeByte(0x0A);
        ctx.writeAndFlush(resp);
    }

    private static int crc16(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) crc = (crc << 1) ^ 0x1021;
                else                      crc <<= 1;
            }
        }
        return crc & 0xFFFF;
    }
}
