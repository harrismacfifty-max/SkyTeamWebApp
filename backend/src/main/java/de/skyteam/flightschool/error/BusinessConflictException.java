package de.skyteam.flightschool.error;

public final class BusinessConflictException extends ApiException {
    public BusinessConflictException(String message) {
        super(409, message);
    }
}

