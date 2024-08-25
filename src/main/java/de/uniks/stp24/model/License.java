package de.uniks.stp24.model;

import java.util.List;

public record License(
        String name,
        List<String> tools
) {
}
