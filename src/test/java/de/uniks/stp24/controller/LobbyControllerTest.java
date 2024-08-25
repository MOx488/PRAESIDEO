package de.uniks.stp24.controller;

import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.component.GameComponent;
import de.uniks.stp24.component.friends.FriendListComponent;
import de.uniks.stp24.component.friends.FriendRequestComponent;
import de.uniks.stp24.component.friends.UserComponent;
import de.uniks.stp24.component.popups.AddFriendPopUpComponent;
import de.uniks.stp24.component.popups.JoinGamePopupComponent;
import de.uniks.stp24.dto.CreateMemberDto;
import de.uniks.stp24.dto.UpdateFriendDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.*;
import de.uniks.stp24.service.AudioService;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.fulib.fx.constructs.Modals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.hasText;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
class LobbyControllerTest extends ControllerTest {
    @Mock
    EventListener eventListener;
    @Mock
    GamesApiService gamesApiService;
    @Mock
    AuthApiService authApiService;
    @Mock
    GameMembersApiService gameMembersApiService;
    @Mock
    FriendsApiService friendsApiService;
    @Mock
    UsersApiService usersApiService;
    @Mock
    AudioService audioService;
    @Mock
    TokenStorage tokenStorage;
    @Mock
    Provider<JoinGamePopupComponent> popup;
    @Spy
    Provider<GameComponent> gamesComponentProvider = spyProvider(GameComponent::new);
    @Spy
    Provider<UserComponent> userComponentProvider = spyProvider(() -> {
        final UserComponent userComponent = new UserComponent();
        userComponent.subscriber = subscriber;
        userComponent.tokenStorage = tokenStorage;
        userComponent.imageCache = imageCache;
        userComponent.friendsApiService = friendsApiService;
        return userComponent;
    });
    @Spy
    Provider<AddFriendPopUpComponent> addFriendPopUpProvider = spyProvider(() -> {
        final AddFriendPopUpComponent addFriendPopUpComponent = new AddFriendPopUpComponent();
        addFriendPopUpComponent.app = app;
        addFriendPopUpComponent.subscriber = subscriber;
        addFriendPopUpComponent.imageCache = imageCache;
        addFriendPopUpComponent.friendsApiService = friendsApiService;
        addFriendPopUpComponent.usersApiService = usersApiService;
        addFriendPopUpComponent.eventListener = eventListener;
        addFriendPopUpComponent.tokenStorage = tokenStorage;
        addFriendPopUpComponent.bundle = bundle;
        addFriendPopUpComponent.notificationService = this.notificationService;
        return addFriendPopUpComponent;
    });
    @Spy
    NotificationService notificationService;
    @InjectMocks
    JoinGamePopupComponent joinGamePopupComponent;
    @InjectMocks
    FriendListComponent friendListComponent;
    @InjectMocks
    FriendRequestComponent friendRequestComponent;
    @InjectMocks
    GameComponent gameComponent;

    @InjectMocks
    LobbyController lobbyController;

    private final Subject<Event<Game>> gameSubject = BehaviorSubject.create();
    private final Subject<Event<Friend>> friendSubject = BehaviorSubject.create();
    private final Subject<Event<Friend>> friendRequestSubject = BehaviorSubject.create();
    private final Subject<Event<User>> userSubject = BehaviorSubject.create();
    private final List<User> users = createUsers();

    private final User testUser = new User(
            null,
            null,
            "3",
            "Jan",
            null
    );


    @Override
    public void start(Stage stage) throws Exception {
        notificationService.app = this.app;
        notificationService.imageCache = this.imageCache;
        lobbyController.friendListComponent = friendListComponent;
        lobbyController.friendRequestComponent = friendRequestComponent;
        lobbyController.notificationService = notificationService;

        Mockito.doReturn(Observable.just(createGames())).when(gamesApiService).getGames();
        Mockito.doReturn(Observable.just(users)).when(usersApiService).getUsers();
        Mockito.doReturn(gameSubject).when(eventListener).listen("games.*.*", Game.class);
        Mockito.doReturn(testUser._id()).when(tokenStorage).getUserId();

        // FriendListComponent mocks
        Mockito.doReturn(friendSubject)
                .when(eventListener).listen("users." + testUser._id() + ".friends.*.*", Friend.class);
        Mockito.doReturn(Observable.just(List.of())).when(friendsApiService).getFriends(testUser._id());

        // FriendRequestComponent mocks
        Mockito.doReturn(friendRequestSubject)
                .when(eventListener).listen("users.*.friends." + testUser._id() + ".*", Friend.class);
        Mockito.doReturn(Observable.just(List.of()))
                .when(friendsApiService).getFriendsByStatus(testUser._id(), "requested");

        super.start(stage);
        app.show(lobbyController);
    }

