package de.uniks.stp24.component.buildings;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.CreateJobDto;
import de.uniks.stp24.dto.UpdateSystemDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.rest.GameSystemsApiService;
import de.uniks.stp24.rest.GamesApiService;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ExplainedVariableService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.PresetsService;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Subscription;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.controller.SubComponent;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.*;
import java.util.function.Consumer;

import static de.uniks.stp24.util.Methods.*;

@Component(view = "BuildingsView.fxml")
public class BuildingsViewComponent extends BorderPane {
    @FXML
    Label buildingErrorLabel;
    @FXML
    Text buildingsTitle;
    @FXML
    VBox buildingsVBox;
    @FXML
    Button buildButton;
    @FXML
    Button destroyButton;
    @FXML
    ListView<String> buildingList;
    @FXML
    GridPane iconsMatrix;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public GameSystemsApiService gameSystemsApiService;
    @Inject
    GamesApiService gamesApiService;
    @Inject
    public JobsApiService jobsApiService;
    @SubComponent
    @Inject
    public BuildingStatsViewComponent buildingStatsComponent;
    @Inject
    public Provider<BuildingComponent> buildingComponentProvider;
    @Inject
    public Provider<BuildingPopUpStatComponent> buildingPopUpStatComponentProvider;
    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    @Named("building-icons")
    public ResourceBundle buildingBundle;
    @Inject
    public EventListener eventListener;
    @Inject
    public PresetsService presetsService;
    @Inject
    public ExplainedVariableService explainedVariableService;
    @Inject
    public GameLogicApiService gameLogicApiService;

    @Param("empire")
    Empire empire;
    @Param("game")
    Game game;
    @Param("system")
    GameSystem system;
    @Param("jobs")
    ObservableList<Job> jobs;

    private Subscription jobsSubscription;
    private VBox selectedBuildIcon;
    private Map<String, Integer> currentResources;

    private final Map<String, Building> storeBuildings = new HashMap<>();
    private final ObservableList<Building> allBuildings = FXCollections.observableArrayList();
    private final ObservableList<String> buildings = FXCollections.observableArrayList();

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty canNotAffordBinding = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty notMine = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty hasJobInProgress = new SimpleBooleanProperty(false);

    private ChangeListener<String> buildingListListener;


    @Inject
    public BuildingsViewComponent() {
    }

