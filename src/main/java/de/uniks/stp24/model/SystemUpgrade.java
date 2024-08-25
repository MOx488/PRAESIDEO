package de.uniks.stp24.model;


import java.util.TreeMap;

public record SystemUpgrade(
        String id,
        String next,
        double capacity_multiplier, double pop_growth, TreeMap<String, Integer> cost, TreeMap<String, Integer> upkeep,
        int upgrade_time
) {
}
