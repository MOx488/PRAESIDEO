package de.uniks.stp24.dto;

import java.util.Map;
import java.util.TreeMap;

public record ReadFleetDto(
        String createdAt,
        String updatedAt,
        String _id,
        String game,
        String empire,
        String name,
        String location,
        TreeMap<String, Integer> size,
        Map<String, Object> _public
) {
}
