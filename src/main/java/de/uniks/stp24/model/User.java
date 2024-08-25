package de.uniks.stp24.model;

public record User(
        String createdAt,
        String updatedAt,
        String _id,
        String name,
        String avatar
) implements Identifiable {
}
