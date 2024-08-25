package de.uniks.stp24.model;


import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record Empire(
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
        List<String> traits,
        TreeMap<String, Integer> resources,
        List<String> technologies,
        List<EffectSource> effects,
        Map<String, Object> _private,
        Map<String, Object> _public
) {
}
