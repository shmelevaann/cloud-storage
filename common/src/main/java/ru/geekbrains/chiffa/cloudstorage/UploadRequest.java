package ru.geekbrains.chiffa.cloudstorage;

import lombok.Getter;

import java.util.UUID;

public class UploadRequest implements Request {
    @Getter
    private UUID id = UUID.randomUUID();
    @Getter
    private String username;
    @Getter
    private final String filename;
    @Getter
    private final long fileSize;

    public UploadRequest(String username, long fileSize, String filename) {
        this.username = username;
        this.filename = filename;
        this.fileSize = fileSize;
    }

}
