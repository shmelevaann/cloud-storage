package ru.geekbrains.chiffa.cloudstorage;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NettyServer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    private final EventLoopGroup acceptGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ServerBootstrap serverBootstrap = setUpServer();
    private final Path storagePath;

    public NettyServer(Path storagePath) {
        this.storagePath = storagePath;
    }


    private ServerBootstrap setUpServer() {
        return new ServerBootstrap()
                .group(acceptGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline().addLast(
                                new ChunkedWriteHandler(),
                                new SwitchableObjectDecoder(ClassResolvers.cacheDisabled(Request.class.getClassLoader())),
                                new ObjectEncoder(),
                                new MainHandler(storagePath)
                        );
                    }
                });
    }

    public void start() throws InterruptedException {
        Channel channel = serverBootstrap.bind(8888).sync().channel();
        logger.info("Server started");
        channel.closeFuture().sync();
    }

    @Override
    public void close() {
        acceptGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        logger.info("Server closed");
    }

    public static void main(String[] args) throws InterruptedException {
        try (NettyServer server = new NettyServer(Paths.get("server_storage"))) {
            server.start();
        }
    }
}
