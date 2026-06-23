package de.skyteam.flightschool.error;

public final class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(403, message);
    }
}
