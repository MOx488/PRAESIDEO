package de.uniks.stp24.dto;

import de.uniks.stp24.model.Resource;

public record ResourcesResult(
        Resource credits,
        Resource population,
        Resource energy,
        Resource minerals,
        Resource food,
        Resource fuel,
        Resource research,
        Resource alloys,
        Resource consumer_goods
) {
}
