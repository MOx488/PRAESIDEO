package de.uniks.stp24.model;

import java.util.List;

public record ErrorResponse(
        List<String> message,
        String error,
        int statusCode
) {
}
