package de.uniks.stp24.dto;

import de.uniks.stp24.model.EffectSource;

import java.util.List;
import java.util.Map;

public record UpdateEmpireDto(
        Map<String, Integer> resources,
        List<String> technologies,
        List<EffectSource> effects,
        Map<String, Object> _private,
        Map<String, Object> _public
) {
}
