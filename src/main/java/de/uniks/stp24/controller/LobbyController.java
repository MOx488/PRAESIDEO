package de.uniks.stp24.controller;

import de.uniks.stp24.component.friends.FriendListComponent;
import de.uniks.stp24.component.friends.FriendRequestComponent;
import de.uniks.stp24.model.Friend;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Member;
import de.uniks.stp24.model.User;
import de.uniks.stp24.ws.Event;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.SubComponent;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.constructs.Modals;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;
import java.util.*;

import static de.uniks.stp24.util.Methods.addNewFriendToList;
import static de.uniks.stp24.util.Methods.initListView;
import static org.fulib.fx.FulibFxApp.FX_SCHEDULER;

@Controller
@Title("Lobby")
public class LobbyController extends BaseController {
    @FXML
    ImageView userSettingsButton;
    @FXML
    ListView<Game> gameList;
    @FXML
    HBox gameNameHbox;
    @FXML
    Text gameName;
    @FXML
    ImageView arrowGameName;
    @FXML
    HBox hostHbox;
    @FXML
    Text host;
    @FXML
    ImageView arrowHost;
    @FXML
    HBox playerCountHbox;
    @FXML
    Text playerCount;
    @FXML
    ImageView arrowPlayerCount;
    @FXML
    Button joinButton;
    @FXML
    Button newGameButton;
    @FXML
    Button logoutButton;
    @FXML
    Pane friendListContainer;
    @FXML
    Pane requestListContainer;
    @FXML
    CheckBox onlyFriendsGames;
    @FXML
    TextField searchField;

    @SubComponent
    @Inject
    FriendListComponent friendListComponent;

    @SubComponent
    @Inject
    FriendRequestComponent friendRequestComponent;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<Game> games = FXCollections.observableArrayList();
    private final ObservableList<User> userFriendList = FXCollections.observableArrayList();

    private ChangeListener<Boolean> onlyFriendsGamesListener;
    private ChangeListener<String> searchFieldListener;

    private boolean ascendingGameName = false;
    private boolean ascendingHost = false;
    private boolean ascendingPlayerCount = false;


    @Inject
    public LobbyController() {
    }

    @OnInit
    void init() {
        discordActivityService.setActivity(bundle.getString("discord.browsing.games"), "");

        this.stopSound();
        this.initializeGamesAndUsers();

        Locale.setDefault(prefService.getLocale());
        this.initializeFriendList();

    }

    private void initializeFriendList() {
        // Dynamically update friend list
        subscriber.subscribe(
                eventListener.listen("users." + tokenStorage.getUserId() + ".friends.*.*", Friend.class),
                this::handleOutgoingFriendEvents
        );

        // Get access to friends
        subscriber.subscribe(friendsApiService.getFriends(tokenStorage.getUserId()), friends -> {
            List<String> friendIds = friends.stream().map(Friend::to).toList();
            if (friendIds.isEmpty()) {
                return;
            }

            subscriber.subscribe(usersApiService.getUsersByIDs(friendIds), userFriendList::setAll);
        });
    }

    private void handleOutgoingFriendEvents(Event<Friend> event) {
        switch (event.suffix()) {
            case "created", "updated" -> addNewFriendToList(event.data(), userFriendList, usersApiService, subscriber);
            case "deleted" -> userFriendList.removeIf(user -> user._id().equals(event.data().to()));
        }
    }

    private void initializeGamesAndUsers() {
        subscriber.subscribe(gamesApiService.getGames(), serverGames -> {
                    this.games.setAll(serverGames);
                    subscriber.subscribe(usersApiService.getUsers(), serverUsers -> {
                        this.users.setAll(serverUsers);
                        this.render();
                    });
                }
        );

        subscriber.subscribe(eventListener.listen("games.*.*", Game.class), event -> {
            final Game game = event.data();
            switch (event.suffix()) {
                case "created" -> games.add(game);
                case "updated" -> {
                    games.replaceAll(u -> u._id().equals(game._id()) ? game : u);
                    if (arrowHost != null && arrowHost.getImage() != null) {
                        if (ascendingHost) {
                            sortAscendingHost();
                        } else {
                            sortDescendingHost();
                        }
                    } else if (arrowGameName != null && arrowGameName.getImage() != null) {
                        if (ascendingGameName) {
                            sortAscendingGameName();
                        } else {
                            sortDescendingGameName();
                        }
                    } else if (arrowPlayerCount != null && arrowPlayerCount.getImage() != null) {
                        if (ascendingPlayerCount) {
                            sortAscendingPlayerCount();
                        } else {
                            sortDescendingPlayerCount();
                        }
                    }
                }
                case "deleted" -> games.removeIf(u -> u._id().equals(game._id()));
            }
        });
    }

