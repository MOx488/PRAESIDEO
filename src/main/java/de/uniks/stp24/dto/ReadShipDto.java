package de.uniks.stp24.dto;

import de.uniks.stp24.model.Ship;

import java.util.Map;

public record ReadShipDto(
        String createdAt,
        String updatedAt,
        String _id,
        String game,
        String empire,
        String fleet,
        String type,
        double health,
        double experience,
        Map<String, Object> _public
) {
    public Ship toShip() {
        return new Ship(
                createdAt,
                updatedAt,
                _id,
                game,
                empire,
                fleet,
                type,
                health,
                experience,
                null,
                _public
        );
    }
}
