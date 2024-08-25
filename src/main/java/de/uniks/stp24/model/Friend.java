package de.uniks.stp24.model;

public record Friend(
        String createdAt,
        String updatedAt,
        String _id,
        String from,
        String to,
        String status
) {
}
