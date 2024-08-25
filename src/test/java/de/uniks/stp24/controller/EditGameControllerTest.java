package de.uniks.stp24.controller;

import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.dto.CreateMemberDto;
import de.uniks.stp24.dto.UpdateGameDto;
import de.uniks.stp24.model.EmpireTemplate;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSettings;
import de.uniks.stp24.rest.GamesApiService;
import io.reactivex.rxjava3.core.Observable;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
public class EditGameControllerTest extends ControllerTest {
    @InjectMocks
    EditGameController editGameController;
    @Spy
    GamesApiService gamesApiService;

    public void start(Stage stage) throws Exception {
        super.start(stage);

        EmpireTemplate empireTemplate = new EmpireTemplate(null, null, null, 1, 1, null, List.of(), null, null, null);
        CreateMemberDto createMembersDto = new CreateMemberDto(false, empireTemplate, null);
        Game game = new Game(
                "1",
                "1",
                "1",
                "1",
                "1",
                1,
                5,
                false,
                1,
                1,
                "",
                new GameSettings(1));

        app.show(editGameController, Map.of("game", game,
                "createMemberDto", createMembersDto,
                "empireTemplate", empireTemplate));
    }


    @Test
    void cancelEditGame() {
        doReturn(null).when(app).show(eq("/members"), anyMap());

        // Start:
        // Friede is the game host and wants to edit the current game.
        // She sees the edit game screen
        assertEquals("PRAESIDEO - Edit Game", stage.getTitle());
        verifyThat("#errorLabel", hasText("Missing game name."));

        // Action:
        // Friede doesn't want to edit the game anymore and clicks on cancel
        clickOn("#cancelButton");
        waitForFxEvents();

        // Result:
        // Friede sees the member screen.
        verify(app, times(1)).show(eq("/members"), anyMap());
    }

    @Test
    void updateGame() {
        doReturn(null).when(app).show(eq("/members"), anyMap());
        doReturn(Observable.just(new Game(
                "1",
                "1",
                "1",
                "Painted World of Ariandel",
                "1",
                1,
                5,
                false,
                1,
                1,
                "",
                new GameSettings(1)))).when(gamesApiService).updateGame(any(), any());

        // Start:
        // Friede is the game host and wants to edit the current game.
        // She sees the edit game screen
        assertEquals("PRAESIDEO - Edit Game", stage.getTitle());
        verifyThat("#errorLabel", hasText("Missing game name."));

        // Action:
        // Friede edits the game and changes the name to "Painted World of Ariandel"
        // with the password "Ariandel" and mapsize 100.
        write("Painted World of Ariandel\t");
        verifyThat("#errorLabel", hasText("Missing password."));

        write("Ariandel\t");

        this.clickOn("#mapSizeBox").clickOn("100");
        verifyThat("#errorLabel", hasText(""));

        clickOn("#updateButton");
        waitForFxEvents();

        // Result:
        // Friede updates the game successfully.
        verify(gamesApiService, times(1)).updateGame("1",
                new UpdateGameDto(
                        "Painted World of Ariandel",
                        100,
                        false,
                        1,
                        new GameSettings(100),
                        "Ariandel"));
        verify(app, times(1)).show(eq("/members"), anyMap());
    }

    @Test
    void errorValidationFailed() {
        doReturn(Observable.error(new Throwable("HTTP 400 "))).when(gamesApiService).updateGame(any(), any());
        // Start:
        // Friede is the game host and wants to delete the current game.
        // She sees the edit game screen.
        assertEquals("PRAESIDEO - Edit Game", stage.getTitle());
        verifyThat("#errorLabel", hasText("Missing game name."));

        // Action:
        // Friede edits the game and changes the name to "Painted World"
        // with the password "Ariandel" and mapsize 100.
        write("Painted World\t");
        verifyThat("#errorLabel", hasText("Missing password."));

        write("Ariandel\t");

        this.clickOn("#mapSizeBox").clickOn("100");
        verifyThat("#errorLabel", hasText(""));

        clickOn("#updateButton");
        waitForFxEvents();

        // Result:
        // Friede sees the error message "Validation failed."
        verifyThat("#errorServerLabel", hasText("Validation failed."));
    }

