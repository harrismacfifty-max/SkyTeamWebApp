package de.skyteam.flightschool.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.skyteam.flightschool.dto.ApiResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class HttpSupport {
    private HttpSupport() {
    }

    public static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return false;
        }
        addCors(exchange.getResponseHeaders());
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return true;
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    public static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        addCors(headers);
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    public static void sendResponse(HttpExchange exchange, int status, ApiResponse<?> response) throws IOException {
        sendJson(exchange, status, ApiJson.response(response));
    }

    public static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendResponse(exchange, status, ApiResponse.failure(message, java.util.List.of(message)));
    }

    private static void addCors(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}


