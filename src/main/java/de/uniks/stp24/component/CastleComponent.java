package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.controller.IngameController;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.service.EmpireService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Subscription;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

import static de.uniks.stp24.util.Constants.*;
import static de.uniks.stp24.util.Methods.showNode;

@Component(view = "Castle.fxml")
public class CastleComponent extends VBox {
    @FXML
    HBox fleetBox;
    @FXML
    ImageView battleImage;
    @FXML
    Label castleNameLabel;
    @FXML
    ImageView castleImage;

    @Inject
    public App app;

    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public ImageCache imageCache;


    @Inject
    public EventListener eventListener;
    @Inject
    public Subscriber subscriber;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public EmpireService empireService;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    public Provider<FleetMapComponent> fleetMapComponentProvider;

    @Inject
    public Provider<CastleViewComponent> castleViewComponent;

    @Param("item")
    GameSystem system;
    @Param("list")
    ObservableList<GameSystem> systems;

    private SideButtonsComponent sideButtonsComponent;

    private boolean isSelectedInCastleList;
    private Game game;
    private Empire localEmpire;
    private AnchorPane ingameRoot;
    private VBox sideBar;
    private VBox sideButtons;
    private VBox troopsListContainer;
    private IngameController parent;
    private List<ReadEmpireDto> empiresInGame;
    private final SimpleObjectProperty<Object> ownerEmpireProperty;
    private final SimpleObjectProperty<CastleType> castleTypeProperty;
    public FleetMapComponent selectedFleetComponent = null;

    private ArrayList<Fleet> castleFleets;
    private ObservableList<String> warEnemies;
    private Map<String, ArrayList<String>> jobs;

    private Subscription subscription;

    public static final SimpleBooleanProperty displayNameTagProperty = new SimpleBooleanProperty(false);

    private final ChangeListener<CastleType> castleTypeChangeListener = (observable, oldValue, newValue) -> {
        if (newValue == null) {
            return;
        }

        Image image = imageCache.get("image/castles/" + getCastleImageNameBasedOnType(newValue), true);
        if (image == null) {
            return;
        }

        this.castleImage.setImage(image);
    };

    //because get enemy empire is async -> listen on change and then set color
    private final ChangeListener<? super Object> ownerEmpireChangeListener = (observable, oldValue, newValue) -> {
        final String ownerColor = this.fetchColorBasedOnOwnerEmpire();
        this.setLabelColor(ownerColor);
        this.setShadowEffect(ownerColor);
    };

    @Inject
    public CastleComponent() {
        this.castleTypeProperty = new SimpleObjectProperty<>();
        this.ownerEmpireProperty = new SimpleObjectProperty<>();
        this.isSelectedInCastleList = false;
    }

    @OnInit
    public void init() {
        this.castleTypeProperty.addListener(this.castleTypeChangeListener);
        this.ownerEmpireProperty.addListener(this.ownerEmpireChangeListener);

        this.setOnMouseEntered(event -> {
            //set hover effect
            this.setShadowEffect(INGAME_SYSTEM_HOVER);
        });

        this.setOnMouseExited(event -> {
            //reset shadow color to empire owner color but only if it's not the selected one
            if (this.isSelectedInCastleList) {
                return;
            }

            this.setShadowEffect(this.fetchColorBasedOnOwnerEmpire());
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems." + system._id() + ".updated", GameSystem.class), event -> {
            final GameSystem newSystem = event.data();

            if (newSystem.equals(this.system)) {
                return;
            }

            this.system = newSystem.setName(this.system.name());
            this.handleNewOwner();
        });
    }

    public void updateBattleIcon(boolean battle) {
        if (battle) {
            this.battleImage.setImage(imageCache.get("image/icons/fight.png"));
            this.battleImage.setStyle("-fx-border-width: 2; -fx-border-color: black;");
            showNode(battleImage, true);
        } else {
            this.battleImage.setImage(null);
            showNode(battleImage, false);
        }

    }

    private void handleNewOwner() {
        //we only care about owner change
        if (this.system.owner() == null) {
            return;
        }

        this.setOwnerEmpireOfSystem();
        this.setShadowEffect(this.fetchColorBasedOnOwnerEmpire());
    }

