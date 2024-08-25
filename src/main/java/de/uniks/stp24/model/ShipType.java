package de.uniks.stp24.model;

import java.util.TreeMap;

public record ShipType(
        String id,
        TreeMap<String, Double> upkeep, double health, double speed, TreeMap<String, Integer> attack,
        TreeMap<String, Integer> defense, TreeMap<String, Double> cost, int build_time
) {
}
