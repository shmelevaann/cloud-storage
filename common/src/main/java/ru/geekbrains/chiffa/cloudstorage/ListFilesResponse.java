package ru.geekbrains.chiffa.cloudstorage;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class ListFilesResponse implements Response {
    @Getter
    private final UUID id;
    @Getter
    private final List<String> files;

}
