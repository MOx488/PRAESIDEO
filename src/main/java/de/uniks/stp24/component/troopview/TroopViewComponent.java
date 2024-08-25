package de.uniks.stp24.component.troopview;

import de.uniks.stp24.App;
import de.uniks.stp24.component.CloseableView;
import de.uniks.stp24.dto.ReadShipDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Fleet;
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
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "TroopView.fxml")
public class TroopViewComponent extends AnchorPane implements CloseableView {
    @FXML
    TabPane troopTabPane;
    @FXML
    Tab viewTab;
    @FXML
    Tab updateTroopTab;
    @FXML
    Tab trainUnitsTab;
    @FXML
    Tab travelTab;
    @FXML
    Tab transferUnitsTab;
    @FXML
    Button destroyTroopButton;
    @FXML
    Label locationLabel;
    @FXML
    ImageView ownerImage;
    @FXML
    AnchorPane troopViewRoot;
    @FXML
    VBox troopViewVBox;
    @FXML
    Button closeTroopViewButton;
    @FXML
    ImageView closeTroopViewImage;
    @FXML
    Label troopNameLabel;
    @FXML
    HBox troopNameContainer;

    @Inject
    public ImageCache imageCache;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public PresetsService presetsService;
    @Inject
    public FleetsApiService fleetsApiService;
    @Inject
    public ShipsApiService shipsApiService;
    @Inject
    public NotificationService notificationService;
    @Inject
    public App app;
    @Inject
    public Provider<ViewTabComponent> viewTabComponentProvider;
    @Inject
    public Provider<UpdateTroopTabComponent> updateTroopTabComponentProvider;
    @Inject
    public Provider<TrainUnitsTabComponent> trainUnitsTabComponentProvider;
    @Inject
    public Provider<TravelTabComponent> travelTabComponentProvider;
    @Inject
    public Provider<TransferUnitsTabComponent> transferUnitsTabComponentProvider;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("troop")
    Fleet troop;
    @Param("systems")
    ObservableList<GameSystem> systems;
    @Param("parent")
    AnchorPane parent;
    @Param("sideBar")
    VBox sideBar;
    @Param("sideButtons")
    VBox sideButtons;
    @Param("troopsList")
    VBox troopsList;
    @Param("empire")
    Empire empire;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private final List<GameSystem> systemsList = new ArrayList<>();
    private GameSystem location;

    private ChangeListener<Number> troopViewRootListener;
    private ChangeListener<Number> troopNameContainerListener;

    @Inject
    public TroopViewComponent() {
    }

