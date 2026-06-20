package de.skyteam.flightschool.api;

import de.skyteam.flightschool.error.ValidationException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class JsonUtil {
    private JsonUtil() {
    }

    public record RawJson(String value) {
    }

    public static RawJson raw(String value) {
        return new RawJson(value);
    }

    public static Map<String, Object> fields(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("JSON fields require key/value pairs.");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return values;
    }

    public static String object(Map<String, Object> values) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        values.forEach((key, value) -> joiner.add(quote(key) + ":" + value(value)));
        return joiner.toString();
    }

    public static String arrayOfJson(Collection<String> jsonItems) {
        return "[" + String.join(",", jsonItems) + "]";
    }

    public static String array(Collection<?> values) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        values.forEach(value -> joiner.add(value(value)));
        return joiner.toString();
    }

    public static Map<String, String> parseObject(String json) {
        return new Parser(json == null ? "" : json).parseObject();
    }

    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof RawJson rawJson) {
            return rawJson.value();
        }
        if (value instanceof Collection<?> collection) {
            return array(collection);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> objectValues = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> objectValues.put(String.valueOf(key), mapValue));
            return object(objectValues);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        StringBuilder quoted = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\b' -> quoted.append("\\b");
                case '\f' -> quoted.append("\\f");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (current < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) current));
                    } else {
                        quoted.append(current);
                    }
                }
            }
        }
        quoted.append('"');
        return quoted.toString();
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private Map<String, String> parseObject() {
            skipWhitespace();
            if (isEnd()) {
                return new LinkedHashMap<>();
            }
            expect('{');
            skipWhitespace();

            Map<String, String> values = new LinkedHashMap<>();
            if (peek('}')) {
                index++;
                return values;
            }

            while (!isEnd()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                values.put(key, parseValue());
                skipWhitespace();
                if (peek(',')) {
                    index++;
                    continue;
                }
                if (peek('}')) {
                    index++;
                    skipWhitespace();
                    if (!isEnd()) {
                        throw error();
                    }
                    return values;
                }
                throw error();
            }

            throw error();
        }

        private String parseValue() {
            if (peek('"')) {
                return parseString();
            }
            if (startsWith("null")) {
                index += 4;
                return null;
            }
            int start = index;
            while (!isEnd() && text.charAt(index) != ',' && text.charAt(index) != '}') {
                index++;
            }
            String raw = text.substring(start, index).trim();
            if (raw.isEmpty()) {
                throw error();
            }
            return raw;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!isEnd()) {
                char current = text.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current != '\\') {
                    builder.append(current);
                    continue;
                }
                if (isEnd()) {
                    throw error();
                }
                char escape = text.charAt(index++);
                switch (escape) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(parseUnicode());
                    default -> throw error();
                }
            }
            throw error();
        }

        private char parseUnicode() {
            if (index + 4 > text.length()) {
                throw error();
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error();
            }
        }

        private void expect(char expected) {
            if (!peek(expected)) {
                throw error();
            }
            index++;
        }

        private boolean peek(char expected) {
            return !isEnd() && text.charAt(index) == expected;
        }

        private boolean startsWith(String value) {
            return text.startsWith(value, index);
        }

        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private boolean isEnd() {
            return index >= text.length();
        }

        private ValidationException error() {
            return new ValidationException("Ungueltiges JSON.");
        }
    }
}


