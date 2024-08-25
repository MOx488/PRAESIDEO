package de.uniks.stp24.model;


import java.util.TreeMap;

public record Building(
        String id,
        TreeMap<String, Integer> production, double health, double defense, double healing_rate,
        TreeMap<String, Integer> cost, TreeMap<String, Integer> upkeep, int build_time
) {
}
