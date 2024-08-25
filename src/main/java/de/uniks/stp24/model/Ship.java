package de.uniks.stp24.model;

import de.uniks.stp24.dto.ReadShipDto;

import java.util.Map;

public record Ship(
        String createdAt,
        String updatedAt,
        String _id,
        String game,
        String empire,
        String fleet,
        String type,
        double health,
        double experience,
        Map<String, Object> _private,
        Map<String, Object> _public
) {
    public ReadShipDto toDto() {
        return new ReadShipDto(
                createdAt,
                updatedAt,
                _id,
                game,
                empire,
                fleet,
                type,
                health,
                experience,
                _public
        );
    }
}
