package de.uniks.stp24.controller;

import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.component.MemberComponent;
import de.uniks.stp24.component.popups.DeleteGamePopUpComponent;
import de.uniks.stp24.dto.CreateMemberDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameMembersApiService;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.service.GameService;
import de.uniks.stp24.service.MembersService;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.util.WaitForAsyncUtils;

import javax.inject.Provider;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MembersControllerTest extends ControllerTest {
    @Mock
    EventListener eventListener;
    @Mock
    GameMembersApiService gameMembersApiService;
    @Mock
    GameService gameService;
    @Mock
    UsersApiService usersApiService;
    @Mock
    MembersService membersService;
    @Mock
    TokenStorage tokenStorage;

    @Spy
    final Provider<DeleteGamePopUpComponent> deleteGamePopUpComponentProvider = new Provider<>() {
        @Override
        public DeleteGamePopUpComponent get() {
            final DeleteGamePopUpComponent deleteGameComponent = new DeleteGamePopUpComponent();
            deleteGameComponent.gameService = gameService;
            deleteGameComponent.subscriber = subscriber;
            deleteGameComponent.app = app;
            return deleteGameComponent;
        }
    };

    @Spy
    Provider<MemberComponent> memberComponentProvider = new Provider<>() {
        @Override
        public MemberComponent get() {
            final MemberComponent memberComponent = new MemberComponent();
            memberComponent.usersApiService = usersApiService;
            memberComponent.imageCache = imageCache;
            memberComponent.subscriber = subscriber;
            return memberComponent;
        }
    };

    @InjectMocks
    MembersController membersController;
    @InjectMocks
    DeleteGamePopUpComponent deleteGamePopUpComponent;


    final Subject<Event<Member>> subjectMember = BehaviorSubject.create();
    final Subject<Event<Game>> subjectGame = BehaviorSubject.create();

    @Override
    public void start(Stage stage) throws Exception {
        Mockito.when(usersApiService.getUser(Mockito.anyString())).thenAnswer(invocation -> {
            final String userId = invocation.getArgument(0);
            return Observable.just(new User("", "", userId, "test" + userId, null));
        });

        doReturn(Observable.just(new Member("", "", "", "", true, null))).when(membersService).updateMember(any(), any(), any());

        Mockito.doReturn(Observable.just(List.of(
                new Member("", "", "testGameId", "1", false, null),
                new Member("", "", "testGameId", "2", true, null),
                new Member("", "", "testGameId", "3", true, null)
        ))).when(gameMembersApiService).getMembersOfGame(Mockito.anyString());


        Mockito.doReturn(subjectMember).when(eventListener).listen("games.testGameId.members.*.*", Member.class);
        Mockito.doReturn(subjectGame).when(eventListener).listen("games.testGameId.*", Game.class);

        Mockito.doReturn("1").when(tokenStorage).getUserId();

        super.start(stage);
    }

    @Test
    void testListMemberJoin() {
        WaitForAsyncUtils.waitForFxEvents();

        // Start:
        // Jan is the owner of a lobby and sees other members in a list together with himself
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(3, membersController.memberListView.getItems().size());

        // Action:
        // Another user joins the same game lobby as Jan
        subjectMember.onNext(new Event<>("games.testGameId.members.4.created",
                new Member("", "", "testGameId", "4", false, null)));

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // The Member list of the game updates immediately
        // Jan can see the new member that joined
        assertEquals(4, membersController.memberListView.getItems().size());
    }

    @Test
    void testListMemberLeave() {
        WaitForAsyncUtils.waitForFxEvents();

        // Start:
        // Jan is the owner of a lobby and sees other members in a list together with himself
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // A member of the game leaves the lobby
        subjectMember.onNext(new Event<>("games.testGameId.members.2.deleted",
                new Member("", "", "testGameId", "2", false, null)));

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // The Member list of the game updates immediately
        // Jan can see that someone left the lobby and the member count decreased
        assertEquals(2, membersController.memberListView.getItems().size());
    }

    @Test
    void testListMemberChangeReadyStatus() {
        WaitForAsyncUtils.waitForFxEvents();

        // Start:
        // Jan is the owner of a lobby and sees other members in a list together with himself
        // The player with the name testPlayerName3 is ready as he has a checkmark next to him
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(membersController.memberListView.getItems().getLast().ready());

        // Action:
        // The player with the name testPlayerName2 changes his ready status
        subjectMember.onNext(new Event<>("games.testGameId.members.3.updated",
                new Member("", "", "testGameId", "3", false, null)));

        WaitForAsyncUtils.waitForFxEvents();
        // Result:
        // The Member list of the game updates immediately
        // testPlayerName2 loses his checkmark, other members of the game can see that he is not ready anymore
        assertFalse(membersController.memberListView.getItems().getLast().ready());
    }

    @Test
    void testGameActionTextHost() {
        WaitForAsyncUtils.waitForFxEvents();

        // Start:
        // Jan created a lobby
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(true, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        // Action:
        // Jan looks at the button text
        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan sees that he is able to start the game as he is the host
        assertEquals(membersController.btnGameAction.getText(), "Start Game");
    }

    @Test
    void testGameActionTextNonHost() {
        WaitForAsyncUtils.waitForFxEvents();

        // Start:
        // Jan joined a game
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "2", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        // Action:
        // Jan looks at the button text
        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan sees that he is only able to ready up as he is not the host of the lobby
        assertEquals(membersController.btnGameAction.getText(), "Ready");
    }

    @Test
    void testLeaveLobby() {
        doReturn(Observable.just(
                new Member("", "", "testGameId", "3", false, null))).
                when(membersService).
                leaveGame(Mockito.any(), Mockito.any());
        doReturn(null).when(app).show("/lobby");

        // Start:
        // Jan is in a lobby but has to go
        // Therefore decides to leave the lobby

        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "2", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("PRAESIDEO - Game Lobby", stage.getTitle());

        // Action:
        // Jan presses the "Return to Lobby" button
        clickOn("Return to Lobby");
        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan successfully left the lobby
        verify(membersService, times(1)).leaveGame(any(), any());
        verify(app, times(1)).show("/lobby");
    }

    @Test
    void testBuildEmpireButton() {
        doReturn(null).when(app).show(eq("/edit-empire"), anyMap());

        // Start:
        // Jan is in a lobby and wants to configure his empire
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"),
                        "empireTemplate", new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""))));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan presses the "Build Empire" button
        clickOn("Build Empire");

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan successfully left the lobby
        verify(app, times(1)).show(eq("/edit-empire"), anyMap());
    }

    @Test
    void testEditGame() {
        doReturn(null).when(app).show(eq("/editGame"), anyMap());

        // Start:
        // Jan is in a lobby and wants to configure his empire
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"),
                        "empireTemplate", new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""))));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan presses the "Build Empire" button
        clickOn("Edit Game");

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan successfully left the lobby
        verify(app, times(1)).show(eq("/editGame"), anyMap());
    }

    @Test
    void testReadyButton() {
        // Start:
        // Jan is in a lobby and is ready to play
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "2", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan presses the "Ready" button to signalize the other members of the game that he is ready
        assertEquals(membersController.btnGameAction.getText(), "Ready");

        clickOn("Ready");

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan successfully made himself ready so that others can see
        // The button text changed to "Unready"
        assertEquals(membersController.btnGameAction.getText(), "Not Ready");
    }

    @Test
    void testStartGameButtonDisable() {
        // Start:
        // Jan is the owner of the lobby and wants to start the game
        // However not everyone is ready
        Mockito.reset(tokenStorage);

        doReturn("2").when(tokenStorage).getUserId();
        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "2", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();
        // Action:
        // Jan tries to press the start button but it‘s greyed out as not everyone is ready
        // He waits until testPlayer1 becomes ready
        assertTrue(membersController.btnGameAction.disabledProperty().getValue());

        WaitForAsyncUtils.waitForFxEvents();

        subjectMember.onNext(new Event<>("games.testGameId.members.1.updated",
                new Member("", "", "testGameId", "1", true, null)));


        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // The player with the name testPlayer1 became ready
        // Jan is now able to click the start game button
        assertFalse(membersController.btnGameAction.disabledProperty().getValue());
    }

    @Test
    void testLeaveGameAsMember() {
        // Start:
        // Jan is a member of the lobby and wants to leave it
        Mockito.doReturn(Observable.just(new Member("", "", "testGameId", "1", false, null))).when(membersService).leaveGame(any(), any());
        Mockito.doReturn(null).when(app).show("/lobby");

        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "2", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();
        // Action:
        // Jan presses the "Return to Lobby" button
        clickOn("Return to Lobby");

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // Jan left the game and is now in the lobby screen again
        verify(app, times(1)).show("/lobby");
        verify(membersService, times(1)).leaveGame(any(), any());
    }

    @Test
    void testLobbyDisband() {
        // Start:
        // Jan is a member of the group
        Mockito.doReturn(null).when(app).show("/lobby");

        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "2", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // The owner is of the member disbands the group and kicks everyone
        subjectGame.onNext(new Event<>("games.testGameId.deleted",
                new Game("", "", "testGameId", "P", "2", 1, 5, false, 1, 1, "", new GameSettings(100))));

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // The lobby is disbanded
        verify(app, times(2)).show("/lobby");
    }

    @Test
    void testStartGame() {
        // Start:
        // Jan is the owner of the group and wants to start the game
        // The members of the game are already ready
        Mockito.doReturn(Observable.just(new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)))).when(gameService).startGame(any(), any());
        Mockito.doReturn(null).when(app).show(eq("/ingame"), anyMap());

        Platform.runLater(() -> app.show(membersController,
                Map.of("game", new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100)),
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan presses the start Game button
        clickOn("Start Game");

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // The game has started
        verify(gameService, times(1)).startGame(any(), any());
        verify(app, times(1)).show(eq("/ingame"), anyMap());
    }

    @Test
    void testDeleteGame() {
        // Start:
        // Jan is the owner of the group and wants to start the game
        // The members of the game are already ready
        Game game = new Game("", "", "testGameId", "P", "1", 1, 5, false, 1, 1, "", new GameSettings(100));
        Mockito.doReturn(Observable.just(game)).when(gameService).deleteGame(any());
        Mockito.doReturn(null).when(app).show("/lobby");
        Mockito.doReturn(deleteGamePopUpComponent).when(deleteGamePopUpComponentProvider).get();

        Platform.runLater(() -> app.show(membersController,
                Map.of("game", game,
                        "createMemberDto", new CreateMemberDto(false, new EmpireTemplate("", "", "", 0, 0, null, List.of(), null, null, ""), "string"))));

        WaitForAsyncUtils.waitForFxEvents();

        // Action:
        // Jan presses the delete Game button
        clickOn("Delete Game");

        WaitForAsyncUtils.waitForFxEvents();

        clickOn("Yes");

        WaitForAsyncUtils.waitForFxEvents();

        // Result:
        // The lobby is disbanded
        verify(gameService, times(1)).deleteGame(any());
        verify(app, times(1)).show("/lobby");
    }

}