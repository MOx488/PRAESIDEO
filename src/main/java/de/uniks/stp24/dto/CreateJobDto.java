package de.uniks.stp24.dto;

import java.util.List;

public record CreateJobDto(
        String system,
        int priority,
        String type,
        String building,
        String district,
        String technology,
        String fleet,
        String ship,
        List<String> path
) {
}
