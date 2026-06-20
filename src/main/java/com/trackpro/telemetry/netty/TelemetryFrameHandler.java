package com.trackpro.telemetry.netty;

import com.trackpro.telemetry.DeviceFrame;
import com.trackpro.telemetry.TelemetryService;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives decoded DeviceFrame objects from Gt06Decoder and hands them
 * to TelemetryService for persistence + broadcasting.
 */
@Sharable
public class TelemetryFrameHandler extends SimpleChannelInboundHandler<DeviceFrame> {

    private static final Logger log = LoggerFactory.getLogger(TelemetryFrameHandler.class);

    private final TelemetryService telemetryService;

    public TelemetryFrameHandler(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DeviceFrame frame) {
        try {
            telemetryService.ingestTelemetry(frame);
        } catch (Exception e) {
            log.error("Failed to process telemetry frame from IMEI={}: {}", frame.imei(), e.getMessage(), e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("TCP device connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String imei = ctx.channel().attr(Gt06Decoder.IMEI_KEY).get();
        log.info("TCP device disconnected: IMEI={} addr={}", imei, ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("TCP channel error: {}", cause.getMessage());
        ctx.close();
    }
}
