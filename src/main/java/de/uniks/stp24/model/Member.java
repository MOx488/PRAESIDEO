package de.uniks.stp24.model;

public record Member(
        String createdAt,
        String updatedAt,
        String game,
        String user,
        boolean ready,
        EmpireTemplate empire
) {
}
