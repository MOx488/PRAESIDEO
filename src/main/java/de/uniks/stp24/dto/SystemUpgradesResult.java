package de.uniks.stp24.dto;

import de.uniks.stp24.model.SystemUpgrade;

public record SystemUpgradesResult(
        SystemUpgrade unexplored,
        SystemUpgrade explored,
        SystemUpgrade colonized,
        SystemUpgrade upgraded,
        SystemUpgrade developed
) {
}
