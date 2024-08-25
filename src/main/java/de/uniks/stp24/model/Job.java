package de.uniks.stp24.model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record Job(
        String createdAt,
        String updatedAt,
        String _id,
        int progress,
        int total,
        String game,
        String empire,
        String system,
        int priority,
        String type,
        String building,
        String district,
        String technology,
        String fleet,
        String ship,
        List<String> path,
        TreeMap<String, Integer> cost,
        Map<String, Object> result
) implements Identifiable {
}