    private void stopSound() {
        if (GraphicsEnvironment.isHeadless() || !this.audioService.isPlayed) {
            return;
        }

        this.audioService.stopSound();
        this.audioService.isPlayed = false;
    }


    void render() {
        final BooleanBinding noGameSelected = gameList.getSelectionModel().selectedItemProperty().isNull();
        gameName.setText(bundle.getString("game.name"));
        playerCount.setText(bundle.getString("player.count"));
        onlyFriendsGames.setText(bundle.getString("only.show.games.of.friends"));
        searchField.setPromptText(bundle.getString("search.by.game.name"));

        initListView(gameList, games, app, gameComponentProvider, Map.of("users", users, "games", games));

        final BooleanBinding waiting = notWaiting.not();
        renderSearchField();
        onlyFriendsGames.selectedProperty().addListener(onlyFriendsGamesListener = (observable, oldValue, newValue) -> {
            if (newValue) {
                gameList.setItems(games.filtered(game -> userFriendList.stream()
                        .anyMatch(user -> user._id().equals(game.owner()))));
            } else {
                gameList.setItems(games);
            }
        });

        friendListContainer.getChildren().add(friendListComponent);
        requestListContainer.getChildren().add(friendRequestComponent);

        joinButton.disableProperty().bind(noGameSelected);
        logoutButton.disableProperty().bind(waiting);
    }

