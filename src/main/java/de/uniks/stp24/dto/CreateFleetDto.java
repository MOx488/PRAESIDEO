package de.uniks.stp24.dto;

import de.uniks.stp24.model.EffectSource;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record CreateFleetDto(
        String name,
        String location,
        TreeMap<String, Integer> size,
        Map<String, Object> _private,
        Map<String, Object> _public,
        List<EffectSource> effects
) {
}
