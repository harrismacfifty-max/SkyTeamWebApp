package de.skyteam.flightschool.error;

public final class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(404, message);
    }
}

