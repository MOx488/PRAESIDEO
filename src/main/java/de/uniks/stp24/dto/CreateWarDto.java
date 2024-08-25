package de.uniks.stp24.dto;

import java.util.Map;

public record CreateWarDto(
        String attacker,
        String defender,
        String name,
        Map<String, Object> _public
) {
}
