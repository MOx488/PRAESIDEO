package de.uniks.stp24.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.TreeMap;

public record ExplainedVariableWithMapValues(
        String variable,
        TreeMap<String, Double> initial,
        List<EffectSource> sources,
        @JsonProperty("final")
        TreeMap<String, Double> end
) {
}
