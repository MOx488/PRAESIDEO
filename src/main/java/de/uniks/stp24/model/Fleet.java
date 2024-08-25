package de.uniks.stp24.model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record Fleet(
        String createdAt,
        String updatedAt,
        String _id,
        String game,
        String empire,
        String name,
        String location,
        TreeMap<String, Integer> size,
        Map<String, Object> _private,
        Map<String, Object> _public,
        List<EffectSource> effects
) implements Identifiable {
}
