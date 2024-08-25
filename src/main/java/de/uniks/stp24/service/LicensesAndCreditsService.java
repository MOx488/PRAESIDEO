package de.uniks.stp24.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.dto.LicensesCreditsDto;
import de.uniks.stp24.model.License;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LicensesAndCreditsService {

    @Inject
    public ObjectMapper objectMapper;

    @Inject
    public LicensesAndCreditsService() {
    }

    public LicensesCreditsDto getLicensesAndCreditsDto() throws IOException {
        // Read the licenses and credits from the file
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("de/uniks/stp24/docs/licenses_and_credits.json");
        if (inputStream == null) {
            System.out.println("Could not find licenses_and_credits.json");
            return null;
        }

        return objectMapper.readValue(inputStream, LicensesCreditsDto.class);
    }

    public String buildLicensesText(List<License> licenses) {
        StringBuilder licensesText = new StringBuilder();
        for (License license : licenses) {
            licensesText.append(license.name()).append("\n");
            for (String tool : license.tools()) {
                licensesText.append(" • ").append(tool).append("\n");
            }
            licensesText.append("\n");
        }
        return licensesText.toString();
    }

    public String buildDeveloperText(List<String> developers) {
        StringBuilder developerText = new StringBuilder();
        developerText.append("Phoenix Studio Developers:\n");

        for (String developer : developers) {
            developerText.append(" • ").append(developer).append("\n");
        }

        return developerText.toString();
    }
}