    private void renderSearchField() {
        searchField.textProperty().addListener(searchFieldListener = (observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                gameList.setItems(games);
                return;
            }

            gameList.setItems(games.filtered(game -> game.name().toLowerCase().contains(newValue.toLowerCase())));
        });
    }

    public void sortGamesName() {
        arrowHost.setImage(null);
        arrowPlayerCount.setImage(null);
        if (!ascendingGameName) {
            arrowGameName.setImage(imageCache.get("image/arrow_up.png"));
            sortAscendingGameName();
            ascendingGameName = true;
        } else {
            arrowGameName.setImage(imageCache.get("image/arrow_down.png"));
            sortDescendingGameName();
            ascendingGameName = false;
        }
    }

    private void sortDescendingGameName() {
        // Sort all games by name in descending order, considering uppercase, lowercase, numbers, and symbols
        List<Game> sortedGames = games.stream()
                .sorted(Comparator.comparing((Game game) -> game.name().toLowerCase()).reversed())
                .toList();

        // Update the original games list
        games.clear();
        games.addAll(sortedGames);
    }

    private void sortAscendingGameName() {
        // Sort all games by name, considering uppercase, lowercase, numbers, and symbols
        List<Game> sortedGames = games.stream()
                .sorted(Comparator.comparing(game -> game.name().toLowerCase()))
                .toList();

        // Update the original games list
        games.clear();
        games.addAll(sortedGames);
    }

    public void sortGamesHost() {
        arrowGameName.setImage(null);
        arrowPlayerCount.setImage(null);
        if (!ascendingHost) {
            arrowHost.setImage(imageCache.get("image/arrow_up.png"));
            sortAscendingHost();
            ascendingHost = true;
        } else {
            arrowHost.setImage(imageCache.get("image/arrow_down.png"));
            sortDescendingHost();
            ascendingHost = false;
        }
    }

    private void sortDescendingHost() {
        // Filter and sort games with uppercase owner names
        List<Game> uppercaseOwnerGames = getGamesByOwnerAndCase(true);

        // Filter and sort games with lowercase owner names
        List<Game> lowercaseOwnerGames = getGamesByOwnerAndCase(false);

        // Merge both lists
        List<Game> sortedGames = new ArrayList<>(uppercaseOwnerGames);
        sortedGames.addAll(lowercaseOwnerGames);

        // Update the original games list
        games.clear();
        games.addAll(sortedGames);
    }

    private List<Game> getGamesByOwnerAndCase(boolean checkUpperCase) {
        return games.stream()
                .filter(game -> {
                    User owner = users.stream().filter(user -> user._id().equals(game.owner())).findFirst().orElse(null);
                    if (checkUpperCase) {
                        return owner != null && Character.isUpperCase(owner.name().charAt(0));
                    } else {
                        return owner != null && Character.isLowerCase(owner.name().charAt(0));
                    }
                })
                .sorted((o1, o2) -> {
                    User owner1 = users.stream().filter(user -> user._id().equals(o1.owner())).findFirst().orElse(null);
                    User owner2 = users.stream().filter(user -> user._id().equals(o2.owner())).findFirst().orElse(null);
                    if (owner1 == null || owner2 == null)
                        return 0;
                    return owner2.name().compareTo(owner1.name());
                })
                .toList();
    }

    private void sortAscendingHost() {
        // Filter and sort games with uppercase owner names
        List<Game> uppercaseOwnerGames = games.stream()
                .filter(game -> {
                    User owner = users.stream().filter(user -> user._id().equals(game.owner())).findFirst().orElse(null);
                    return owner != null && Character.isUpperCase(owner.name().charAt(0));
                })
                .sorted(Comparator.comparing(game -> {
                    User owner = users.stream().filter(user -> user._id().equals(game.owner())).findFirst().orElse(null);
                    if (owner == null)
                        return "";
                    return owner.name();
                }))
                .toList();

        // Filter and sort games with lowercase owner names
        List<Game> lowercaseOwnerGames = games.stream()
                .filter(game -> {
                    User owner = users.stream().filter(user -> user._id().equals(game.owner())).findFirst().orElse(null);
                    return owner != null && Character.isLowerCase(owner.name().charAt(0));
                })
                .sorted(Comparator.comparing(game -> {
                    User owner = users.stream().filter(user -> user._id().equals(game.owner())).findFirst().orElse(null);
                    if (owner == null)
                        return "";
                    return owner.name();
                }))
                .toList();

        // Merge both lists
        List<Game> sortedGames = new ArrayList<>(lowercaseOwnerGames);
        sortedGames.addAll(uppercaseOwnerGames);

        // Update the original games list
        games.clear();
        games.addAll(sortedGames);
    }

    public void sortGamesPlayerCount() {
        arrowGameName.setImage(null);
        arrowHost.setImage(null);
        if (!ascendingPlayerCount) {
            arrowPlayerCount.setImage(imageCache.get("image/arrow_down.png"));
            sortAscendingPlayerCount();
            ascendingPlayerCount = true;
        } else {
            arrowPlayerCount.setImage(imageCache.get("image/arrow_up.png"));
            sortDescendingPlayerCount();
            ascendingPlayerCount = false;
        }
    }

    private void sortDescendingPlayerCount() {
        games.sort((o1, o2) -> Integer.compare(o2.members(), o1.members()));
    }

    private void sortAscendingPlayerCount() {
        games.sort(Comparator.comparingInt(Game::members));
    }


    public void join() {
        final Game game = gameList.getSelectionModel().getSelectedItem();

        subscriber.subscribe(gameMembersApiService.getMembersOfGame(game._id()), result -> {
            //Hot Join only when game already started
            if (game.started()) {
                for (Member member : result) {
                    if (!member.user().equals(tokenStorage.getUserId())) {
                        continue;
                    }
                    this.app.show("/ingame", Map.of("game", game));
                    return;
                }
            }


            //Hot join didn't work -> we aren't a member of the game

            new Modals(app).modal(joinGamePopupComponentProvider.get())
                    .dialog(true)
                    .params(Map.of("game", game))
                    .show();
        });
    }

    public void logout() {
        notWaiting.set(false);
        subscriber.subscribe(authApiService.logout()
                .observeOn(FX_SCHEDULER)
                .doOnError(onError -> notWaiting.set(true))
                .doOnComplete(() -> {
                    prefService.removeRefreshToken();
                    app.show("/login");
                })
                .subscribe()
        );
    }


    public void openUserSettings() {
        app.show("/userSettings");
    }

    public void newGame() {
        app.show("/newGame");
    }

    @OnDestroy
    void onDestroy() {
        super.destroy();
        if (onlyFriendsGamesListener != null) {
            onlyFriendsGames.selectedProperty().removeListener(onlyFriendsGamesListener);
        }
        if (searchFieldListener != null) {
            searchField.textProperty().removeListener(searchFieldListener);
        }
    }
}
