package de.skyteam.flightschool.service;

import de.skyteam.flightschool.error.ValidationException;
import java.util.Map;

final class ServiceSupport {
    private ServiceSupport() {
    }

    static String required(Map<String, String> data, String field) {
        String value = data.get(field);
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " ist erforderlich.");
        }
        return value.trim();
    }

    static String optional(Map<String, String> data, String field, String fallback) {
        String value = data.get(field);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    static long requiredLong(Map<String, String> data, String field) {
        try {
            return Long.parseLong(required(data, field));
        } catch (NumberFormatException exception) {
            throw new ValidationException(field + " muss eine Zahl sein.");
        }
    }

    static int optionalInt(Map<String, String> data, String field, int fallback) {
        String value = data.get(field);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new ValidationException(field + " muss eine Zahl sein.");
        }
    }
}


