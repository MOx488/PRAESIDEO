package de.uniks.stp24.dto;

import java.util.List;
import java.util.Map;

public record UpdateSystemDto(
        String name,
        Map<String, Integer> districts,
        List<String> buildings,
        String upgrade,
        String owner,
        Map<String, Object> _public
) {
}
