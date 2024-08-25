package de.uniks.stp24.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientChangeDto {
    @JsonProperty("filename")
    private String filename;
    @JsonProperty("name")
    private String name;
    @JsonProperty("stopPeriod")
    private int stopPeriod;

    public ClientChangeDto() {
    }

    public String filename() {
        return filename;
    }

    public String name() {
        return name;
    }

    public int stopPeriod() {
        return stopPeriod;
    }
}
