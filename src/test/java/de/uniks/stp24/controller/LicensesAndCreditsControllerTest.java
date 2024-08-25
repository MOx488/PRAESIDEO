package de.uniks.stp24.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.dto.LicensesCreditsDto;
import de.uniks.stp24.service.LicensesAndCreditsService;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.matcher.control.TextMatchers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
public class LicensesAndCreditsControllerTest extends ControllerTest {
    @InjectMocks
    LicensesAndCreditsController licensesAndCreditsController;
    @Spy
    LicensesAndCreditsService licensesAndCreditsService;
    @Spy
    ObjectMapper objectMapper;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        licensesAndCreditsService.objectMapper = objectMapper;

        app.show(licensesAndCreditsController);
    }

    @Test
    void setTexts() throws IOException {
        waitForFxEvents();

        assertEquals("PRAESIDEO - Licenses and Credits", stage.getTitle());

        // Check if the licenses and credits are displayed correctly
        LicensesCreditsDto licensesCreditsDto = licensesAndCreditsService.getLicensesAndCreditsDto();
        verifyThat("#licensesText", TextMatchers.hasText(licensesAndCreditsService.buildLicensesText(licensesCreditsDto.licenses())));
        verifyThat("#creditsText", TextMatchers.hasText(licensesAndCreditsService.buildDeveloperText(licensesCreditsDto.developers())));
    }

    @Test
    void backToLogin() {
        waitForFxEvents();

        Mockito.doReturn(null).when(app).show("/login");

        // Start:
        // Jan has launched Praesideo. He sees the licenses and credits screen.
        assertEquals("PRAESIDEO - Licenses and Credits", stage.getTitle());

        // Action:
        // Jan wants to go back to the login screen. He clicks the back button.
        clickOn("Back");

        waitForFxEvents();

        // Result:
        // Jan sees the login screen.
        verify(app, times(1)).show("/login");
    }
}
