package de.uniks.stp24.model;

import java.util.List;

public record Trait(
        String id,
        List<Effect> effects,
        int cost,
        List<String> conflicts
) {
}
