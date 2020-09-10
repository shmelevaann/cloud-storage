package ru.geekbrains.chiffa.cloudstorage;

import lombok.Getter;

import java.util.UUID;

public class RenameRequest implements Request {
    @Getter
    private final UUID id = UUID.randomUUID();
    @Getter
    private final String username;
    @Getter
    private final String filename;
    @Getter
    private final String newFileName;

    public RenameRequest(String username, String fileName, String newFileName) {
        this.username = username;
        this.filename = fileName;
        this.newFileName = newFileName;
    }
}
