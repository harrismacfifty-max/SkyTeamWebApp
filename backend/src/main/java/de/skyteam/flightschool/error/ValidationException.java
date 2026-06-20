package de.skyteam.flightschool.error;

public final class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(400, message);
    }
}

