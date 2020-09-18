package ru.geekbrains.chiffa.cloudstorage;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
public class DownloadResponse implements Response {
    @Getter
    private final UUID id;
    @Getter
    private final String filename;
    @Getter
    private final long fileSize;

}
