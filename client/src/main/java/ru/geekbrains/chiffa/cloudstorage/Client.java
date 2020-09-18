package ru.geekbrains.chiffa.cloudstorage;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class Client {
    public static final int BUFFER_SIZE = 4096;
    private final Path rootDir = Paths.get("./Downloads");
    private final String userName;
    private Socket socket;
    private final ExecutorService executorService;

    public Client(String clientName, ExecutorService executorService) {
        this.userName = clientName;
        this.executorService = executorService;
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 8888);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<List<String>> upload(Path filePath) {
        return CompletableFuture.supplyAsync(() -> {
            connect();

            try (OutputStream os = socket.getOutputStream();
                 ObjectEncoderOutputStream encoderOutputStream = new ObjectEncoderOutputStream(os);
                 ObjectDecoderInputStream decoderInputStream = new ObjectDecoderInputStream(socket.getInputStream())) {

                UploadRequest uploadRequest = new UploadRequest(
                        userName,
                        Files.size(filePath),
                        filePath.getFileName().toString());
                encoderOutputStream.writeObject(uploadRequest);
                encoderOutputStream.flush();
                sendFile(filePath.toString(), os);

                return handleResponse(decoderInputStream.readObject());
            } catch (IOException e) {
                throw new RuntimeException("Connection failed", e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Server's answer is unreadable. Please check updates", e);
            }
        }, executorService);
    }

    private void sendFile(String filePath, OutputStream ous) throws IOException {
        try (InputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                ous.write(buffer, 0, count);
            }
            ous.flush();
        }
    }

    public CompletableFuture<List<String>> refresh() {
        return CompletableFuture.supplyAsync(() -> {
            connect();

            try (ObjectEncoderOutputStream ous = new ObjectEncoderOutputStream(socket.getOutputStream());
                 ObjectDecoderInputStream ois = new ObjectDecoderInputStream(socket.getInputStream())) {
                ous.writeObject(new ListFilesRequest(userName));

                return handleResponse(ois.readObject());
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Server is unable", e);
            }
        }, executorService);
    }

    public CompletableFuture<List<String>> delete(String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            connect();

            try (ObjectEncoderOutputStream ous = new ObjectEncoderOutputStream(socket.getOutputStream());
                 ObjectDecoderInputStream ois = new ObjectDecoderInputStream(socket.getInputStream())) {
                ous.writeObject(new DeleteRequest(userName, fileName));

                return handleResponse(ois.readObject());
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Server is unable", e);
            }
        }, executorService);
    }

    private List<String> handleResponse(Object response) {
        if (response instanceof ListFilesResponse) {
            return ((ListFilesResponse) response).getFiles();
        } else {
            throw new RuntimeException(handleErrorResponse(response));
        }
    }

    private String handleErrorResponse(Object response) {
        if (response instanceof ErrorResponse) {
            return ((ErrorResponse) response).getText();
        } else {
            return "Server response unreadable";
        }
    }

    public CompletableFuture<Boolean> download(String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            connect();

            try (InputStream is = socket.getInputStream();
                 ObjectEncoderOutputStream ous = new ObjectEncoderOutputStream(socket.getOutputStream());
                 ObjectDecoderInputStream ois = new ObjectDecoderInputStream(is)) {
                ous.writeObject(new DownloadRequest(userName, fileName));

                Object response = ois.readObject();
                if (response instanceof DownloadResponse) {
                    return handleDownloadResponse((DownloadResponse) response, ois);
                } else {
                    throw new RuntimeException(handleErrorResponse(response));
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Server is unable", e);
            }
        }, executorService);
    }

    private boolean handleDownloadResponse(DownloadResponse response, ObjectDecoderInputStream is) throws IOException, ClassNotFoundException {
        Path destPath = Paths.get(rootDir.toString(), response.getFilename());
        long fileSize = response.getFileSize();

        if (!Files.exists(destPath)) {
            Files.createFile(destPath);
        }
        try (FileOutputStream fos = new FileOutputStream(destPath.toFile())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (long i = 0; i < fileSize; ) {
                int count = is.read(buffer, 0,
                        (int) (i + BUFFER_SIZE > fileSize ? fileSize - i : BUFFER_SIZE));
                fos.write(buffer, 0, count);
                i += count;
            }
            return true;
        }
    }

    public CompletableFuture<List<String>> rename(String fileName, String newFileName) {
        return CompletableFuture.supplyAsync(() -> {
            connect();

            try (ObjectEncoderOutputStream ous = new ObjectEncoderOutputStream(socket.getOutputStream());
                 ObjectDecoderInputStream ois = new ObjectDecoderInputStream(socket.getInputStream())) {
                ous.writeObject(new RenameRequest(userName, fileName, newFileName));

                return handleResponse(ois.readObject());
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Server is unable", e);
            }
        }, executorService);
    }
}
