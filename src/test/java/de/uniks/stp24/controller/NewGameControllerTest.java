package de.uniks.stp24.controller;

import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.dto.CreateGameDto;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSettings;
import de.uniks.stp24.rest.GamesApiService;
import io.reactivex.rxjava3.core.Observable;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
class NewGameControllerTest extends ControllerTest {

    @InjectMocks
    NewGameController newGameController;
    @Mock
    GamesApiService gamesApiService;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        app.show(newGameController);
    }

    @Test
    void cancel() {
        waitForFxEvents();

        // Start:
        // Jan is currently on the new-game-screen but decides he doesn't want to create a game.
        assertEquals("PRAESIDEO - New Game", stage.getTitle());


        Mockito.doReturn(null).when(app).show("/lobby");

        // Action:
        // Jan presses the cancel-button.
        clickOn("#cancelButton");

        // Result:
        // Jan is now on the lobby-screen.
        verify(app, times(1)).show("/lobby");
    }

    @Test
    void createGame() {
        waitForFxEvents();

        // Start: Jan is currently on the new-game-screen and wants to create a game.
        assertEquals("PRAESIDEO - New Game", stage.getTitle());


        CreateGameDto createGameDto = new CreateGameDto(
                "Test Game",
                100,
                new GameSettings(50),
                "Password"
        );

        Game game = new Game(
                "2024-04-22T12:11:22.391Z",
                "2024-05-15T10:54:00.168Z",
                "662653eaa51a788b23a53c31",
                "Test Game",
                "662653d5a51a788b23a53c31",
                1,
                1,
                false,
                1,
                1,
                "",
                new GameSettings(50)
        );

        Mockito.doReturn(Observable.just(game)).when(gamesApiService).createGame(createGameDto);

        // Action:
        // Jan enters a name and password for his game. He clicks the create button.
        clickOn("#nameField").write("Test Game");

        clickOn("#passwordField").write("Password");

        Mockito.doReturn(null).when(app).show(eq("/members"), any());

        clickOn("#createButton");

        // Result:
        // Jan is now on the member screen for his new game.
        verify(app, times(1)).show(eq("/members"), any());
    }

}