    @OnInit
    void onInit() {
        systemsList.addAll(systems);
        // React to location changes
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".systems.*.updated", GameSystem.class),
                event -> {
                    final GameSystem changedCastle = event.data();

                    // Only react to changes of the location
                    if (!changedCastle._id().equals(location._id())) {
                        return;
                    }

                    final GameSystem oldCastle = systemsList.stream().filter(s -> s._id().equals(changedCastle._id())).findFirst().orElse(null);

                    // Update castle if owner changed
                    if (oldCastle != null && !oldCastle.owner().equals(changedCastle.owner())) {
                        updateLocation();
                        systemsList.replaceAll(s -> s._id().equals(changedCastle._id()) ? changedCastle : s);
                    }
                }
        );

        // Update troop
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".fleets." + troop._id() + ".*", Fleet.class),
                event -> {
                    switch (event.suffix()) {
                        case "updated" -> {
                            final Fleet updatedTroop = event.data();
                            if (!updatedTroop.location().equals(troop.location())) {
                                updateLocation();
                            }
                            if (!updatedTroop.name().equals(troop.name())) {
                                troopNameLabel.setText(updatedTroop.name());
                            }
                            troop = event.data();
                        }
                        case "deleted" -> closeView();
                    }
                }
        );
    }

    private void updateLocation() {
        if (troop == null) {
            return;
        }

        // Update location
        location = systemsList.stream().filter(s -> s._id().equals(troop.location())).findFirst().orElse(null);
        locationLabel.setText(location != null ? location.name() : null);

        // Update owner image (smile)
        if (location != null && troop.empire().equals(location.owner())) {
            ownerImage.setImage(imageCache.get("image/icons/face-smile.png"));
        } else {
            ownerImage.setImage(imageCache.get("image/icons/face-frown.png"));
        }
    }

    @OnRender
    void onRender() {
        this.sideBar.setVisible(false);
        this.sideButtons.setVisible(false);
        this.troopsList.setVisible(false);
        closeTroopViewImage.setImage(imageCache.get("image/cross_red.png"));
        troopNameLabel.setText(troop != null ? troop.name() : "Unknown name");
        closeTroopViewButton.setOnAction(event -> closeView());

        // "Destroy troop" button should only show in the "View" tab
        destroyTroopButton.visibleProperty().bind(viewTab.selectedProperty());
        final BooleanBinding waiting = notWaiting.not();
        destroyTroopButton.disableProperty().bind(waiting);

        // Set location
        location = systemsList.stream().filter(s -> s._id().equals(troop.location())).findFirst().orElse(null);
        locationLabel.setText(bundle.getString("location") + ": " + (location != null ? location.name() : null));
        if (location != null && troop.empire().equals(location.owner())) {
            ownerImage.setImage(imageCache.get("image/icons/face-smile.png"));
        } else {
            ownerImage.setImage(imageCache.get("image/icons/face-frown.png"));
        }

        // Responsive design
        troopViewRoot.prefWidthProperty().bind(parent.widthProperty());
        troopViewRoot.prefHeightProperty().bind(parent.heightProperty());
        troopViewRoot.widthProperty().addListener(troopViewRootListener = (observable, oldValue, newValue) ->
                troopNameContainer.setLayoutX(newValue.doubleValue() / 2 - troopNameContainer.getWidth() / 2)
        );
        troopNameContainer.widthProperty().addListener(troopNameContainerListener = (observable, oldValue, newValue) ->
                troopNameContainer.setLayoutX(troopViewRoot.getWidth() / 2 - newValue.doubleValue() / 2)
        );


        // Get units of this troop and init tabs
        final ObservableList<ReadShipDto> unitsList = FXCollections.observableArrayList();
        subscriber.subscribe(shipsApiService.getShips(troop.game(), troop._id()), units -> {
            unitsList.setAll(units);
            initTabs(unitsList);
        });
    }

    private void initTabs(ObservableList<ReadShipDto> units) {
        final List<ShipType> unitTypeInfo = new ArrayList<>();
        subscriber.subscribe(presetsService.getCachedPreset("getShips"), ships -> {
            unitTypeInfo.addAll((List<ShipType>) ships);

            ViewTabComponent viewTabComponent = app.initAndRender(
                    viewTabComponentProvider.get(),
                    Map.of("tabPane", troopTabPane, "units", units, "troop", troop, "unitTypeInfo", unitTypeInfo),
                    subscriber
            );
            viewTab.setContent(viewTabComponent);
            UpdateTroopTabComponent updateTroopTabComponent = app.initAndRender(
                    updateTroopTabComponentProvider.get(),
                    Map.of("tabPane", troopTabPane, "units", units, "troop", troop, "unitTypeInfo", unitTypeInfo),
                    subscriber
            );
            updateTroopTab.setContent(updateTroopTabComponent);
            TrainUnitsTabComponent trainUnitsTabComponent = app.initAndRender(
                    trainUnitsTabComponentProvider.get(),
                    Map.of(
                            "tabPane", troopTabPane, "troop", troop, "location", location,
                            "unitTypeInfo", unitTypeInfo, "empire", empire
                    ),
                    subscriber
            );
            trainUnitsTab.setContent(trainUnitsTabComponent);
            TravelTabComponent travelTabComponent = app.initAndRender(
                    travelTabComponentProvider.get(),
                    Map.of("tabPane", troopTabPane, "troop", troop, "systems", systems),
                    subscriber
            );
            travelTab.setContent(travelTabComponent);
            TransferUnitsTabComponent transferUnitsTabComponent = app.initAndRender(
                    transferUnitsTabComponentProvider.get(),
                    Map.of("tabPane", troopTabPane, "troop", troop, "units", units, "unitTypeInfo", unitTypeInfo),
                    subscriber
            );
            transferUnitsTab.setContent(transferUnitsTabComponent);
        });
    }

    public void destroyTroop() {
        notWaiting.set(false);

        subscriber.subscribe(fleetsApiService.deleteFleet(troop.game(), troop._id()),
                success -> {
                    closeView();
                    notificationService.displayNotification(bundle.getString("success.troop.destroyed"), true);
                },
                error -> notWaiting.set(true)
        );
    }

    @Override
    public void closeView() {
        parent.getChildren().remove(this);
        onDestroy();
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
        sideBar.setVisible(true);
        sideButtons.setVisible(true);
        troopsList.setVisible(true);
        if (troopViewRootListener != null) {
            troopViewRoot.widthProperty().removeListener(troopViewRootListener);
        }
        if (troopNameContainerListener != null) {
            troopNameContainer.widthProperty().removeListener(troopNameContainerListener);
        }
    }
}
