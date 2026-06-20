package com.trackpro.telemetry.netty;

import com.trackpro.config.TelemetryProperties;
import com.trackpro.telemetry.TelemetryService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "trackpro.telemetry.tcp", name = "enabled", havingValue = "true", matchIfMissing = false)
public class NettyTcpServer implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(NettyTcpServer.class);

    private final TelemetryProperties props;
    private final TelemetryService telemetryService;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannel;

    public NettyTcpServer(TelemetryProperties props, TelemetryService telemetryService) {
        this.props = props;
        this.telemetryService = telemetryService;
    }

    @Override
    public void afterPropertiesSet() throws InterruptedException {
        int port = props.tcp().port();
        bossGroup   = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        TelemetryFrameHandler frameHandler = new TelemetryFrameHandler(telemetryService);

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 5-minute read idle → close stale connections
                                .addLast(new IdleStateHandler(300, 0, 0, TimeUnit.SECONDS))
                                .addLast(new Gt06Decoder())
                                .addLast(frameHandler);
                    }
                });

        serverChannel = bootstrap.bind(port).sync();
        log.info("Netty TCP server listening on port {}", port);
    }

    @Override
    public void destroy() throws InterruptedException {
        log.info("Shutting down Netty TCP server");
        if (serverChannel != null) serverChannel.channel().close().sync();
        if (bossGroup   != null) bossGroup.shutdownGracefully().sync();
        if (workerGroup != null) workerGroup.shutdownGracefully().sync();
    }
}
