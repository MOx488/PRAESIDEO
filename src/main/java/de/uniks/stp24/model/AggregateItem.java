package de.uniks.stp24.model;

public record AggregateItem(
        String variable,
        int count,
        int subtotal
) {
}