    @OnInit
    void init() {
        AnchorPane.setTopAnchor(this, 0.0d);
        AnchorPane.setBottomAnchor(this, 0.0d);
        AnchorPane.setLeftAnchor(this, 0.0d);
        AnchorPane.setRightAnchor(this, 0.0d);

        this.notMine.set(this.checkNotMine());
        this.buildings.setAll(system.buildings());
        this.currentResources = empire.resources();

        this.jobsSubscription = jobs.subscribe(this::checkAlreadyExistingJob);

        subscriber.subscribe(presetsService.getCachedPreset("getBuildings"), buildings -> {
                    final List<Building> buildingsList = (List<Building>) buildings;
                    this.allBuildings.setAll(buildingsList);

                    this.storeBuildings.clear();
                    for (Building building : buildingsList) {
                        this.storeBuildings.put(building.id(), building);
                    }

                    this.renderExistingBuildings();
                    this.renderBuildIcons();

                }
        );

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems." + system._id() + ".updated", GameSystem.class),
                event -> {
                    final GameSystem newSystem = event.data();
                    this.system = newSystem;
                    this.checkCostAndCapacity();

                    if (this.buildings.equals(newSystem.buildings())) {
                        return;
                    }

                    this.buildings.setAll(newSystem.buildings());
                    this.notMine.set(this.checkNotMine());
                }
        );

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class),
                event -> {
                    final Empire newEmpire = event.data();
                    if (newEmpire.equals(this.empire)) {
                        return;
                    }

                    this.updateOnResourceChange(newEmpire);
                    this.updateOnVariablesEffectingChange(newEmpire);
                    this.empire = newEmpire;
                }
        );
    }

    private void updateOnVariablesEffectingChange(Empire empire) {
        if (empire.traits().equals(this.empire.traits()) && empire.technologies().equals(this.empire.technologies())) {
            return;
        }

        this.renderBuildIcons();
    }

    private void updateOnResourceChange(Empire empire) {
        final TreeMap<String, Integer> resources = empire.resources();
        if (resources.equals(this.currentResources)) {
            return;
        }

        this.currentResources = resources;
        this.checkCostAndCapacity();
    }

    void renderExistingBuildings() {
        initListView(buildingList, buildings, app, buildingComponentProvider, Map.of());

        this.buildingList.getSelectionModel().selectedItemProperty().addListener(buildingListListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                buildingsVBox.getChildren().remove(buildingStatsComponent);
                return;
            }

            if (Objects.equals(oldValue, newValue)) {
                return;
            }

            this.buildingStatsComponent.setBuildingStats(newValue, this.storeBuildings);
            if (buildingStatsComponent.getParent() != null) {
                return;
            }

            //parent is null -> add it to buildingsVbox
            this.buildingsVBox.getChildren().add(buildingStatsComponent);
        });

        final BooleanBinding waiting = notWaiting.not();
        final BooleanBinding noBuildingSelected = buildingList.getSelectionModel().selectedItemProperty().isNull();

        this.destroyButton.disableProperty().bind(noBuildingSelected.or(waiting).or(notMine));
        this.buildButton.disableProperty().bind(canNotAffordBinding.or(waiting).or(hasJobInProgress));
        this.buildingErrorLabel.visibleProperty().bind(canNotAffordBinding.or(waiting).or(hasJobInProgress));
    }

    public void renderBuildIcons() {
        for (int i = 0; i < iconsMatrix.getChildren().size(); i++) {
            final Node node = iconsMatrix.getChildren().get(i);

            if (!(node instanceof SplitPane splitPane)) {
                continue;
            }

            final VBox vbox = (VBox) splitPane.getItems().getFirst();
            final Node imageNode = vbox.getChildren().getFirst();

            if (!(imageNode instanceof ImageView imageView)) {
                continue;
            }

            final Building building = allBuildings.get(i);
            final String buildingName = building.id();

            final String imageName = buildingBundle.getString("building.icon." + buildingName);
            final String path = "image/game_resources/" + imageName + ".png";

            //assign images/ids
            imageView.setImage(imageCache.get(path));
            vbox.setId(buildingName);

            //build tooltip for building icon
            this.buildToolTip(splitPane, building);
        }
    }

    public void clickedBuildIcon(MouseEvent event) {
        if (this.checkNotMine()) {
            return;
        }

        final VBox clickedVbox = (VBox) event.getSource();
        if (clickedVbox == null || clickedVbox == selectedBuildIcon || clickedVbox.getChildren().isEmpty()) {
            return;
        }

        if (this.selectedBuildIcon != null) {
            this.selectedBuildIcon.setStyle("");
        }

        clickedVbox.setStyle("-fx-background-color: yellow;");

        this.selectedBuildIcon = clickedVbox;
        this.checkCostAndCapacity();
        this.checkAlreadyExistingJob();
    }

    private boolean checkNotMine() {
        return (system.owner() == null) || (!system.owner().equals(empire._id()));
    }

    private void buildToolTip(SplitPane toolTipOwner, Building building) {
        if (building == null) {
            return;
        }

        Consumer<VBox> afterBuildingTooltip = getDurationConsumer(bundle, toolTipOwner);

        final String localizedBuildingName = bundle.getString("building." + building.id());

        if (!building.production().isEmpty()) {
            this.explainedVariableService.buildExplainedVariableToolTip(localizedBuildingName, empire._id(), "buildings", building.id(), building, List.of("cost", "upkeep", "production", "build_time"), afterBuildingTooltip, true);
        } else {
            if (building.defense() > 0) {
                this.explainedVariableService.buildExplainedVariableToolTip(localizedBuildingName, empire._id(), "buildings", building.id(), building, List.of("cost", "upkeep", "health", "defense", "build_time"), afterBuildingTooltip, true);
            } else {
                this.explainedVariableService.buildExplainedVariableToolTip(localizedBuildingName, empire._id(), "buildings", building.id(), building, List.of("cost", "upkeep", "healing_rate", "build_time"), afterBuildingTooltip, true);
            }
        }
    }

    private void checkCostAndCapacity() {
        // check if user has enough resources and capacity to build the building
        if (this.selectedBuildIcon == null) {
            this.buildButton.setTooltip(null);
            this.buildingErrorLabel.setText("No building selected");
            return;
        }

        final Building building = this.storeBuildings.get(selectedBuildIcon.getId());

        boolean canNotBuild = false;
        int buildingsSize = this.system.buildings().size();
        int districtsSize = this.system.districts().values().stream().reduce(0, Integer::sum);
        if (buildingsSize + districtsSize >= this.system.capacity()) {
            canNotAffordBinding.set(true);
            this.buildingErrorLabel.setText("Not enough capacity");
            return;
        }

        for (Map.Entry<String, Integer> costEntry : building.cost().entrySet()) {
            String resource = costEntry.getKey();
            Integer costOfResource = costEntry.getValue();
            if (this.currentResources.get(resource) == null || this.currentResources.get(resource) < costOfResource) {
                canNotBuild = true;
                this.buildingErrorLabel.setText("Not enough resources");
                break;
            }
        }

        canNotAffordBinding.set(canNotBuild);
    }

    private void checkAlreadyExistingJob() {
        // check if user has enough resources and capacity to build the building
        if (this.selectedBuildIcon == null) {
            return;
        }

        final Building building = this.storeBuildings.get(selectedBuildIcon.getId());
        if (building == null) {
            return;
        }

        final int currentTasksThatIncreaseCapacity = jobs.stream().filter(job -> (job.type().equals("district") || job.type().equals("building")) && job.progress() != job.total()).toList().size();

        final int freeCapacity = getFreeCapacity(system, currentTasksThatIncreaseCapacity);

        this.hasJobInProgress.set(freeCapacity <= 0);
    }

    public void buildBuilding() {
        notWaiting.set(false);
        String newBuildingName = selectedBuildIcon.getId();

        CreateJobDto createJobDto = new CreateJobDto(
                this.system._id(), 0, "building", newBuildingName, null, null, null, null, null
        );

        subscriber.subscribe(jobsApiService.createJob(game._id(), empire._id(), createJobDto),
                result -> {
                    notWaiting.set(true);
                    checkCostAndCapacity();
                },
                error -> {
                    notWaiting.set(true);
                    checkCostAndCapacity();
                });
    }

    public void destroyBuilding() {
        notWaiting.set(false);
        int deleteBuildingIndex = buildingList.getSelectionModel().getSelectedIndex();

        // delete building from the castle
        List<String> newBuildings = new ArrayList<>(buildings);
        newBuildings.remove(deleteBuildingIndex);

        updateCastle(newBuildings);
    }

    private void updateCastle(List<String> newBuildings) {
        UpdateSystemDto newSystem = new UpdateSystemDto(
                null, null,
                newBuildings, null, system.owner(), null
        );
        subscriber.subscribe(gameSystemsApiService.updateSystem(game._id(), system._id(), newSystem),
                result -> {
                    notWaiting.set(true);
                    checkCostAndCapacity();
                },
                error -> {
                    notWaiting.set(true);
                    checkCostAndCapacity();
                }
        );
    }

    @OnDestroy
    public void onDestroy() {
        explainedVariableService.destroy();
        jobsSubscription.unsubscribe();
        subscriber.dispose();
        if (buildingListListener != null) {
            buildingList.getSelectionModel().selectedItemProperty().removeListener(buildingListListener);
        }
    }
}
