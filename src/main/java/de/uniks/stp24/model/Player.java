package de.uniks.stp24.model;

public record Player(
        String _id,
        int flag,
        String color,
        String name,
        String empireId,
        int portrait,
        String military,
        String economy,
        String technology
) {
}