    public String fetchColorBasedOnOwnerEmpire() {
        if (this.ownerEmpireProperty.get() == null) {
            return null;
        }

        String color;
        Object ownerEmpire = this.ownerEmpireProperty.get();
        if (ownerEmpire instanceof Empire) {
            color = ((Empire) ownerEmpire).color();
        } else {
            color = ((ReadEmpireDto) ownerEmpire).color();
        }

        return color;
    }

    private void setOwnerEmpireOfSystem() {
        final String ownerId = this.system.owner();
        if (ownerId == null || ownerId.isEmpty()) {
            return;
        }

        if (this.localEmpire != null && ownerId.equals(this.localEmpire._id())) {
            this.ownerEmpireProperty.set(this.localEmpire);
            return;
        }

        //needs to be enemy empire
        this.empiresInGame.stream().filter(readEmpireDto -> readEmpireDto._id().equals(ownerId)).findFirst().ifPresent(this.ownerEmpireProperty::set);
    }

    public void setShadowEffect(String color) {
        if (color == null) {
            //no owner -> no shadow
            this.setStyle("");
            return;
        }

        this.setStyle("-fx-effect: dropshadow(three-pass-box, " + color + ", 15, 0, 0, 0);");
    }

    public GameSystem getSystem() {
        return this.system;
    }

    @OnRender
    public void onRender() {
        //game settings size dictate how many cities need to be displayed on our map
        //min 50, max 200, default 100
        float castleImageSize = -0.23f * game.settings().size() + 60.f;
        this.castleImage.setFitWidth(castleImageSize);
        this.castleImage.setFitHeight(castleImageSize);

        this.castleNameLabel.visibleProperty().bind(CastleComponent.displayNameTagProperty);
        this.castleNameLabel.managedProperty().bind(CastleComponent.displayNameTagProperty);
        this.castleNameLabel.setText(this.system.name());
        this.castleNameLabel.setFont(new Font(-0.06667d * game.settings().size() + 16.33d));

        this.setOwnerEmpireOfSystem();
        this.setShadowEffect(this.fetchColorBasedOnOwnerEmpire());
        this.setLabelColor(this.fetchColorBasedOnOwnerEmpire());

        this.subscription = displayNameTagProperty.subscribe(() -> updateFleets(this.castleFleets, this.warEnemies, this.jobs));
    }

    private void setLabelColor(String color) {
        if (color == null) {
            color = "black";
        }

        this.castleNameLabel.setStyle("-fx-border-color: " + color);
    }

    public void onCastleClick() {
        if (parent.handleCastleClicked(this)) {
            return;
        }

        final CastleViewComponent castleView = app.initAndRender(
                castleViewComponent.get(),
                Map.of("game", game, "system", system, "empire", localEmpire, "sideBar", sideBar,
                        "sideButtons", sideButtons, "troopsListContainer", troopsListContainer, "sideButtonsComponent", sideButtonsComponent),
                subscriber
        );

        this.ingameRoot.getChildren().add(castleView);
    }

