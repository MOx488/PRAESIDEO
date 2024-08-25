package de.uniks.stp24.dto;

import de.uniks.stp24.model.EffectSource;

import java.util.List;
import java.util.Map;

public record ReadEmpireDto(
        String createdAt,
        String updatedAt,
        String _id,
        String game,
        String user,
        String name,
        String description,
        String color,
        int flag,
        int portrait,
        String homeSystem,
        List<EffectSource> effects,
        Map<String, Object> _public
) {
}
