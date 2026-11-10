package io.github.regalpine.agentforge.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.util.function.Consumer;

/**
 * BE-07: Netty-based HTTP server bootstrap.
 *
 * <p>Replaces {@code com.sun.net.httpserver.HttpServer.create()} with
 * Netty's NIO event loop. Thread count is configurable via
 * {@code AGENTFORGE_WORKER_THREADS} (default: 2 × CPU cores).
 *
 * <p>Pipeline:
 * <pre>
 * NioServerSocketChannel
 *   └── HttpServerCodec        (HTTP/1.1 encode/decode)
 *   └── HttpObjectAggregator   (aggregate chunks → FullHttpRequest)
 *   └── AgentForgeHttpRouter   (route dispatch to handlers)
 * </pre>
 */
public final class NettyServerBootstrap {

    private final int port;
    private final int workerThreads;
    private final Consumer<AgentForgeHttpRouter> routerConfig;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyServerBootstrap(int port, int workerThreads, Consumer<AgentForgeHttpRouter> routerConfig) {
        this.port = port;
        this.workerThreads = workerThreads > 0 ? workerThreads : Runtime.getRuntime().availableProcessors() * 2;
        this.routerConfig = routerConfig;
    }

    /**
     * Start the Netty server.
     */
    public void start() throws InterruptedException {
        AgentForgeHttpRouter router = new AgentForgeHttpRouter();
        routerConfig.accept(router);

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(workerThreads);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(1048576)); // 1MB max
                    ch.pipeline().addLast(router);
                }
            });

        serverChannel = bootstrap.bind(port).sync().channel();

        System.out.println("  Netty server started on port " + port
            + " with " + workerThreads + " worker threads");
    }

    /**
     * Stop the Netty server gracefully.
     */
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        System.out.println("  Netty server stopped.");
    }
}
