package com.trackpro.telemetry.netty;

import static org.assertj.core.api.Assertions.assertThat;

import com.trackpro.telemetry.DeviceFrame;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;

import org.junit.jupiter.api.Test;

class Gt06DecoderTest {

    @Test
    void decodesBcdImeiFromTrackerLoginPacket() {
        byte[] loginData = new byte[] {
                0x03, 0x57, 0x44, 0x51, 0x01, 0x67, 0x06, 0x08
        };

        assertThat(Gt06Decoder.decodeImei(loginData)).isEqualTo("357445101670608");
    }

    @Test
    void decodesGpsCoordinateSignBits() {
        EmbeddedChannel channel = new EmbeddedChannel(new Gt06Decoder());

        // Login packet (IMEI 357445101670608)
        byte[] login = {
                0x78, 0x78, 0x0D, 0x01, 0x03, 0x57, 0x44, 0x51, 0x01, 0x67, 0x06, 0x08, 0x00, 0x01, 0x00, 0x00, 0x0D, 0x0A
        };
        channel.writeInbound(Unpooled.wrappedBuffer(login));

        // GPS packet: raw lat=1, raw lon=1, flags = bit 10 clear (south) and bit 11 set (west)
        byte[] gps = {
                0x78, 0x78, 0x17, 0x10,
                0x17, 0x07, 0x13, 0x0A, 0x0A, 0x0A, // date/time
                (byte) 0xCB, // GPS info
                0x00, 0x00, 0x00, 0x01, // lat raw
                0x00, 0x00, 0x00, 0x01, // lon raw
                0x00, // speed
                0x08, 0x00, // course/status: west on, south off -> flags 0x0800
                0x00, 0x02, 0x00, 0x00, 0x0D, 0x0A
        };
        channel.writeInbound(Unpooled.wrappedBuffer(gps));

        DeviceFrame frame = (DeviceFrame) channel.readInbound();
        assertThat(frame).isNotNull();
        assertThat(frame.latitude()).isNegative();
        assertThat(frame.longitude()).isNegative();
        assertThat(frame.headingDeg()).isEqualTo(0.0);
    }
}
