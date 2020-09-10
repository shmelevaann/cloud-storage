package ru.geekbrains.chiffa.cloudstorage;

import lombok.Getter;

import java.util.UUID;

public class DownloadRequest implements Request {
    @Getter
    private UUID id = UUID.randomUUID();
    @Getter
    private String username;
    @Getter
    private String filename;

    public DownloadRequest(String username, String filename) {
        this.username = username;
        this.filename = filename;
    }
}
