package de.uniks.stp24.dto;

import de.uniks.stp24.model.Market;
import de.uniks.stp24.model.Pop;
import de.uniks.stp24.model.Technologies;

public record EmpireVariables(
        Market market,
        Pop pop,
        Technologies technologies
) {
}
