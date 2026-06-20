package de.skyteam.flightschool.error;

import com.sun.net.httpserver.HttpExchange;
import de.skyteam.flightschool.api.HttpSupport;
import de.skyteam.flightschool.dto.ApiResponse;
import java.io.IOException;
import java.util.List;

public final class ErrorHandler {
    private ErrorHandler() {
    }

    public static void handle(HttpExchange exchange, Exception exception) throws IOException {
        if (exception instanceof ApiException apiException) {
            HttpSupport.sendResponse(exchange, apiException.statusCode(), ApiResponse.failure(
                    apiException.getMessage(),
                    apiException.errors()
            ));
            return;
        }

        if (exception instanceof IllegalArgumentException) {
            HttpSupport.sendResponse(exchange, 400, ApiResponse.failure(
                    "Validierungsfehler.",
                    List.of(exception.getMessage())
            ));
            return;
        }

        exception.printStackTrace();
        HttpSupport.sendResponse(exchange, 500, ApiResponse.failure(
                "Unerwarteter Serverfehler.",
                List.of("Ein unerwarteter Fehler ist aufgetreten.")
        ));
    }
}

