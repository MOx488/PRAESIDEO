package de.uniks.stp24.dto;

import java.util.Map;

public record UpdateShipDto(
        String fleet,
        Map<String, Object> _private,
        Map<String, Object> _public
) {
}
