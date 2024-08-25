package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.CreateFleetDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.model.ShipType;
import de.uniks.stp24.rest.FleetsApiService;
import de.uniks.stp24.rest.ShipsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.service.PresetsService;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import static de.uniks.stp24.util.Methods.initListView;
import static de.uniks.stp24.util.Methods.updateOurCastles;


@Component(view = "BuildFleet.fxml")
public class BuildFleetComponent extends HBox {
    public VBox unitView;
    public ImageView unitImage;
    public Text unitName;
    public ScrollPane unitScrollPane;
    @FXML
    ImageView cancelImage;
    @FXML
    HBox buildFleetRoot;
    @FXML
    Text buildFleetTitle;
    @FXML
    ChoiceBox<String> systemsChoiceBox;
    @FXML
    ListView<ShipType> shipsList;
    @FXML
    TextField fleetName;
    @FXML
    Button buildFleetButton;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public ShipsApiService shipsApiService;
    @Inject
    public Provider<ShipComponent> shipComponentProvider;
    @Inject
    public FleetsApiService fleetsApiService;
    @Inject
    public PresetsService presetsService;
    @Inject
    public ImageCache imageCache;
    @Inject
    public EventListener eventListener;
    @Inject
    public NotificationService notificationService;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("game")
    Game game;
    @Param("empire")
    Empire empire;
    @Param("systems")
    ObservableList<GameSystem> systems;

    private GameSystem previousWebsocketSystem;
    private ObservableList<GameSystem> ourCastles = FXCollections.observableArrayList();
    private GameSystem selectedSystem;
    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private final SimpleStringProperty usernameProperty = new SimpleStringProperty();
    private final ObservableList<ShipType> shipTypes = FXCollections.observableArrayList();
    private final TreeMap<String, Integer> shipAmounts = new TreeMap<>();
    public final TreeMap<String, Boolean> shipTypeClicked = new TreeMap<>();

    private ChangeListener<String> systemsChoiceBoxListener;

    @Inject
    public BuildFleetComponent() {
    }

    @OnInit
    public void onInit() {
        ourCastles = FXCollections.observableArrayList(systems);
        ourCastles.removeIf(system -> system.owner() == null || !system.owner().equals(empire._id()));

        subscriber.subscribe(presetsService.getCachedPreset("getShips"), ships ->
                shipTypes.setAll((List<ShipType>) ships)
        );

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems.*.updated", GameSystem.class),
                event -> {
                    final GameSystem newSystem = event.data();
                    updateOurCastles(previousWebsocketSystem, newSystem, empire, ourCastles, systems, this::populateChoiceBox);
                    this.previousWebsocketSystem = newSystem;
                }
        );
    }

    @OnRender
    public void onRender() {
        fleetName.textProperty().bindBidirectional(usernameProperty);
        buildFleetTitle.setText(bundle.getString("build.new.troop")); // Set the title of the buildFleetTitle.
        fleetName.setPromptText(bundle.getString("troop.name")); // Set the prompt text for fleetName.
        cancelImage.setImage(imageCache.get("image/circle-xmark-regular.png")); // Set the image of the cancelImage.
        populateChoiceBox();

        initListView(shipsList, shipTypes, app, shipComponentProvider,
                Map.of("empire", empire, "shipAmounts", shipAmounts, "BuildFleetComponent", this,
                        "shipTypeClicked", shipTypeClicked)
        );

        bindBuildFleet();
    }

    private void populateChoiceBox() {
        systemsChoiceBox.getItems().clear();// Clear existing items
        systemsChoiceBox.getItems().add(bundle.getString("choose.one.of.your.castles")); // Add a default item
        for (GameSystem system : ourCastles) {
            systemsChoiceBox.getItems().add(system.name()); // Add system names to the ChoiceBox
        }
        systemsChoiceBox.getSelectionModel().selectFirst(); // Select the first system by default
        systemsChoiceBox.getSelectionModel().selectedItemProperty().addListener(systemsChoiceBoxListener = (obs, oldSelection, newSelection) -> {
            if (newSelection == null) return;
            if (newSelection.equals(bundle.getString("choose.one.of.your.castles"))) return;
            selectedSystem = ourCastles.stream().filter(system -> system.name().equals(newSelection)).findFirst().orElse(null);
        });
    }

    public void bindBuildFleet() {
        final BooleanBinding usernameNotEmpty = fleetName.textProperty().isNotEmpty();
        final BooleanBinding usernameLessThirtyTwo = fleetName.textProperty().length().lessThanOrEqualTo(25);
        final BooleanBinding waiting = this.notWaiting.not();
        final BooleanBinding selectedCastle = systemsChoiceBox.getSelectionModel().selectedItemProperty().isNotEqualTo(bundle.getString("choose.one.of.your.castles"));


        buildFleetButton.disableProperty().bind(
                (usernameLessThirtyTwo.and(usernameNotEmpty).and(selectedCastle)).not().or(waiting)
        );
    }

    public void buildFleet() {
        if (selectedSystem == null) return;
        boolean shipAmountsEmpty = shipAmounts.values().stream().allMatch(amount -> amount == 0);
        if (shipAmountsEmpty) return;
        notWaiting.set(false);
        // Create a TreeMap with the name of the selected shipType and the amount of That.
        CreateFleetDto createFleetDto = new CreateFleetDto(fleetName.getText(), selectedSystem._id(), shipAmounts, null, null, null);
        subscriber.subscribe(fleetsApiService.createFleet(game._id(), createFleetDto),
                success -> {
                    notificationService.displayNotification(bundle.getString("success.troop.created"), true);
                    notWaiting.set(true);
                    fleetName.clear(); // Clear the fleetName.
                    shipAmounts.replaceAll((k, v) -> 0);// Reset the shipAmounts.
                    onRender(); // Re-render the component.
                },
                error -> notWaiting.set(true)
        );
        selectedSystem = null; // Reset the selectedSystem.
    }

    public void closeUnitView() {
        if (unitView.isVisible()) {
            shipsList.refresh();
            unitView.setVisible(false);
            shipTypeClicked.replaceAll((k, v) -> false);
        }
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
        if (systemsChoiceBoxListener != null) {
            systemsChoiceBox.getSelectionModel().selectedItemProperty().removeListener(systemsChoiceBoxListener);
        }
    }
}

