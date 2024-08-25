package de.uniks.stp24.model;

import java.util.List;

public record Technology(
        String id,
        List<String> tags,
        int cost,
        List<String> requires,
        List<String> precedes,
        List<Effect> effects
) {
}
