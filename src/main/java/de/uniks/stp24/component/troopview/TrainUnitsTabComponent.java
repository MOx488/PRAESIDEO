package de.uniks.stp24.component.troopview;

import de.uniks.stp24.App;
import de.uniks.stp24.component.TaskComponent;
import de.uniks.stp24.dto.CreateJobDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.model.troopview.TroopSizeItem;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.rest.GameSystemsApiService;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ExplainedVariableService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
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

import static de.uniks.stp24.util.Methods.*;

@Component(view = "TrainUnitsTab.fxml")
public class TrainUnitsTabComponent extends AnchorPane {
    @FXML
    Label errorLabel;
    @FXML
    AnchorPane viewTabRoot;
    @FXML
    ListView<TroopSizeItem> chooseUnitsListView;
    @FXML
    ScrollPane focusedUnitScrollPane;
    @FXML
    Button trainUnitButton;
    @FXML
    ListView<Job> unitTasksListView;
    @FXML
    ImageView shipyardImageView;
    @FXML
    Label shipyardAmountLabel;

    @Inject
    public ImageCache imageCache;
    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public Provider<TroopSizeComponent> troopSizeComponentProvider;
    @Inject
    public Provider<TaskComponent> taskComponentProvider;
    @Inject
    public GameSystemsApiService gameSystemsApiService;
    @Inject
    public ExplainedVariableService explainedVariableService;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public GameLogicApiService gameLogicApiService;
    @Inject
    @Resource
    public ResourceBundle bundle;

    private final ObservableList<TroopSizeItem> sizes = FXCollections.observableArrayList();
    private final ObservableList<Job> unitTasks = FXCollections.observableArrayList();
    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty locationIsOwned = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty shipyardAvailable = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty enoughResources = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty unitUnlocked = new SimpleBooleanProperty(false);

    private ChangeListener<TroopSizeItem> chooseUnitsListViewListener;
    private ChangeListener<Number> viewTabRootListener;

    private Job lastJobUpdate;
    private Empire lastEmpireUpdate;

    @Param("tabPane")
    TabPane tabPane;
    @Param("troop")
    Fleet troop;
    @Param("location")
    GameSystem location;
    @Param("unitTypeInfo")
    List<ShipType> unitTypeInfo;
    @Param("empire")
    Empire empire;

    @Inject
    public TrainUnitsTabComponent() {
    }

