package ru.geekbrains.chiffa.cloudstorage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ObjectDecoder;

public class SwitchableObjectDecoder extends ObjectDecoder {

    public SwitchableObjectDecoder(ClassResolver classResolver) {
        this(1048576, classResolver);
    }

    public SwitchableObjectDecoder(int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, classResolver);
        setSingleDecode(true);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        MainHandler mainHandler = ctx.pipeline().get(MainHandler.class);

        if (!mainHandler.isUploading()) {
            super.channelRead(ctx, byteBuf);
            propagateToFileHandler(ctx, byteBuf, false);
        }

        if (mainHandler.isUploading()) {
            propagateToFileHandler(ctx, byteBuf, true);
        }
    }

    private void propagateToFileHandler(ChannelHandlerContext ctx, ByteBuf msg, boolean release) {
        if (msg.isReadable() && msg.readableBytes() > 0) {
            try {
                ctx.fireChannelRead(msg);
            } finally {
                if (release) msg.release();
            }
        }
    }
}
