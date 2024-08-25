package de.uniks.stp24.model;

import java.util.List;
import java.util.Map;

public record EmpireTemplate(
        String name,
        String description,
        String color,
        int flag,
        int portrait,
        List<String> traits,
        List<EffectSource> effects,
        Map<String, Object> _private,
        Map<String, Object> _public,
        String homeSystem
) {
}
