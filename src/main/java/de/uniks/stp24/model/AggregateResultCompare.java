package de.uniks.stp24.model;

import java.util.List;

public record AggregateResultCompare(
        double total,
        List<AggregateItem> items
) {
}