    @Test
    void errorBearerToken() {
        doReturn(Observable.error(new Throwable("HTTP 401 "))).when(gamesApiService).updateGame(any(), any());
        // Start:
        // Friede is the game host and wants to delete the current game.
        // She sees the edit game screen.
        assertEquals("PRAESIDEO - Edit Game", stage.getTitle());
        verifyThat("#errorLabel", hasText("Missing game name."));

        // Action:
        // Friede edits the game and changes the name to "Painted World"
        // with the password "Ariandel" and mapsize 100.
        write("Painted World\t");
        verifyThat("#errorLabel", hasText("Missing password."));

        write("Ariandel\t");

        this.clickOn("#mapSizeBox").clickOn("100");
        verifyThat("#errorLabel", hasText(""));

        clickOn("#updateButton");
        waitForFxEvents();

        // Result:
        // Friede sees the error message "Missing or invalid Bearer token."
        verifyThat("#errorServerLabel", hasText("Missing or invalid bearer token."));
    }

    @Test
    void errorNotOwner() {
        doReturn(Observable.error(new Throwable("HTTP 403 "))).when(gamesApiService).updateGame(any(), any());
        // Start:
        // Friede is the game host and wants to delete the current game.
        // She sees the edit game screen.
        assertEquals("PRAESIDEO - Edit Game", stage.getTitle());
        verifyThat("#errorLabel", hasText("Missing game name."));

        // Action:
        // Friede edits the game and changes the name to "Painted World"
        // with the password "Ariandel" and mapsize 100.
        write("Painted World\t");
        verifyThat("#errorLabel", hasText("Missing password."));

        write("Ariandel\t");

        this.clickOn("#mapSizeBox").clickOn("100");
        verifyThat("#errorLabel", hasText(""));

        clickOn("#updateButton");
        waitForFxEvents();

        // Result:
        // Friede sees the error message "Attempt to change a game that the current user does not own."
        verifyThat("#errorServerLabel", hasText("Attempt to change someone else's game."));
    }

    @Test
    void errorNotFound() {
        doReturn(Observable.error(new Throwable("HTTP 404 "))).when(gamesApiService).updateGame(any(), any());
        // Start:
        // Friede is the game host and wants to delete the current game.
        // She sees the edit game screen.
        assertEquals("PRAESIDEO - Edit Game", stage.getTitle());
        verifyThat("#errorLabel", hasText("Missing game name."));

        // Action:
        // Friede edits the game and changes the name to "Painted World"
        // with the password "Ariandel" and mapsize 100.
        write("Painted World\t");
        verifyThat("#errorLabel", hasText("Missing password."));

        write("Ariandel\t");

        this.clickOn("#mapSizeBox").clickOn("100");
        verifyThat("#errorLabel", hasText(""));

        clickOn("#updateButton");
        waitForFxEvents();

        // Result:
        // Friede sees the error message "Not found"
        verifyThat("#errorServerLabel", hasText("Not found."));
    }

    @Test
    void errorGameRunning() {
        doReturn(Observable.error(new Throwable("HTTP 409 "))).when(gamesApiService).updateGame(any(), any());
        // Start:
        // Friede is the game host and wants to delete the current game.
        // She sees the edit game screen.
        assertEquals("PRAESIDEO - Edit Game", stage.getTitle());
        verifyThat("#errorLabel", hasText("Missing game name."));

        // Action:
        // Friede edits the game and changes the name to "Painted World"
        // with the password "Ariandel" and mapsize 100.
        write("Painted World\t");
        verifyThat("#errorLabel", hasText("Missing password."));

        write("Ariandel\t");

        this.clickOn("#mapSizeBox").clickOn("100");
        verifyThat("#errorLabel", hasText(""));

        clickOn("#updateButton");
        waitForFxEvents();

        // Result:
        // Friede sees the error message "Game is already running."
        verifyThat("#errorServerLabel", hasText("Game is already running."));
    }

    @Test
    void errorRateLimit() {
        doReturn(Observable.error(new Throwable("HTTP 429 "))).when(gamesApiService).updateGame(any(), any());
        // Start:
        // Friede is the game host and wants to delete the current game.
        // She sees the edit game screen.
        assertEquals("PRAESIDEO - Edit Game", stage.getTitle());
        verifyThat("#errorLabel", hasText("Missing game name."));

        // Action:
        // Friede edits the game and changes the name to "Painted World"
        // with the password "Ariandel" and mapsize 100.
        write("Painted World\t");
        verifyThat("#errorLabel", hasText("Missing password."));

        write("Ariandel\t");

        this.clickOn("#mapSizeBox").clickOn("100");
        verifyThat("#errorLabel", hasText(""));

        clickOn("#updateButton");
        waitForFxEvents();

        // Result:
        // Friede sees the error message "Rate limit reached."
        verifyThat("#errorServerLabel", hasText("Rate limit reached."));
    }
}
