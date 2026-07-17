package com.trackpro.telemetry.netty;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Gt06DecoderTest {

    @Test
    void decodesBcdImeiFromTrackerLoginPacket() {
        byte[] loginData = new byte[] {
                0x03, 0x57, 0x44, 0x51, 0x01, 0x67, 0x06, 0x08
        };

        assertThat(Gt06Decoder.decodeImei(loginData)).isEqualTo("357445101670608");
    }
}
