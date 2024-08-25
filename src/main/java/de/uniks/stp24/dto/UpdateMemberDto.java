package de.uniks.stp24.dto;

import de.uniks.stp24.model.EmpireTemplate;

public record UpdateMemberDto(
        boolean ready,
        EmpireTemplate empire
) {
}
