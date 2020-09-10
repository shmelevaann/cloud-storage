package ru.geekbrains.chiffa.cloudstorage;


import lombok.Getter;

import java.util.UUID;

public class ListFilesRequest implements Request {
    @Getter
    private UUID id = UUID.randomUUID();
    @Getter
    private String username;

    public ListFilesRequest(String username) {
        this.username = username;
    }
}