    @OnInit
    void onInit() {
        lastEmpireUpdate = empire;

        // Update troop
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".fleets." + troop._id() + ".updated", Fleet.class),
                event -> {
                    final Fleet updatedTroop = event.data();
                    onTroopUpdate(troop, updatedTroop, sizes);

                    if (!updatedTroop.location().equals(troop.location())) {
                        troop = updatedTroop;
                        updateLocation();
                    } else {
                        troop = updatedTroop;
                    }
                }
        );

        // Might need to update location if systems have updated (maybe owner change or new shipyard)
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".systems.*.updated", GameSystem.class),
                event -> {
                    if (location != null && location._id().equals(event.data()._id())) {
                        setLocationAndShipyardCount(event.data());
                    }
                }
        );

        // Get all unit tasks for this fleet
        subscriber.subscribe(jobsApiService.getFilteredJobs(troop.game(), troop.empire(), "ship", troop._id(), troop.location()),
                unitTasks::setAll
        );

        // Update tasks
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".empires." + troop.empire() + ".jobs.*.*", Job.class),
                event -> {
                    Job job = event.data();

                    // Ignore identical events if they were not finished yet
                    if (lastJobUpdate != null && lastJobUpdate.equals(job)) {
                        return;
                    }

                    // Only look at unit jobs from this troop
                    if (!troop._id().equals(job.fleet()) || !job.type().equals("ship")) {
                        return;
                    }

                    onBasicListEvent(event, unitTasks);

                    lastJobUpdate = job;
                }
        );

        // Update explained variables
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".empires." + troop.empire() + ".updated", Empire.class),
                event -> {
                    Empire empire = event.data();
                    final TroopSizeItem focusedUnit = chooseUnitsListView.getSelectionModel().getSelectedItem();
                    final String type = focusedUnit != null ? focusedUnit.type() : null;

                    updateExplainedUnitDetails(focusedUnitScrollPane, explainedVariableService, troop, unitTypeInfo,
                            type, List.of("health", "speed", "attack", "defense", "cost", "upkeep", "build_time"),
                            lastEmpireUpdate, empire, bundle, unitUnlocked
                    );

                    // Ignore identical events and only look at effect or resource updates
                    if (lastEmpireUpdate != null && lastEmpireUpdate.effects().equals(empire.effects()) && lastEmpireUpdate.resources().equals(empire.resources())) {
                        return;
                    }
                    checkIfEnoughResources();

                    lastEmpireUpdate = empire;
                }
        );
    }

    private void checkIfEnoughResources() {
        // Only update the binding if a type is selected
        final TroopSizeItem focusedUnit = chooseUnitsListView.getSelectionModel().getSelectedItem();
        final String type = focusedUnit != null ? focusedUnit.type() : null;
        if (type == null) {
            return;
        }

        // Check if the user has enough resources to pay for the unit
        subscriber.subscribe(gameLogicApiService.getExplainedVariableWithMapValues(troop.game(), troop.empire(), "ships." + type + ".cost"),
                var -> {
                    final Map<String, Double> cost = var.end();
                    for (Map.Entry<String, Double> entry : cost.entrySet()) {
                        final String resource = entry.getKey();
                        final double value = entry.getValue();
                        if (lastEmpireUpdate.resources().getOrDefault(resource, 0) < value) {
                            enoughResources.set(false);
                            return;
                        }
                    }
                    enoughResources.set(true);
                }
        );
    }

    private void updateLocation() {
        subscriber.subscribe(gameSystemsApiService.getSystem(troop.game(), troop.location()), this::setLocationAndShipyardCount);
    }

    private void setLocationAndShipyardCount(GameSystem system) {
        location = system;
        int shipyardAmount = (int) location.buildings().stream().filter(b -> b.equals("shipyard")).count();
        shipyardAmountLabel.setText(String.valueOf(shipyardAmount));

        // Train unit button might be blocked
        locationIsOwned.set(troop.empire().equals(location.owner()));
        shipyardAvailable.set(shipyardAmount > 0);
    }

    @OnRender
    void onRender() {
        ensureResponsiveDesign();

        shipyardImageView.setImage(imageCache.get("image/game_resources/shipyard.png"));
        if (location != null) {
            shipyardAmountLabel.setText(String.valueOf(location.buildings().stream().filter(b -> b.equals("shipyard")).count()));
            setLocationAndShipyardCount(location);
        }

        // Fill "choose unit" list
        for (Map.Entry<String, Integer> entry : troop.size().entrySet()) {
            String type = entry.getKey();

            // Only show units that have a "planned" value of at least 1
            if (entry.getValue().equals(0)) {
                continue;
            }

            sizes.add(new TroopSizeItem(type, 0, 0));
        }
        initListView(chooseUnitsListView, sizes, app, troopSizeComponentProvider,
                Map.of("listView", chooseUnitsListView, "dontShowAmounts", true, "selectable", true)
        );

        // Fill "unit tasks" list
        initListView(unitTasksListView, unitTasks, app, taskComponentProvider,
                Map.of("dontShowDetails", true, "listView", unitTasksListView)
        );
        prepareUnitInfo();

        // Show fitting error message and disable button if necessary
        final BooleanBinding waiting = notWaiting.not();
        final BooleanBinding locationNotOwned = locationIsOwned.not();
        final BooleanBinding noShipyardAvailable = shipyardAvailable.not();
        final BooleanBinding noUnitSelected = chooseUnitsListView.getSelectionModel().selectedItemProperty().isNull();
        final BooleanBinding notEnoughResources = enoughResources.not();
        final BooleanBinding notUnlockedYet = unitUnlocked.not();
        errorLabel.textProperty().bind(
                Bindings.when(noUnitSelected).then(bundle.getString("error.no.unit.selected"))
                        .otherwise(Bindings.when(locationNotOwned).then(bundle.getString("error.location.not.owned"))
                                .otherwise(Bindings.when(notUnlockedYet).then(bundle.getString("error.unit.not.unlocked"))
                                        .otherwise(Bindings.when(notEnoughResources).then(bundle.getString("error.not.enough.resources"))
                                                .otherwise(Bindings.when(noShipyardAvailable).then(bundle.getString("error.no.shipyard.available"))
                                                        .otherwise("")))))
        );
        trainUnitButton.disableProperty().bind(
                waiting.or(locationNotOwned).or(noShipyardAvailable).or(noUnitSelected).or(notEnoughResources).or(notUnlockedYet)
        );
    }

    public void trainUnit() {
        notWaiting.set(false);

        String unitType = chooseUnitsListView.getSelectionModel().getSelectedItem().type();
        CreateJobDto createJobDto = new CreateJobDto(
                troop.location(),
                0,
                "ship",
                null,
                null,
                null,
                troop._id(),
                unitType,
                null
        );
        subscriber.subscribe(jobsApiService.createJob(troop.game(), troop.empire(), createJobDto),
                success -> notWaiting.set(true),
                error -> notWaiting.set(true)
        );
    }

    private void prepareUnitInfo() {
        // Fill focused unit information (if a unit is selected)
        chooseUnitsListView.getSelectionModel().selectedItemProperty().addListener(chooseUnitsListViewListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            final String localizedUnitName = bundle.getString(newValue.type());
            explainUnitDetails(
                    focusedUnitScrollPane, explainedVariableService, localizedUnitName, troop, unitTypeInfo,
                    newValue.type(), List.of("health", "speed", "attack", "defense", "cost", "upkeep", "build_time"),
                    unitUnlocked
            );
            checkIfEnoughResources();
        });
    }

    private void ensureResponsiveDesign() {
        ensureBasicTabResponsiveDesign(viewTabRoot, tabPane, List.of(chooseUnitsListView, focusedUnitScrollPane, unitTasksListView));
        viewTabRoot.prefWidthProperty().addListener(viewTabRootListener = (observable, oldValue, newValue) -> {
            shipyardImageView.setFitWidth(newValue.doubleValue() / 10);
            shipyardImageView.setFitHeight(newValue.doubleValue() / 10);
        });
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
        if (chooseUnitsListViewListener != null) {
            chooseUnitsListView.getSelectionModel().selectedItemProperty().removeListener(chooseUnitsListViewListener);
        }
        if (viewTabRootListener != null) {
            viewTabRoot.prefWidthProperty().removeListener(viewTabRootListener);
        }
    }
}