    public String getCastleImageNameBasedOnType(CastleType type) {
        final String prefix = switch (type) {
            case WATER -> "water";
            case SAND -> "sand";
            case FOREST -> "forest";
            case GRASS -> {
                final Random rand = new Random();
                final int randomNum = rand.nextInt(2) + 1;
                yield "grass" + randomNum;
            }
            case MOUNTAIN -> "mountain";
        };

        return prefix + "_castle.png";
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setCastleType(CastleType type) {
        this.castleTypeProperty.set(type);
    }

    public void setEmpire(Empire empire) {
        this.localEmpire = empire;
    }

    public void setIngameRoot(AnchorPane ingameRoot) {
        this.ingameRoot = ingameRoot;
    }

    public void setSideBar(VBox sidebar) {
        this.sideBar = sidebar;
    }

    public void setSideButtons(VBox sideButtons) {
        this.sideButtons = sideButtons;
    }

    public void setTroopsListContainer(VBox troopsListContainer) {
        this.troopsListContainer = troopsListContainer;
    }

    public void setEmpires(List<ReadEmpireDto> empiresInGame) {
        this.empiresInGame = empiresInGame;
    }

    public void setSideButtonsComponent(SideButtonsComponent sideButtonsComponent) {
        this.sideButtonsComponent = sideButtonsComponent;
    }

    public void setSelectedInCastleList(boolean selectedInCastleList) {
        this.isSelectedInCastleList = selectedInCastleList;
    }

    public void setParent(IngameController ingameController) {
        this.parent = ingameController;
    }

    @OnDestroy
    public void destroy() {
        subscription.unsubscribe();
        this.subscriber.dispose();
        this.castleTypeProperty.removeListener(this.castleTypeChangeListener);
        this.ownerEmpireProperty.removeListener(this.ownerEmpireChangeListener);
    }

    public void updateFleets(ArrayList<Fleet> castleFleets, ObservableList<String> warEnemies, Map<String, ArrayList<String>> jobs) {
        this.warEnemies = warEnemies;
        this.castleFleets = castleFleets;
        this.jobs = jobs;

        if (displayNameTagProperty.get()) {
            fleetBox.getChildren().clear();

            for (Fleet fleet : castleFleets) {
                Result result = getResult(warEnemies, jobs, fleet);
                FleetMapComponent fleetMapComponent = app.initAndRender(
                        fleetMapComponentProvider.get(),
                        getFleetMapParams(fleet, result),
                        subscriber);
                fleetBox.getChildren().add(fleetMapComponent);
            }
        } else {
            fleetBox.getChildren().clear();

            if (!castleFleets.isEmpty()) {
                FleetMapComponent fleetMapComponent = app.initAndRender(
                        fleetMapComponentProvider.get(),
                        Map.of("parent", this, "alignment", "#transparent", "empire", localEmpire),
                        subscriber
                );
                fleetBox.getChildren().add(fleetMapComponent);
            }
        }
    }

    private Map<String, Object> getFleetMapParams(Fleet fleet, Result result) {
        Map<String, Object> params = new HashMap<>();
        params.put("parent", this);
        params.put("fleet", fleet);
        params.put("alignment", result.alignment());
        params.put("empires", empiresInGame);
        params.put("ingameRoot", ingameRoot);
        params.put("systems", systems);
        params.put("sideBar", sideBar);
        params.put("sideButtons", sideButtons);
        params.put("troopsList", troopsListContainer);
        params.put("jobInfo", result.jobInfo());
        params.put("empire", localEmpire);

        return params;
    }

    private @NotNull Result getResult(ObservableList<String> warEnemies, Map<String, ArrayList<String>> jobs, Fleet fleet) {
        ArrayList<String> jobInfo = new ArrayList<>();
        if (fleet.empire() == null) {
            return new Result(jobInfo, "#transparent");
        }
        if (fleet.empire().equals(localEmpire._id())) {
            for (Map.Entry<String, ArrayList<String>> entry : jobs.entrySet()) {
                if (fleet._id().equals(entry.getKey())) {
                    jobInfo = entry.getValue();
                    return new Result(jobInfo, COLOR_ORANGE);
                }
            }
            return new Result(jobInfo, COLOR_GREEN);
        }
        if (warEnemies.contains(fleet.empire())) {
            return new Result(jobInfo, COLOR_RED);
        }
        return new Result(jobInfo, "#transparent");
    }

    private record Result(ArrayList<String> jobInfo, String alignment) {
    }

    public void deselect() {
        if (selectedFleetComponent != null) {
            parent.deselect();
            FleetMapComponent temp = selectedFleetComponent;
            selectedFleetComponent = null;
            if (temp != null) {
                temp.deselect();
            }
        }
    }

    public void select(FleetMapComponent fleetMapComponent) {
        parent.deselect();
        if (selectedFleetComponent != null) {
            selectedFleetComponent.deselect();
        }
        parent.select(this);
        selectedFleetComponent = fleetMapComponent;
    }

    public Fleet getSelectedFleet() {
        return selectedFleetComponent.fleet;
    }

    public void setTravelling() {
        selectedFleetComponent.setTravelling();
        this.selectedFleetComponent = null;
    }
}
