package de.uniks.stp24.controller;

import de.uniks.stp24.component.*;
import de.uniks.stp24.component.events.EventPreviewComponent;
import de.uniks.stp24.component.players.ContactsViewComponent;
import de.uniks.stp24.component.players.PlayerListComponent;
import de.uniks.stp24.component.troopview.TroopViewComponent;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.*;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnKey;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.Modals;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static de.uniks.stp24.util.Methods.fillPlayerList;
import static de.uniks.stp24.util.Methods.onBasicListEvent;
import static javafx.scene.input.KeyCode.ESCAPE;
import static org.fulib.fx.FulibFxApp.FX_SCHEDULER;

@Controller
@Title("%ingame.title")
public class IngameController extends BaseController {
    public StackPane battleResult;
    @FXML VBox contactsContainer;
    @FXML VBox rightSideBarVbox;
    @FXML AnchorPane castleListContainer;
    @FXML AnchorPane ingameAnchorPane;
    @FXML StackPane ingameRoot;
    @FXML VBox avatarContainer;
    @FXML ImageView avatar;
    @FXML HBox resourceBarContainer;
    @FXML AnchorPane playerListContainer;
    @FXML Pane pauseTextContainer;
    @FXML VBox vBoxRoot;
    @FXML VBox sideButtons;
    @FXML VBox troopsListContainer;
    @FXML AnchorPane overlapContainer;

    private CastleViewComponent castleView;
    private List<ReadEmpireDto> empiresInGame;
    private List<Player> players;
    private List<User> users;

    private final ObservableList<GameSystem> systems = FXCollections.observableArrayList();
    private final ObservableList<GameSystem> updatedSystems = FXCollections.observableArrayList();
    private final ObservableList<Fleet> fleets = FXCollections.observableArrayList();
    private final ObservableList<War> wars = FXCollections.observableArrayList();
    private final ObservableList<String> warEnemies = FXCollections.observableArrayList();
    private final Graph<String, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

    private EventPreviewComponent eventPreviewComponent;
    private CastleComponent selectedCastle = null;
    private SideButtonsComponent sideButtonsComponent;
    private Empire empire;
    private boolean isHost;
    private boolean hasCastlesBeenPopulated;
    private Disposable systemsWebsocketDisposable;

    @Param("game")
    Game game;

    private final ListChangeListener<Node> castleContainerListener = change -> {
        ObservableList<? extends Node> nodeList = change.getList();

        if (nodeList.isEmpty() || nodeList.size() != this.systems.size()) {
            return;
        }

        //signalise zoomDragComponent that it should place the castles
        this.zoomDragComponent.signalizePlacing();

        //unsubscribe systems websocket
        this.systemsWebsocketDisposable.dispose();
    };

    private final ListChangeListener<Node> ingameAnchorPaneChildrenListener = change -> {
        ObservableList<? extends Node> nodeList = change.getList();

        final Node castleViewNode = nodeList.stream().filter(node -> node instanceof CastleViewComponent).findFirst().orElse(null);
        if (castleViewNode == null) {
            this.castleView = null;
            return;
        }

        //center our castle view inside the effective bounds
        this.castleView = (CastleViewComponent) castleViewNode;
        this.setCastleViewAnchorBounds();
    };


    private void setCastleViewAnchorBounds() {
        if (this.castleView == null) {
            return;
        }

        final Rectangle2D effectiveBounds = this.getEffectiveBounds();

        AnchorPane.setTopAnchor(castleView, effectiveBounds.getMinY());
        AnchorPane.setLeftAnchor(castleView, effectiveBounds.getMinX());

        AnchorPane.setRightAnchor(castleView, 0d);
        AnchorPane.setBottomAnchor(castleView, 0d);

        //start castle view below avatar container
        this.castleView.setLayoutY(avatarContainer.getLayoutY() + avatarContainer.getHeight());
    }

    @Inject
    public IngameController() {
    }

    @OnInit
    public void onInit() {
        this.discordActivityService.setActivity(bundle.getString("discord.in.game.uppercase"), game.name());

        this.isHost = this.game.owner().equals(tokenStorage.getUserId());
        this.initUsers();
        this.initializeSystems();
        this.audioService.init(prefService.getVolume());
    }

    private void initUsers() {
        subscriber.subscribe(usersApiService.getUsers(), users -> this.users = users);
    }

    @OnRender
    public void render() {
        this.initializeOwnEmpire();
        this.initializeZoomDragComponent();
        this.initializeAnchorListeners();
        this.initializeTroopsListComponent();
        this.gameTicksService.init(this.isHost, this.game, this.pauseTextContainer);
        mapService.updateFleets(zoomDragComponent, fleets, warEnemies, systems, wars, empire);
    }

