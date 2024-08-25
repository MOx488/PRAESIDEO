package de.uniks.stp24.model;

public record Game(
        String createdAt,
        String updatedAt,
        String _id,
        String name,
        String owner,
        int members,
        int maxMembers,
        boolean started,
        int speed,
        int period,
        String tickedAt,
        GameSettings settings
) {
}
