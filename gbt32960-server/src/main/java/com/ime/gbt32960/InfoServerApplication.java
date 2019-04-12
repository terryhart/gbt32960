package com.ime.gbt32960;

import com.ime.gbt32960.codec.GBT32960Decoder;
import com.ime.gbt32960.codec.GBT32960Encoder;
import com.ime.gbt32960.protocol.ProtocolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Qingxi
 */

@Slf4j
@SpringBootApplication
public class InfoServerApplication implements CommandLineRunner {

    private static final int LISTEN_PORT = 32960;

    public static void main(String[] args) {
        SpringApplication.run(InfoServerApplication.class);
    }

    @Override
    public void run(String... args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);

            Bootstrap clientBoot = new Bootstrap();
            clientBoot.group(workerGroup);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                            .addLast(new CombinedChannelDuplexHandler<>(new GBT32960Decoder(), new GBT32960Encoder()))
                            .addLast(new IdleStateHandler(60 * 5, 0, 0))
                            .addLast(ProtocolHandler.getInstance());
                }
            });
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);

            ChannelFuture f = serverBootstrap.bind(LISTEN_PORT).sync();
            log.info("server listened on {}", LISTEN_PORT);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
