package de.uniks.stp24.controller;

import de.uniks.stp24.App;
import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.service.LoginService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
class GameLaunchControllerTest extends ControllerTest {
    @InjectMocks
    GameLaunchController launchController;
    @Mock
    LoginService loginService;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        app.show(launchController);
    }

    @Test
    void showLogos() {
        // Start: Jan heard about the game "PRAESIDEO"
        // and wants to try it out.

        // Action: Jan clicks on the game and starts it.

        // End: The game starts and displays the launch screen,
        // where two logos are shown, one from "Dead Birds Society"
        // and one from "Phoenix Studio".

        assertEquals("PRAESIDEO - Launching", stage.getTitle());

        Image dead_Birds_Image = new Image(Objects.requireNonNull(App.class.getResource("image/dead_birds_society_logo_round.png")).toString());
        assertEquals(dead_Birds_Image.getUrl(), ((ImageView) lookup("#deadBirdsImageView").query()).getImage().getUrl());

        Image phoenix_Image = new Image(Objects.requireNonNull(App.class.getResource("image/phoenix_studio_logo_round.png")).toString());
        assertEquals(phoenix_Image.getUrl(), ((ImageView) lookup("#phoenixImageView").query()).getImage().getUrl());
    }

    @Test
    void closeGameLaunch() {
        doReturn(null).when(app).show("/login");
        // Start: Jan heard about the game "PRAESIDEO"
        // and wants to try it out.

        // Action: Jan clicks on the game and starts it.
        // Jan sees the GameLaunch screen and clicks Enter to switch to Login Screen.

        assertEquals("PRAESIDEO - Launching", stage.getTitle());

        press(KeyCode.ENTER);

        waitForFxEvents();

        // End: Jan sees the Login screen.

        verify(app, times(1)).show("/login");
    }

    @AfterAll
    public static void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

}
