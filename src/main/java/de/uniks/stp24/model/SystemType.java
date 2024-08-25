package de.uniks.stp24.model;

import java.util.List;

public record SystemType(
        String id,
        double chance,
        List<Integer> capacity_range,
        double district_percentage
) {

}
