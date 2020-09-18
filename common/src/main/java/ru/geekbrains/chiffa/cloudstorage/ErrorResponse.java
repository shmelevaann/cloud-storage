package ru.geekbrains.chiffa.cloudstorage;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
public class ErrorResponse implements Response {
    @Getter
    private final UUID id;
    @Getter
    private final String text;
}