    // ----- Friend tests ----------------------------------------------------------------------------------------------
    @Test
    void sendRequestCancel() {
        Mockito.doReturn(Observable.just(List.of(testUser))).when(usersApiService).getUsers();
        Mockito.doReturn(userSubject).when(eventListener).listen("users.*.*", User.class);

        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Jan wants to add a friend. He clicks on "New Friend". A popup shows. Jan changes his mind and cancels the
        // action.
        clickOn("New Friend");
        waitForFxEvents();
        assertEquals(1, Modals.getModalStages().size());
        clickOn("Cancel");
        waitForFxEvents();

        // Result:
        // The popup closes and Jan sees no notification.
        assertEquals(0, Modals.getModalStages().size());
    }

    @Test
    void sendRequestSuccess() {
        User joe = new User(
                null,
                null,
                "joeId",
                "Joe",
                null
        );
        Mockito.doReturn(Observable.just(List.of(testUser, joe))).when(usersApiService).getUsers();
        Mockito.doReturn(userSubject).when(eventListener).listen("users.*.*", User.class);
        Mockito.doReturn(Observable.just(new Friend(
                null,
                null,
                null,
                testUser._id(),
                joe._id(),
                "requested"
        ))).when(friendsApiService).createFriendRequest(testUser._id(), joe._id());

        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Jan wants to add a friend. He clicks on "New Friend". A popup shows and Jan sends a friend request to "Joe".
        clickOn("New Friend");
        waitForFxEvents();
        assertEquals(1, Modals.getModalStages().size());
        clickOn("#friendNameField").write("Joe");
        clickOn("Send Request");
        waitForFxEvents();

        // Result:
        // The popup closes and Jan sees a message that the friend request was sent.
        assertEquals(0, Modals.getModalStages().size());
        Label notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("Friend request sent."));
    }

    @Test
    void sendRequestUnknownUser() {
        Mockito.doReturn(Observable.just(List.of(testUser))).when(usersApiService).getUsers();
        Mockito.doReturn(userSubject).when(eventListener).listen("users.*.*", User.class);

        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Jan wants to add a friend. He clicks on "New Friend". A popup shows and Jan tries to send a friend request
        // to "Joe".
        clickOn("New Friend");
        waitForFxEvents();
        assertEquals(1, Modals.getModalStages().size());
        clickOn("#friendNameField").write("Joe");
        clickOn("Send Request");
        waitForFxEvents();

        // Result:
        // The popup does not close and Jan sees a message that the user does not exist.
        assertEquals(1, Modals.getModalStages().size());
        Label notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("User not found."));
    }

    @Test
    void sendRequestSelf() {
        Mockito.doReturn(Observable.just(List.of(testUser))).when(usersApiService).getUsers();
        Mockito.doReturn(userSubject).when(eventListener).listen("users.*.*", User.class);

        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Jan wants to add a friend. He clicks on "New Friend". A popup shows and Jan tries to send a friend request
        // to himself.
        clickOn("New Friend");
        waitForFxEvents();
        assertEquals(1, Modals.getModalStages().size());
        clickOn("#friendNameField").write("Jan");
        clickOn("Send Request");
        waitForFxEvents();

        // Result:
        // The popup does not close and Jan sees a message that he cannot befriend himself.
        assertEquals(1, Modals.getModalStages().size());
        Label notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("You cannot befriend yourself."));
    }

    @Test
    void sendRequestNewUser() {
        User joe = new User(
                null,
                null,
                "joeId",
                "Joe",
                null
        );
        Mockito.doReturn(Observable.just(List.of(testUser))).when(usersApiService).getUsers();
        Mockito.doReturn(userSubject).when(eventListener).listen("users.*.*", User.class);
        Mockito.doReturn(Observable.just(new Friend(
                null,
                null,
                null,
                testUser._id(),
                joe._id(),
                "requested"
        ))).when(friendsApiService).createFriendRequest(testUser._id(), joe._id());

        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Jan wants to add joe as a friend. He clicks on "New Friend". A popup shows and Jan tries to send a friend
        // request to "Joe".
        clickOn("New Friend");
        waitForFxEvents();
        assertEquals(1, Modals.getModalStages().size());
        clickOn("#friendNameField").write("Joe");
        waitForFxEvents();
        clickOn("Send Request");
        waitForFxEvents();

        // Result:
        // The popup does not close and Jan sees a message that the user does not exist.
        assertEquals(1, Modals.getModalStages().size());
        Label notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("User not found."));

        // Action:
        // Joe creates a new account. Jan closes the error notification and tries to send a friend request to "Joe"
        // again.
        userSubject.onNext(new Event<>("users.*.created", joe));
        Button closeNotificationButton = (Button) notificationLabel.getParent().getChildrenUnmodifiable().get(2);
        clickOn(closeNotificationButton);
        waitForFxEvents();
        clickOn("Send Request");
        waitForFxEvents();

        // Result:
        // The popup closes and Jan sees a message that the friend request was sent.
        assertEquals(0, Modals.getModalStages().size());
        notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("Friend request sent."));
    }

    @Test
    void receiveRequest() {
        Mockito.doReturn(Observable.just(new User(
                null,
                null,
                "joeId",
                "Joe",
                null
        ))).when(usersApiService).getUser("joeId");

        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Joe sends Jan a friend request.
        friendRequestSubject.onNext(new Event<>(
                "users.joeId.friends." + testUser._id() + ".created",
                new Friend(
                        null,
                        null,
                        null,
                        "joeId",
                        testUser._id(),
                        "requested"
                )
        ));

        // Result:
        // Jan sees an exclamation mark in the top right corner of the request tab.
        assertDoesNotThrow(() -> lookup("#exclamationImageView").query());

        // Action:
        // Jan clicks on the request tab to view the friend request.
        clickOn("Friend Requests");
        waitForFxEvents();

        // Result:
        // Jan sees the friend request from Joe.
        assertDoesNotThrow(() -> lookup("#requestsListView").query());
        assertEquals(1, lookup("#requestsListView").queryAs(ListView.class).getItems().size());
        verifyThat("#username", hasText("Joe"));
    }

    @Test
    void declineRequest() {
        Mockito.doReturn(Observable.just(new User(
                null,
                null,
                "joeId",
                "Joe",
                null
        ))).when(usersApiService).getUser("joeId");
        friendRequestSubject.onNext(new Event<>(
                "users.joeId.friends." + testUser._id() + ".created",
                new Friend(
                        null,
                        null,
                        "joeRequestId",
                        "joeId",
                        testUser._id(),
                        "requested"
                )
        ));
        clickOn("Friend Requests");
        waitForFxEvents();


        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby screen.
        // He has received a friend request from Joe.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#exclamationImageView").query());
        assertDoesNotThrow(() -> lookup("#requestsListView").query());
        assertEquals(1, lookup("#requestsListView").queryAs(ListView.class).getItems().size());
        verifyThat("#username", hasText("Joe"));

        // Action:
        // Jan declines the friend request.
        Mockito.doReturn(Observable.empty()).when(friendsApiService).deleteFriendOrRejectFriendRequest("joeId", testUser._id());
        clickOn("#declineImageView");
        friendRequestSubject.onNext(new Event<>(
                "users.joeId.friends." + testUser._id() + ".deleted",
                new Friend(
                        null,
                        null,
                        "joeRequestId",
                        "joeId",
                        testUser._id(),
                        "requested"
                )
        ));
        waitForFxEvents();

        // Result:
        // The friend request is removed from the list.
        assertEquals(0, lookup("#requestsListView").queryAs(ListView.class).getItems().size());
    }

    @Test
    void acceptRequest() {
        Mockito.doReturn(Observable.just(new User(
                null,
                null,
                "joeId",
                "Joe",
                null
        ))).when(usersApiService).getUser("joeId");
        friendRequestSubject.onNext(new Event<>(
                "users.joeId.friends." + testUser._id() + ".created",
                new Friend(
                        null,
                        null,
                        "joeRequestId",
                        "joeId",
                        testUser._id(),
                        "requested"
                )
        ));
        clickOn("Friend Requests");
        waitForFxEvents();


        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby screen.
        // He has received a friend request from Joe.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        assertDoesNotThrow(() -> lookup("#exclamationImageView").query());
        assertDoesNotThrow(() -> lookup("#requestsListView").query());
        assertEquals(1, lookup("#requestsListView").queryAs(ListView.class).getItems().size());
        verifyThat("#username", hasText("Joe"));

        // Action:
        // Jan accepts the friend request.
        Mockito.doReturn(Observable.empty()).when(friendsApiService).acceptFriendRequest(
                "joeId", testUser._id(), new UpdateFriendDto("accepted")
        );
        clickOn("#acceptImageView");
        friendRequestSubject.onNext(new Event<>(
                "users.joeId.friends." + testUser._id() + ".updated",
                new Friend(
                        null,
                        null,
                        "joeRequestId",
                        "joeId",
                        testUser._id(),
                        "accepted"
                )
        ));
        friendSubject.onNext(new Event<>(
                "users. + testUser._id() + .friends.joeId.created",
                new Friend(
                        null,
                        null,
                        "joeFriendId",
                        testUser._id(),
                        "joeId",
                        "accepted"
                )
        ));
        waitForFxEvents();

        // Result:
        // The friend request is removed from the list. "Joe" now shows up in Jan's friend list.
        assertEquals(0, lookup("#requestsListView").queryAs(ListView.class).getItems().size());
        assertEquals(1, lookup("#friendListView").queryAs(ListView.class).getItems().size());
        verifyThat("#username", hasText("Joe"));
    }

    @Test
    void deleteFriend() {
        Mockito.doReturn(Observable.just(new User(
                null,
                null,
                "joeId",
                "Joe",
                null
        ))).when(usersApiService).getUser("joeId");
        Friend joeFriend = new Friend(
                null,
                null,
                "joeFriendId",
                testUser._id(),
                "joeId",
                "accepted"
        );
        friendSubject.onNext(new Event<>("users." + testUser._id() + ".friends.joeId.created", joeFriend));
        waitForFxEvents();

        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby screen.
        // He has a friend named Joe.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        assertEquals(1, lookup("#friendListView").queryAs(ListView.class).getItems().size());
        verifyThat("#username", hasText("Joe"));

        // Action:
        // Jan wants to remove Joe as a friend.
        Mockito.doReturn(Observable.just(joeFriend))
                .when(friendsApiService).deleteFriendOrRejectFriendRequest(testUser._id(), "joeId");
        clickOn("#username");
        waitForFxEvents();
        clickOn("#deleteFriendButton");
        friendSubject.onNext(new Event<>("users." + testUser._id() + ".friends.joeId.deleted", joeFriend));
        waitForFxEvents();

        // Result:
        // Joe is removed from Jan's friend list. Jan sees a success notification.
        assertEquals(0, lookup("#friendListView").queryAs(ListView.class).getItems().size());
        Label notificationLabel = (Label) lookup("#notificationImage").query().getParent();
        verifyThat(notificationLabel, hasText("Joe was deleted from your friends list."));
    }

    // ----- Other -----------------------------------------------------------------------------------------------------

    @Test
    void initAndRenderTest() {
        waitForFxEvents();
        assertEquals(4, lobbyController.gameList.getItems().size());
        gameSubject.onNext(
                new Event<>(
                        "games.5.created",
                        new Game("2024-04-22T12:11:22.391Z",
                                "2024-05-15T10:54:00.168Z",
                                "5555",
                                "Game 2",
                                "662653d5a51a788b23a53c31",
                                1,
                                5,
                                false,
                                1,
                                1,
                                "",
                                new GameSettings(50)
                        )));

        waitForFxEvents();
        assertEquals(5, lobbyController.gameList.getItems().size());

        gameSubject.onNext(
                new Event<>(
                        "games.5.deleted",
                        new Game(
                                "2024-04-22T12:11:22.391Z",
                                "2024-05-15T10:54:00.168Z",
                                "5555",
                                "Game 2",
                                "662653d5a51a788b23a53c31",
                                1,
                                5,
                                false,
                                1,
                                1,
                                "",
                                new GameSettings(50)
                        )
                )
        );

        waitForFxEvents();
        assertEquals(4, lobbyController.gameList.getItems().size());
        gameSubject.onNext(
                new Event<>(
                        "games.1.updated",
                        new Game(
                                "2024-04-22T12:11:22.391Z",
                                "2024-05-15T10:54:00.168Z",
                                "662653eaa51a788b23a53c31",
                                "Game 1",
                                "662653d5a51a788b23a53c31",
                                1,
                                5,
                                false,
                                1,
                                1,
                                "",
                                new GameSettings(50)
                        )
                )
        );
        waitForFxEvents();

        clickOn("Game 1");
    }

    @Test
    void logoutTest() {
        // Start:
        // Jan is currently on the lobby-screen.
        // He wants to log out.
        waitForFxEvents();

        Mockito.doReturn(Completable.complete()).when(authApiService).logout();

        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        Mockito.doReturn(null).when(app).show("/login");

        // Action:
        // Jan presses the logout-button.
        clickOn("#logoutButton");

        // Result:
        // Jan is now on the login-screen.
        verify(app, times(1)).show("/login");
    }

    @Test
    void joinCancelTest() {
        waitForFxEvents();

        Mockito.doReturn(joinGamePopupComponent).when(popup).get();
        Mockito.doReturn(Observable.just(List.of())).when(gameMembersApiService).getMembersOfGame(any());

        // Start:
        // Jan is currently on the Lobby-Screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Jan clicks on a game named "Game 1". He clicks the join-button.
        clickOn("AGame 1");

        clickOn("#joinButton");

        // Result:
        // Jan now sees the join-game-popup. He is prompted to enter a password.
        verifyThat("#textBox", hasText("Enter the password to join AGame 1"));

        // Action:
        // Jan now remembers he forgot the password and clicks the cancel-button.
        clickOn("#cancelButton");

        // Result:
        // The popup is gone.
        Assertions.assertEquals(0, Modals.getModalStages().size());
    }

    @Test
    void joinTest() {
        waitForFxEvents();

        Mockito.doReturn(joinGamePopupComponent).when(popup).get();
        Mockito.doReturn(Observable.just(List.of())).when(gameMembersApiService).getMembersOfGame(any());

        // Start:
        // Jan is currently on the Lobby-Screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Jan clicks on the join-button.

        clickOn("#joinButton");

        // Result:
        // Nothing has happened since the button is still disabled because no game is selected yet.

        // Action:
        // Jan clicks on a game named "Game 1". He clicks the join-button.
        clickOn("AGame 1");

        clickOn("#joinButton");

        // Result:
        // Jan now sees the join-game-popup. He is prompted to enter a password.
        verifyThat("#textBox", hasText("Enter the password to join AGame 1"));

        // Action:
        // Jan clicks the confirm button.
        clickOn("#confirmButton");

        // Result:
        // Nothing has happened because Jan hasn't put in a password and the button is disabled.
        Button confirmButton = lookup("#confirmButton").query();

        Assertions.assertTrue(confirmButton.isDisabled());

        // Action:
        // Jan enters the password for the game and clicks the confirm-button.
        clickOn("#passwordField").write("Password");

        EmpireTemplate empireTemplate = new EmpireTemplate(
                "New Empire",
                "",
                "FFFFFF",
                0,
                0,
                List.of(),
                List.of(),
                null,
                null,
                "regular"
        );

        Mockito.doReturn(Observable.just(new Member(
                "2024-04-22T12:11:22.391Z",
                "2024-05-15T10:54:00.168Z",
                "AGame 1",
                "Ben",
                false,
                empireTemplate
        ))).when(gameMembersApiService).joinGame(
                "662653eaa51a788b23a53c38",
                new CreateMemberDto(false, empireTemplate, "Password")
        );

        Mockito.doReturn(null).when(app).show(eq("/members"), any());

        clickOn("#confirmButton");

        // Result:
        // Jan is now in the members-screen for "AGame 1" and the popup is gone.
        verify(app, times(1)).show(eq("/members"), any());

        waitForFxEvents();

        Assertions.assertEquals(0, Modals.getModalStages().size());
    }

    @Test
    void openUserSettingsTest() {
        waitForFxEvents();

        // Start
        // Jan is currently on the lobby-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        // Action:
        // Jan clicks the gear in the top right corner which is the user-settings-button.
        Mockito.doReturn(null).when(app).show("/userSettings");

        clickOn("#userSettingsButton");

        // Result:
        // Jan is now on the user-settings-screen.
        verify(app, times(1)).show("/userSettings");
    }

    @Test
    void newGameTest() {
        waitForFxEvents();

        // Start:
        // Jan is currently on the login-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());

        Mockito.doReturn(null).when(app).show("/newGame");

        // Action:
        // Jan clicks on the new-game-button.
        clickOn("#newGameButton");

        // Result:
        // Jan is now on the new-game-screen.
        verify(app, times(1)).show("/newGame");
    }

    @Test
        // Test sorting ascending by game name
    void sortGameNameAscendingTest() {
        waitForFxEvents();
        // Start:
        // Jan is currently on the lobby-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        // jan sees the game list and it is unsorted
        assertFalse(isSortedAscending(lobbyController.gameList.getItems().stream().map(Game::name).toList()));
        // Action:
        // Jan clicks on the sort-button.
        clickOn("#gameName");
        // Result:
        // Jan sees the games sorted by name in ascending order.
        assertEquals("AGame 1", lobbyController.gameList.getItems().get(0).name());
        assertEquals("BGame 2", lobbyController.gameList.getItems().get(1).name());
        assertEquals("CGame 3", lobbyController.gameList.getItems().get(2).name());
        assertTrue(isSortedAscending(lobbyController.gameList.getItems().stream().map(Game::name).toList()));
    }

    @Test
        // Test sorting descending by game name
    void sortGameNameDescendingTest() {
        waitForFxEvents();
        // Start:
        // Jan is currently on the lobby-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        // jan sees the game list and it is unsorted
        assertFalse(isSortedDescending(lobbyController.gameList.getItems().stream().map(Game::name).toList()));
        // Action:
        // Jan clicks on the sort-button.
        clickOn("#gameName");
        clickOn("#gameName");
        // Result:
        // Jan sees the games sorted by name in descending order.
        assertEquals("DGame 4", lobbyController.gameList.getItems().get(0).name());
        assertEquals("CGame 3", lobbyController.gameList.getItems().get(1).name());
        assertEquals("BGame 2", lobbyController.gameList.getItems().get(2).name());
        assertEquals("AGame 1", lobbyController.gameList.getItems().get(3).name());
        assertTrue(isSortedDescending(lobbyController.gameList.getItems().stream().map(Game::name).toList()));
    }

    @Test
        // Test sorting ascending by game owner
    void sortGameOwnerAscendingTest() {
        waitForFxEvents();
        // Start:
        // Jan is currently on the lobby-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        // jan sees the game list and it is unsorted
        assertFalse(isSortedAscending(getGameOwnerNames(lobbyController.gameList.getItems(), users)));
        // Action:
        // Jan clicks on the sort-button.
        clickOn("#host");
        // Result:
        // Jan sees the games sorted by owner in ascending order.
        assertEquals("Ben", getGameOwnerNames(lobbyController.gameList.getItems(), users).get(0));
        assertEquals("Jan", getGameOwnerNames(lobbyController.gameList.getItems(), users).get(1));
        assertEquals("Joe", getGameOwnerNames(lobbyController.gameList.getItems(), users).get(2));
        assertEquals("Mo", getGameOwnerNames(lobbyController.gameList.getItems(), users).get(3));
        assertTrue(isSortedAscending(getGameOwnerNames(lobbyController.gameList.getItems(), users)));
    }

    @Test
        // Test sorting descending by game owner
    void sortGameOwnerDescendingTest() {
        waitForFxEvents();
        // Start:
        // Jan is currently on the lobby-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        // jan sees the game list and it is unsorted
        assertFalse(isSortedDescending(getGameOwnerNames(lobbyController.gameList.getItems(), users)));
        // Action:
        // Jan clicks on the sort-button.
        clickOn("#host");
        clickOn("#host");
        // Result:
        // Jan sees the games sorted by owner in descending order.
        assertEquals("Mo", getGameOwnerNames(lobbyController.gameList.getItems(), users).get(0));
        assertEquals("Joe", getGameOwnerNames(lobbyController.gameList.getItems(), users).get(1));
        assertEquals("Jan", getGameOwnerNames(lobbyController.gameList.getItems(), users).get(2));
        assertEquals("Ben", getGameOwnerNames(lobbyController.gameList.getItems(), users).get(3));
        assertTrue(isSortedDescending(getGameOwnerNames(lobbyController.gameList.getItems(), users)));
    }

    @Test
        // Test sorting ascending by game player count
    void sortGamePlayerCountAscendingTest() {
        waitForFxEvents();
        // Start:
        // Jan is currently on the lobby-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        // jan sees the game list and it is unsorted
        assertFalse(isSortedAscendingInteger(lobbyController.gameList.getItems().stream().map(Game::members).toList()));
        // Action:
        // Jan clicks on the sort-button.
        clickOn("#playerCount");
        // Result:
        // Jan sees the games sorted by player count in ascending order.
        assertEquals(1, lobbyController.gameList.getItems().get(0).members());
        assertEquals(2, lobbyController.gameList.getItems().get(1).members());
        assertEquals(2, lobbyController.gameList.getItems().get(2).members());
        assertEquals(4, lobbyController.gameList.getItems().get(3).members());
        assertTrue(isSortedAscendingInteger(lobbyController.gameList.getItems().stream().map(Game::members).toList()));
    }

    @Test
        // Test sorting descending by game player count
    void sortGamePlayerCountDescendingTest() {
        waitForFxEvents();
        // Start:
        // Jan is currently on the lobby-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        // jan sees the game list and it is unsorted
        assertFalse(isSortedDescendingIntegers(lobbyController.gameList.getItems().stream().map(Game::members).toList()));
        // Action:
        // Jan clicks on the sort-button.
        clickOn("#playerCount");
        clickOn("#playerCount");
        // Result:
        // Jan sees the games sorted by player count in descending order.
        assertEquals(4, lobbyController.gameList.getItems().get(0).members());
        assertEquals(2, lobbyController.gameList.getItems().get(1).members());
        assertEquals(2, lobbyController.gameList.getItems().get(2).members());
        assertEquals(1, lobbyController.gameList.getItems().get(3).members());
        assertTrue(isSortedDescendingIntegers(lobbyController.gameList.getItems().stream().map(Game::members).toList()));
    }

    @Test
        // Test filtering by games that are created by a friend
    void filterByFriendTest() {
        Mockito.doReturn(Observable.just(new User(
                null,
                null,
                "joeId",
                "Joe",
                null
        ))).when(usersApiService).getUser("joeId");
        Friend joeFriend = new Friend(
                null,
                null,
                "joeFriendId",
                testUser._id(),
                "joeId",
                "accepted"
        );
        friendSubject.onNext(new Event<>("users." + testUser._id() + ".friends.joeId.created", joeFriend));
        waitForFxEvents();
        // Start:
        // Jan is playing PRAESIDEO. He currently sees the lobby screen.
        // He has a friend named Joe.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        assertEquals(1, lookup("#friendListView").queryAs(ListView.class).getItems().size());
        verifyThat("#username", hasText("Joe"));

        // jan sees the game list and it is unfiltered
        assertEquals(4, lobbyController.gameList.getItems().size());

        // Action:
        // Jan clicks on the filter-button.
        clickOn("#onlyFriendsGames");
        // Result:
        // Jan sees the games filtered by games that are created by a friend
        assertEquals(1, lobbyController.gameList.getItems().size());
        assertEquals("DGame 4", lobbyController.gameList.getItems().getFirst().name());
    }

    @Test
        // Test by writing the name of the game in the search field
    void searchGameTest() {
        waitForFxEvents();
        // Start:
        // Jan is currently on the lobby-screen.
        assertEquals("PRAESIDEO - Lobby", stage.getTitle());
        // jan sees the game list and it is unfiltered
        assertEquals(4, lobbyController.gameList.getItems().size());
        // Action:
        // Jan writes the name of the game in the search field
        clickOn("#searchField").write("AGame 1");
        // Result:
        // Jan sees the games filtered by the name of the game
        assertEquals(1, lobbyController.gameList.getItems().size());
        assertEquals("AGame 1", lobbyController.gameList.getItems().getFirst().name());
    }

    private List<User> createUsers() {
        return Arrays.asList(
                new User(
                        null,
                        null,
                        "662653d5a51a788b23a53c31",
                        "Mo",
                        null
                ),
                new User(
                        null,
                        null,
                        "2",
                        "Ben",
                        null
                ),
                new User(
                        null,
                        null,
                        "3",
                        "Jan",
                        null
                ),
                new User(
                        null,
                        null,
                        "joeId",
                        "Joe",
                        null
                )
        );
    }

    private List<Game> createGames() {
        return Arrays.asList(
                new Game(
                        "2024-04-22T12:11:22.391Z",
                        "2024-05-15T10:54:00.168Z",
                        "662653eaa51a788b23a53c31",
                        "CGame 3",
                        "662653d5a51a788b23a53c31",
                        4,
                        5,
                        false,
                        1,
                        1,
                        "",
                        new GameSettings(50)
                ),
                new Game(
                        "2024-04-22T12:11:22.391Z",
                        "2024-05-15T10:54:00.168Z",
                        "662653eaa51a788b23a53c38",
                        "AGame 1",
                        "2",
                        1,
                        5,
                        false,
                        1,
                        1,
                        "",
                        new GameSettings(50)
                ),
                new Game(
                        "2024-04-22T12:11:22.391Z",
                        "2024-05-15T10:54:00.168Z",
                        "662653eaa51a788b23a53c33",
                        "BGame 2",
                        "3",
                        2,
                        5,
                        false,
                        1,
                        1,
                        "",
                        new GameSettings(50)
                ),
                new Game(
                        "2024-04-22T12:11:22.391Z",
                        "2024-05-15T10:54:00.168Z",
                        "662653eaa51a788b23a53c34",
                        "DGame 4",
                        "joeId",
                        2,
                        5,
                        false,
                        1,
                        1,
                        "",
                        new GameSettings(50)
                )
        );
    }

    private boolean isSortedAscending(List<String> names) {
        if (names == null || names.size() < 2) {
            return true; // A list with 0 or 1 element is considered sorted.
        }
        for (int i = 0; i < names.size() - 1; i++) {
            if (names.get(i).compareTo(names.get(i + 1)) > 0) {
                return false; // Found a pair of elements that are not in ascending order.
            }
        }
        return true; // No unsorted pairs found, the list is sorted in ascending order.
    }
    //wirte a method which sees if a list of name are sorted in descending order

    private boolean isSortedDescending(List<String> names) {
        if (names == null || names.size() < 2) {
            return true; // A list with 0 or 1 element is considered sorted.
        }
        for (int i = 0; i < names.size() - 1; i++) {
            if (names.get(i).compareTo(names.get(i + 1)) < 0) {
                return false; // Found a pair of elements that are not in descending order.
            }
        }
        return true; // No unsorted pairs found, the list is sorted in descending order.
    }

    private boolean isSortedAscendingInteger(List<Integer> players) {
        if (players == null || players.size() < 2) {
            return true; // A list with 0 or 1 element is considered sorted.
        }
        for (int i = 0; i < players.size() - 1; i++) {
            if (players.get(i) > players.get(i + 1)) {
                return false; // Found a pair of elements that are not in ascending order.
            }
        }
        return true; // No unsorted pairs found, the list is sorted in ascending order.
    }

    private boolean isSortedDescendingIntegers(List<Integer> players) {
        if (players == null || players.size() < 2) {
            return true; // A list with 0 or 1 element is considered sorted.
        }
        for (int i = 0; i < players.size() - 1; i++) {
            if (players.get(i) < players.get(i + 1)) {
                return false; // Found a pair of elements that are not in descending order.
            }
        }
        return true; // No unsorted pairs found, the list is sorted in descending order.
    }

    //returns a list of game owners with the same order as the games
    private List<String> getGameOwnerNames(List<Game> games, List<User> users) {
        return games.stream()
                .map(game -> users.stream()
                        .filter(user -> user._id().equals(game.owner()))
                        .findFirst()
                        .map(User::name)
                        .orElse(null))
                .toList();
    }


}