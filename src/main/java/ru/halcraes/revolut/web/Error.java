package ru.halcraes.revolut.web;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Error {
    private String message;
}