    private void initializeWars() {
        subscriber.subscribe(warsApiService.getWars(game._id(), empire._id()), wars -> {
            this.wars.setAll(wars);
            if (mapService.findWarEnemies(wars, empire) != null) {
                this.warEnemies.clear();
                this.warEnemies.addAll(mapService.findWarEnemies(wars, empire));
            }
            initializeFleets();
            mapService.updateFleets(zoomDragComponent, fleets, warEnemies, systems, this.wars, empire);
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".wars.*.*", War.class), event -> {
                    onBasicListEvent(event, this.wars);
                    if (mapService.findWarEnemies(wars, empire) != null) {
                        this.warEnemies.clear();
                        this.warEnemies.addAll(mapService.findWarEnemies(wars, empire));
                    }
                    mapService.updateFleets(zoomDragComponent, fleets, warEnemies, systems, wars, empire);
                }
        );
    }

    private void initializeFleets() {
        subscriber.subscribe(fleetsApiService.getFleets(game._id()), fleets -> {
            this.fleets.setAll(fleets);
            mapService.updateFleets(zoomDragComponent, this.fleets, warEnemies, systems, wars, empire);
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".fleets.*.*", Fleet.class), event -> {
            onBasicListEvent(event, this.fleets);
            mapService.updateFleets(zoomDragComponent, fleets, warEnemies, systems, wars, empire);
        });

        subscriber.subscribe(jobService.listenForJobEvent("games." + game._id() + ".empires." + empire._id() + ".jobs.*.*"), onNext -> mapService.updateFleets(zoomDragComponent, fleets, warEnemies, systems, wars, empire));
    }

    private void initializeEvents() {
        this.eventPreviewComponent = app.initAndRender(eventPreviewComponentProvider.get(), Map.of("game", game, "empire", empire, "parent", ingameAnchorPane, "overlapContainer", overlapContainer), subscriber);
        this.ingameAnchorPane.getChildren().add(eventPreviewComponent);
    }

    private void initializeAnchorListeners() {
        this.ingameAnchorPane.getChildren().addListener(this.ingameAnchorPaneChildrenListener);
    }

    private void initializeOwnEmpire() {
        subscriber.subscribe(empireService.getReadEmpires(game._id()), readEmpireDtoList -> {
            this.empiresInGame = readEmpireDtoList;

            readEmpireDtoList.stream().filter(readEmpireDto -> readEmpireDto.user().equals(tokenStorage.getUserId())).findFirst().ifPresent(readEmpireDto -> subscriber.subscribe(gameEmpiresApiService.getEmpire(game._id(), readEmpireDto._id()), playerEmpire -> {
                setPlayers(empiresInGame);
                this.empire = playerEmpire;
                this.setAvatar();
                this.initializeSideButton();
                this.populateCastles();
                this.initializePlayerListComponent(empiresInGame);
                this.listenOnEmpireChange();
                this.initializeCastleListComponent();
                this.initializeEvents();
                this.initializeWars();
                this.eventService.initializeEventService(game, empire);
                this.createResourceBar();
                this.initializeTroopsListComponent();
                this.clientChangeService.initializeClientChangeService(game);
                this.emojiService.initializeEmojiService(empire, game);
            }));
        });
    }

    private void setPlayers(List<ReadEmpireDto> empiresInGame) {
        players = FXCollections.observableArrayList();
        subscriber.subscribe(gameMembersApiService.getMembersOfGame(game._id()), members -> {
            List<String> ids = members.stream().map(Member::user).toList();
            subscriber.subscribe(usersApiService.getUsersByIDs(ids), users ->
                    fillPlayerList(members, users, empiresInGame, players)
            );
        });
    }

