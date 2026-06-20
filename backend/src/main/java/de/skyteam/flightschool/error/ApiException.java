package de.skyteam.flightschool.error;

import java.util.List;

public class ApiException extends RuntimeException {
    private final int statusCode;
    private final List<String> errors;

    public ApiException(int statusCode, String message) {
        this(statusCode, message, List.of(message));
    }

    public ApiException(int statusCode, String message, List<String> errors) {
        super(message);
        this.statusCode = statusCode;
        this.errors = List.copyOf(errors);
    }

    public int statusCode() {
        return statusCode;
    }

    public List<String> errors() {
        return errors;
    }
}

