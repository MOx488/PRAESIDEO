package de.uniks.stp24.dto;

import de.uniks.stp24.model.License;

import java.util.List;

public record LicensesCreditsDto(
        List<License> licenses,
        List<String> developers
) {
}
