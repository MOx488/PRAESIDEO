package de.uniks.stp24.model;

import java.util.List;

public record AggregateResult(
        int total,
        List<AggregateItem> items
) {
}
