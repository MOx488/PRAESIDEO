package de.uniks.stp24.model;

import java.util.Map;

public record War(
        String createdAt,
        String updatedAt,
        String _id,
        String game,
        String attacker,
        String defender,
        String name,
        Map<String, Object> _public
) implements Identifiable {
}
