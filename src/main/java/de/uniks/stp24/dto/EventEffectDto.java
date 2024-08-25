package de.uniks.stp24.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.uniks.stp24.model.Effect;
import de.uniks.stp24.model.EventType;

import java.util.List;

public class EventEffectDto {
    @JsonProperty("id")
    private String id;
    @JsonProperty("type")
    private EventType type;
    @JsonProperty("mtth")
    private double mtth;
    @JsonProperty("duration")
    private int duration;
    @JsonProperty("effects")
    private List<Effect> effects;
    @JsonProperty("effects_accept")
    private List<List<Effect>> effects_accept;

    public EventEffectDto() {
    }

    public EventEffectDto(String id, EventType type, double mtth, int duration, List<Effect> effects, List<List<Effect>> effects_accept) {
        this.id = id;
        this.type = type;
        this.mtth = mtth;
        this.duration = duration;
        this.effects = effects;
        this.effects_accept = effects_accept;
    }

    public String id() {
        return id;
    }

    public EventType type() {
        return type;
    }

    public double mtth() {
        return mtth;
    }

    public int duration() {
        return duration;
    }

    public List<Effect> effects() {
        return effects;
    }

    public List<List<Effect>> effects_accept() {
        return effects_accept;
    }

    public EventEffectDto setId(String id) {
        this.id = id;
        return this;
    }

    public EventEffectDto setType(EventType type) {
        this.type = type;
        return this;
    }

    public EventEffectDto setMtth(double mtth) {
        this.mtth = mtth;
        return this;
    }

    public EventEffectDto setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public EventEffectDto setEffects(List<Effect> effects) {
        this.effects = effects;
        return this;
    }

    public EventEffectDto setEffects_accept(List<List<Effect>> effects_accept) {
        this.effects_accept = effects_accept;
        return this;
    }
}