    private void listenOnEmpireChange() {
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), event -> {
                    if (this.empire.equals(event.data())) {
                        return;
                    }

                    this.empire = event.data();

                    this.zoomDragComponent.getCastleContainer().getChildren().forEach(node -> {
                        if (node instanceof CastleComponent) {
                            ((CastleComponent) node).setEmpire(this.empire);
                        }
                    });
                }
        );
    }

    private void initializeSystems() {
        this.systemsWebsocketDisposable = eventListener.listen("games." + game._id() + ".systems.*.*", GameSystem.class).observeOn(FX_SCHEDULER)
                .subscribe(
                        event -> {
                            switch (event.suffix()) {
                                case "created" -> systems.add(event.data());
                                case "updated" ->
                                        systems.replaceAll(u -> u._id().equals(event.data()._id()) ? event.data().setName(u.name()) : u);
                                case "deleted" -> systems.removeIf(u -> u._id().equals(event.data()._id()));
                            }
                        }
                );

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems.*.updated", GameSystem.class),
                event -> {
                    final GameSystem oldSystem = updatedSystems.stream().filter(u -> u._id().equals(event.data()._id())).
                            findFirst().orElse(null);
                    if (oldSystem == null) {
                        return;
                    }
                    final GameSystem newSystem = event.data();

                    //update systems
                    updatedSystems.replaceAll(u -> u._id().equals(newSystem._id()) ? newSystem.setName(u.name()) : u);

                    List<Object> init = battleResultService.handleSystemBattles(oldSystem, newSystem, empire, empiresInGame, users);
                    if (init != null) {
                        initializeBattleResult((Boolean) init.get(0), (ReadEmpireDto) init.get(1), (User) init.get(2));
                    }
                }
        );

        final List<String> castleNames = this.ingameService.readCastleNames();
        subscriber.subscribe(ingameService.getSystems(game._id()), (systems) -> {
            //assign castle names to systems
            if (castleNames != null) {
                for (int i = 0; i < systems.size(); i++) {
                    systems.get(i).setName(castleNames.get(i % castleNames.size()));
                }
            }

            this.systems.setAll(systems);
            updatedSystems.setAll(systems);

            this.populateCastles();
            this.initializeCastleListComponent();

            //create graph
            for (GameSystem system : systems) {
                graph.addVertex(system._id());
            }

            for (GameSystem system : systems) {
                for (Map.Entry<String, Double> entry : system.links().entrySet()) {
                    graph.addEdge(system._id(), entry.getKey());
                    graph.setEdgeWeight(graph.getEdge(system._id(), entry.getKey()), entry.getValue());
                }
            }
        });
    }

    private void initializeBattleResult(boolean battleLost, ReadEmpireDto enemyEmpire, User enemyUser) {
        battleResult.setVisible(true);
        this.battleResult.getChildren().add(app.initAndRender(battleResultComponentProvider.get(), Map.of("game", game, "empire", empire, "updatedSystems", updatedSystems, "battleLost", battleLost, "IngameController", this, "isHost", isHost, "enemyEmpire", enemyEmpire, "enemyUser", enemyUser), subscriber));
    }

    private void initializeSideButton() {
        sideButtonsComponent = app.initAndRender(sideButtonsComponentProvider.get(), Map.of("game", game, "empire", empire, "systems", systems, "parent", ingameAnchorPane, "troopsListContainer", troopsListContainer, "players", players), subscriber);
        sideButtons.getChildren().add(sideButtonsComponent);
    }

    private void initializeZoomDragComponent() {
        //pass on game to do map scaling depending on game size
        this.vBoxRoot.getChildren().add(app.initAndRender(zoomDragComponent, Map.of("game", game), subscriber));
        VBox.setVgrow(zoomDragComponent, Priority.ALWAYS);
        this.zoomDragComponent.getCastleContainer().getChildren().addListener(this.castleContainerListener);
    }

    private void initializePlayerListComponent(List<ReadEmpireDto> empiresInGame) {
        if (this.empire == null) {
            return;
        }

        subscriber.subscribe(gameMembersApiService.getMembersOfGame(game._id()), members -> {
            PlayerListComponent playerListComponent = app.initAndRender(playerListComponentProvider.get(),
                    Map.of("members", members, "empire", empire, "game", game, "empires", empiresInGame, "overlapContainer", overlapContainer, "empiresInGame", empiresInGame, "sideButtons", sideButtons, "troopsListContainer", troopsListContainer), subscriber);
            playerListContainer.getChildren().add(playerListComponent);
            playerListComponent.setParentContainer(contactsContainer);
        });
    }

    private void populateCastles() {
        if (this.hasCastlesBeenPopulated || this.systems.isEmpty() || this.empire == null) {
            return;
        }

        //populate the zoomDragComponent.castleContainer with a castle component for every system in systems
        subscriber.subscribe(fxFor.of(zoomDragComponent.getCastleContainer(), this.systems, castleComponentProvider, (component, item) -> {
            component.setParent(this);
            component.setGame(this.game);
            component.setEmpire(this.empire);
            component.setIngameRoot(this.ingameAnchorPane);
            component.setEmpires(this.empiresInGame);
            component.setSideBar(this.rightSideBarVbox);
            component.setEmpires(this.empiresInGame);
            component.setSideButtons(this.sideButtons);
            component.setTroopsListContainer(this.troopsListContainer);
            component.setSideButtonsComponent(this.sideButtonsComponent);
        }).disposable());

        this.hasCastlesBeenPopulated = true;
    }

    private void setAvatar() {
        this.avatarContainer.setStyle("-fx-effect: dropshadow(three-pass-box, " + empire.color() + ", 15, 0, 0, 0);");
        this.avatar.setImage(imageCache.get("image/portraits/" + empire.portrait() + ".png"));
    }

    private void createResourceBar() {
        ResourceBarComponent resourceBar = app.initAndRender(
                resourceBarComponent.get(),
                Map.of("game", game, "empire", empire, "windowCloseHandler", eventPreviewComponent.windowCloseEventHandler),
                subscriber
        );

        this.resourceBarContainer.getChildren().add(resourceBar);
    }

    private void initializeCastleListComponent() {
        if (this.systems.isEmpty() || this.empire == null) {
            return;
        }

        final CastleListComponent castleListComponent = app.initAndRender(castleListComponentProvider.get(), Map.of(
                "game", game, "empire", empire, "systems", this.systems, "zoomDragComponent",
                this.zoomDragComponent, "ingameRoot", ingameAnchorPane, "sideBar", rightSideBarVbox,
                "sideButtons", sideButtons, "troopsListContainer", troopsListContainer, "sideButtonsComponent", sideButtonsComponent), subscriber);
        this.castleListContainer.getChildren().add(castleListComponent);
    }

    private void initializeTroopsListComponent() {
        if (this.empire == null) {
            return;
        }

        final TroopsListComponent troopsListComponent = app.initAndRender(
                troopsListComponentProvider.get(),
                Map.of("empire", empire, "ingameRoot", ingameAnchorPane, "systems", systems,
                        "sideBar", rightSideBarVbox, "sideButtons", sideButtons),
                subscriber);
        this.troopsListContainer.getChildren().add(troopsListComponent);
    }

    //with effective bounds is meant the sub bounds of the actual window in which the player sees the map
    //so without player list, resource bar, avatar etc
    private Rectangle2D getEffectiveBounds() {
        final double minX = 0;
        final double minY = resourceBarContainer.getHeight();

        final double width = ingameRoot.getWidth() - (ingameRoot.getWidth() - rightSideBarVbox.getLayoutX()) - minX - 15;
        final double height = ingameRoot.getHeight() - minY;

        return new Rectangle2D(minX, minY, width, height);
    }

    @OnDestroy
    public void onDestroy() {
        super.destroy();

        if (isHost) {
            this.gameTicksService.stopGameTicks();
        }

        this.jobService.saveJobStartPeriods(game, empire);
        this.eventService.stopEventService();
        this.emojiService.stopEmojiService();
        this.zoomDragComponent.getCastleContainer().getChildren().removeListener(this.castleContainerListener);
        this.ingameAnchorPane.getChildren().removeListener(this.ingameAnchorPaneChildrenListener);
        this.clientChangeService.stopClientChangeService();
    }

    @OnKey()
    public void keyPressed(KeyEvent event) {
        boolean isEscape = event.getCode() == ESCAPE;

        boolean doNotOpenSettings = closeIfOpen(isEscape, CastleViewComponent.class, ingameAnchorPane)
                || closeIfOpen(isEscape, ContactsViewComponent.class, contactsContainer)
                || closeIfOpen(isEscape, TroopViewComponent.class, ingameAnchorPane);
        if (doNotOpenSettings) {
            return;
        }

        if (isEscape) {
            new Modals(app)
                    .modal(pauseMenuComponentProvider.get())
                    .params(Map.of("windowCloseHandler", eventPreviewComponent.windowCloseEventHandler))
                    .dialog(true)
                    .show();
        } else if (event.isControlDown()) {
            switch (event.getCode()) {
                case Q -> sideButtonsComponent.openTasks();
                case W -> sideButtonsComponent.openMarket();
                case E -> sideButtonsComponent.openEnhancements();
                case R -> sideButtonsComponent.openDiplomacy();
                case T -> sideButtonsComponent.openBuildFleet();
            }
        }
    }

    private <T extends CloseableView> boolean closeIfOpen(boolean close, Class<T> viewClass, Pane container) {
        T component = container.getChildren().stream()
                .filter(viewClass::isInstance)
                .map(viewClass::cast)
                .findFirst()
                .orElse(null);

        if (component == null) {
            return false;
        }
        if (close) {
            component.closeView();
        }
        return true;
    }

    public void deselect() {
        if (this.selectedCastle != null) {
            CastleComponent temp = this.selectedCastle;
            selectedCastle = null;
            temp.deselect();
        }
    }

    public void select(CastleComponent castleComponent) {
        if (this.selectedCastle != null) {
            this.selectedCastle.deselect();
        }
        this.selectedCastle = castleComponent;
    }

    public boolean handleCastleClicked(CastleComponent castleComponent) {
        if (mapService.handleCastleClicked(castleComponent, selectedCastle, graph, game, empire)) {
            this.selectedCastle = null;
            return true;
        }
        return false;
    }
}
