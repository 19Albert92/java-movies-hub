package ru.practicum.moviehub.exception;

import java.util.List;

public class BodyValidateException extends RuntimeException {

    private final List<String> details;

    public BodyValidateException(String message, final List<String> details) {
        super(message);
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
