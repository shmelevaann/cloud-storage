package ru.geekbrains.chiffa.cloudstorage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MainHandler extends ChannelHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

    private final Path rootDir;

    //uploading fields
    private UploadRequest uploadRequest;
    private FileChannel toStorageFile;
    private long loadedBytes;

    public MainHandler(Path rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ListFilesRequest) {
            handleListFilesRequest(ctx, (ListFilesRequest) msg);
        } else if (msg instanceof DeleteRequest) {
            handleDeleteRequest(ctx, (DeleteRequest) msg);
        } else if (msg instanceof RenameRequest) {
            handleRenameRequest(ctx, (RenameRequest) msg);
        } else if (msg instanceof DownloadRequest) {
            handleDownloadRequest(ctx, (DownloadRequest) msg);
        } else if (msg instanceof UploadRequest) {
            handleUploadRequest(ctx, (UploadRequest) msg);
        } else if (msg instanceof ByteBuf) {
            handleByteBuf(ctx, (ByteBuf) msg);
        } else {
            ctx.writeAndFlush(new ErrorResponse(UUID.fromString("-1"), "No such request type"));
        }
    }

    private void handleDownloadRequest(ChannelHandlerContext ctx, DownloadRequest request) {
        String username = request.getUsername();
        String fileName = request.getFilename();
        Path filePath = rootDir.resolve(username).resolve(fileName);
        try {
            if (Files.exists(filePath)) {
                long fileSize = Files.size(filePath);
                ctx.writeAndFlush(new DownloadResponse(request.getId(), fileName, fileSize));
                ctx.writeAndFlush(new ChunkedFile(filePath.toFile()));
            } else {
                ctx.writeAndFlush(new ErrorResponse(
                        request.getId(),
                        String.format("File %s doesn't exist", fileName)));
            }
        } catch (IOException e) {
            logger.warn("File wasn't downloaded.", e);
            ctx.writeAndFlush(new ErrorResponse(request.getId(), "File wasn't downloaded."));
        }
    }

    private void handleByteBuf(ChannelHandlerContext ctx, ByteBuf msg) {
        if (isUploading()) {
            long readableBytes = msg.readableBytes();
            long remaining = uploadRequest.getFileSize() - loadedBytes;

            try {
                if (remaining < readableBytes) {
                    writeToChannel(msg.readBytes((int) remaining));
                } else {
                    writeToChannel(msg);
                }

                if (loadedBytes == uploadRequest.getFileSize()) {
                    logger.info("File {} with size {} uploaded", uploadRequest.getFilename(), uploadRequest.getFileSize());
                    closeQuietly();
                    sendListFileResponse(ctx, uploadRequest.getId(), uploadRequest.getUsername());
                }
            } catch (IOException e) {
                closeQuietly();
                logger.warn("File wasn't uploaded.", e);
                ctx.writeAndFlush(new ErrorResponse(uploadRequest.getId(), "File wasn't uploaded."));
            }
        }
    }

    private void writeToChannel(ByteBuf buf) throws IOException {
        int readBytes = buf.readableBytes();
        this.loadedBytes += readBytes;
        toStorageFile.write(buf.nioBuffer());
        buf.skipBytes(readBytes);
        toStorageFile.force(false);
    }

    private void closeQuietly() {
        try {
            toStorageFile.close();
            toStorageFile = null;
            loadedBytes = 0;
        } catch (Exception e) {
            logger.warn("Can't close stream correctly.", e);
        }
    }

    private void handleUploadRequest(ChannelHandlerContext ctx, UploadRequest request) {
        try {
            String username = request.getUsername();
            uploadRequest = request;
            loadedBytes = 0;

            Path userDir = rootDir.resolve(username);
            if (!Files.exists(userDir)) {
                Files.createDirectory(userDir);
            }
            Path fileName = userDir.resolve(request.getFilename());

            toStorageFile = new FileOutputStream(fileName.normalize().toString()).getChannel();
        } catch (IOException e) {
            logger.warn("File wasn't uploaded.", e);
            ctx.writeAndFlush(new ErrorResponse(request.getId(), "File wasn't uploaded."));
        }
    }

    private void handleRenameRequest(ChannelHandlerContext ctx, RenameRequest request) {
        String username = request.getUsername();
        Path fileName = rootDir.resolve(username).resolve(request.getFilename());
        Path newFileName = rootDir.resolve(username).resolve(request.getNewFileName());

        if (!Files.exists(fileName)) {
            ctx.writeAndFlush(new ErrorResponse(
                    request.getId(),
                    String.format("File %s doesn't exist.", fileName.toString())));
        } else if (Files.exists(newFileName)) {
            ctx.writeAndFlush(new ErrorResponse(
                    request.getId(),
                    String.format("File %s already exist.", newFileName)
            ));
        } else {
            try {
                Files.move(fileName, newFileName);

                sendListFileResponse(ctx, request.getId(), username);
            } catch (IOException e) {
                logger.warn("File wasn't renamed", e);
                ctx.writeAndFlush(new ErrorResponse(request.getId(), "File wasn't renamed"));
            }
        }

    }

    private void handleDeleteRequest(ChannelHandlerContext ctx, DeleteRequest request) {
        String username = request.getUsername();
        Path fileName = rootDir.resolve(username).resolve(request.getFilename());
        if (!Files.exists(fileName)) {
            ctx.writeAndFlush(new ErrorResponse(request.getId(), "File isn't found."));
        } else {
            try {
                Files.delete(fileName);

                sendListFileResponse(ctx, request.getId(), username);
            } catch (IOException e) {
                logger.warn("File wasn't deleted.", e);
                ctx.writeAndFlush(new ErrorResponse(request.getId(), "File wasn't deleted."));
            }
        }
    }

    private void handleListFilesRequest(ChannelHandlerContext ctx, ListFilesRequest request) {
        String username = request.getUsername();
        UUID id = request.getId();

        sendListFileResponse(ctx, id, username);
    }

    private void sendListFileResponse(ChannelHandlerContext ctx, UUID id, String username) {
        try {
            Path userDir = rootDir.resolve(username);
            if (!Files.exists(userDir)) {
                Files.createDirectory(userDir);
            }
            List<String> files = Files
                    .list(userDir)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            ListFilesResponse lfr =
                    new ListFilesResponse(id, files);
            ctx.writeAndFlush(lfr);
        } catch (Exception e) {
            logger.warn("Unable to list files for directory {}", username, e);
            ctx.writeAndFlush(new ErrorResponse(id, "Unable to list files, try again"));
        }
    }

    public boolean isUploading() {
        return toStorageFile != null;
    }
}
