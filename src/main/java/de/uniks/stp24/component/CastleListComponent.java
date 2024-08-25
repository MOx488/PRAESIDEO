package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.rest.GameMembersApiService;
import de.uniks.stp24.rest.GameSystemsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.listview.ComponentListCell;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Constants.INGAME_SYSTEM_HOVER;
import static de.uniks.stp24.util.Methods.provideListClickFunctionality;
import static de.uniks.stp24.util.Methods.updateOurCastles;

@Component(view = "CastleList.fxml")
public class CastleListComponent extends VBox {

    @FXML
    ImageView empireFlag;
    @FXML
    ListView<GameSystem> castleListView;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public GameSystemsApiService gameSystemsApiService;
    @Inject
    public GameMembersApiService gameMembersApiService;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    public Provider<CastleNameComponent> castleNameComponentProvider;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;
    @Inject
    public EventListener eventListener;
    @Inject
    public Provider<CastleViewComponent> castleViewComponentProvider;

    private ObservableList<GameSystem> castles = FXCollections.observableArrayList();
    private GameSystem previousWebsocketSystem;
    private CastleComponent highlightedCastleComponent;

    @Param("game")
    Game game;
    @Param("empire")
    Empire empire;
    @Param("systems")
    ObservableList<GameSystem> systems;
    @Param("zoomDragComponent")
    ZoomDragComponent zoomDragComponent;
    @Param("ingameRoot")
    AnchorPane ingameRoot;
    @Param("sideBar")
    VBox sideBar;
    @Param("sideButtons")
    VBox sideButtons;
    @Param("troopsListContainer")
    VBox troopsListContainer;
    @Param("sideButtonsComponent")
    SideButtonsComponent sideButtonsComponent;

    @Inject
    public CastleListComponent() {
    }

    @OnInit
    public void init() {
        // signalize castle components of the new empire change
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), event -> {
                    if (this.empire.equals(event.data())) {
                        return;
                    }

                    this.empire = event.data();
                }
        );

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems.*.updated", GameSystem.class),
                event -> {
                    final GameSystem newSystem = event.data();
                    updateOurCastles(previousWebsocketSystem, newSystem, empire, castles, systems, null);
                    this.previousWebsocketSystem = newSystem;
                }
        );

        //we get passed on all systems
        //create new list that only holds our systems
        //we need to create a new list as otherwise we would also modify the original list
        this.castles = FXCollections.observableArrayList(systems);

        //only our systems
        this.castles.removeIf(system -> system.owner() == null || !system.owner().equals(empire._id()));
    }

    @OnRender
    public void render() {
        subscriber.subscribe(gameMembersApiService.getMember(game._id(), tokenStorage.getUserId()), member ->
                empireFlag.setImage(imageCache.get("image/flags/" + member.empire().flag() + ".png"))
        );

        castleListView.setItems(castles);
        castleListView.setCellFactory(list -> {
            final ListCell<GameSystem> cellList = new ComponentListCell<>(app, castleNameComponentProvider);
            final MultipleSelectionModel<GameSystem> selectionModel = castleListView.getSelectionModel();
            provideListClickFunctionality(selectionModel, cellList, castleListView);
            return cellList;
        });

        castleListView.setOnMouseClicked((MouseEvent event) -> {
                    if (!event.getButton().equals(MouseButton.PRIMARY)) {
                        return;
                    }

                    final GameSystem clickedCastle = castleListView.getSelectionModel().getSelectedItem();

                    //highlight selected city list
                    this.highlightCastle(clickedCastle);

                    if (clickedCastle == null || event.getClickCount() != 2) {
                        return;
                    }

                    final CastleViewComponent castleView = app.initAndRender(
                            castleViewComponentProvider.get(),
                            Map.of("game", game, "system", clickedCastle, "empire", empire,
                                    "sideBar", sideBar, "sideButtons", sideButtons,
                                    "troopsListContainer", troopsListContainer, "sideButtonsComponent", sideButtonsComponent),
                            subscriber
                    );

                    this.ingameRoot.getChildren().add(castleView);
                }
        );
    }

    private void highlightCastle(GameSystem clickedCastle) {
        //if user deselects -> reset highlighting on previously highlighted castle
        if (clickedCastle == null) {
            this.resetHighlight();
            return;
        }

        final CastleComponent castleComponent = zoomDragComponent.getCastleContainer().getChildren()
                .stream()
                .map(child -> (CastleComponent) child)
                .filter(child -> child.system._id().equals(clickedCastle._id()))
                .findFirst()
                .orElse(null);

        if (castleComponent == null) {
            return;
        }

        if (this.highlightedCastleComponent != castleComponent) {
            //previously highlighted is not currently selected anymore -> unhighlight old
            //reset shadow from previously highlighted city
            this.resetHighlight();
        }

        //highlight currently selected city
        castleComponent.setShadowEffect(INGAME_SYSTEM_HOVER);
        castleComponent.setSelectedInCastleList(true);

        //set currently selected for comparison later
        this.highlightedCastleComponent = castleComponent;
    }

    private void resetHighlight() {
        if (this.highlightedCastleComponent == null) {
            return;
        }

        this.highlightedCastleComponent.setShadowEffect(this.highlightedCastleComponent.fetchColorBasedOnOwnerEmpire());
        this.highlightedCastleComponent.setSelectedInCastleList(false);
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }
}

