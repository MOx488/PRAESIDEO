package de.uniks.stp24.dto;

import de.uniks.stp24.model.EmpireTemplate;

public record CreateMemberDto(
        boolean ready,
        EmpireTemplate empire,
        String password
) {
}
