package de.uniks.stp24.model;

import de.uniks.stp24.dto.DistrictChance;

import java.util.TreeMap;

public record District(
        String id,
        TreeMap<String, Integer> production, DistrictChance chance, TreeMap<String, Integer> cost,
        TreeMap<String, Integer> upkeep, int build_time
) {
}
