package de.skyteam.flightschool.error;

public final class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(401, message);
    }
}

