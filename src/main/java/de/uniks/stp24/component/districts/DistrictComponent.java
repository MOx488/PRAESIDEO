package de.uniks.stp24.component.districts;

import de.uniks.stp24.App;
import de.uniks.stp24.component.buildings.BuildingPopUpStatComponent;
import de.uniks.stp24.dto.CreateJobDto;
import de.uniks.stp24.dto.UpdateSystemDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.rest.GameSystemsApiService;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ExplainedVariableService;
import de.uniks.stp24.service.PresetsService;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Subscription;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.function.Consumer;

import static de.uniks.stp24.util.Methods.getDurationConsumer;
import static de.uniks.stp24.util.Methods.getFreeCapacity;

@Component(view = "District.fxml")
public class DistrictComponent extends BorderPane {
    @FXML
    Button districtBuildButton;
    @FXML
    Button districtDestroyButton;
    @FXML
    ListView<DistrictBarComponent> districtList;
    @FXML
    VBox districtRoot;
    @FXML
    SplitPane districtBuildTooltipOwner;

    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public PresetsService presetsService;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public GameSystemsApiService gameSystemsApiService;
    @Inject
    public GameLogicApiService gameLogicApiService;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public EventListener eventListener;
    @Inject
    public Provider<DistrictBarComponent> districBarComponentProvider;
    @Inject
    public Provider<BuildingPopUpStatComponent> buildingPopUpStatComponentProvider;
    @Inject
    public ExplainedVariableService explainedVariableService;

    @Param("game")
    Game game;
    @Param("empire")
    Empire empire;
    @Param("system")
    GameSystem system;
    @Param("jobs")
    ObservableList<Job> jobs;

    DistrictBarComponent selected = null;
    DistrictBarComponent previouslySelected = null;
    String selectedKey = null;
    private List<District> presets;
    private Subscription jobsSubscription;


    private final SimpleBooleanProperty waiting = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty selectedOwned = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty nothingSelected = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty capacityReached = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty cantAfford = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty notMine = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isNotAbleToQueueJobs = new SimpleBooleanProperty(false);

    @Inject
    public DistrictComponent() {

    }

    @OnInit
    void init() {
        AnchorPane.setTopAnchor(this, 0.0d);
        AnchorPane.setBottomAnchor(this, 0.0d);
        AnchorPane.setLeftAnchor(this, 0.0d);
        AnchorPane.setRightAnchor(this, 0.0d);

        this.jobsSubscription = jobs.subscribe(this::checkJobCapacityExistingJob);

        this.notMine.set(this.checkNotMine());
        capacityReached.set((system.districts().size() + system.buildings().size()) >= system.capacity());

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems." + system._id() + ".updated", GameSystem.class),
                event -> {
                    final GameSystem newSystem = event.data();
                    if (system.owner() != null && newSystem.owner() != null && system.owner().equals(newSystem.owner()) && system.districtSlots().equals(newSystem.districtSlots()) && system.districts().equals(newSystem.districts())) {
                        return;
                    }

                    this.system = newSystem;
                    this.notMine.set(this.checkNotMine());
                    this.updateDistricts();
                    this.checkCapacity();
                });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), event -> {
                    // Ignore same server responses
                    final Empire newEmpire = event.data();
                    if (newEmpire.equals(empire)) {
                        return;
                    }

                    this.updatePriceOnResourceChange(newEmpire);
                    this.updateToolTipOnVariableChange(newEmpire);

                    this.empire = event.data();
                }
        );
    }

    private void checkJobCapacityExistingJob() {
        final District district = getDistrict();
        if (district == null) {
            return;
        }

        final int currentTasksThatIncreaseCapacity = jobs.stream().filter(job -> (job.type().equals("district") || job.type().equals("building")) && job.progress() != job.total()).toList().size();

        final int currentTasksForTheSelectedDistrictType = jobs.stream().filter(job -> job.type().equals("district") && job.district().equals(district.id()) && job.progress() != job.total()).toList().size();
        final int totalSlotsForTheCurrentDistrictType = system.districtSlots().get(district.id());
        final int occupiedSlotsForTheCurrentDistrictType = system.districts().get(district.id());
        final int availableSlotsForTheCurrentDistrictType = totalSlotsForTheCurrentDistrictType - occupiedSlotsForTheCurrentDistrictType;
        final int availableSlotsForTheCurrentDistrictTypeAfterCurrentJobs = availableSlotsForTheCurrentDistrictType - currentTasksForTheSelectedDistrictType;

        final int freeCapacity = getFreeCapacity(system, currentTasksThatIncreaseCapacity);

        this.isNotAbleToQueueJobs.set(availableSlotsForTheCurrentDistrictTypeAfterCurrentJobs <= 0 || freeCapacity <= 0);
    }

    private void updateToolTipOnVariableChange(Empire newEmpire) {
        if (newEmpire.traits().equals(empire.traits()) && newEmpire.technologies().equals(empire.technologies())) {
            return;
        }

        this.buildTooltip();
    }

    private void updatePriceOnResourceChange(Empire newEmpire) {
        if (newEmpire.resources().equals(empire.resources())) {
            return;
        }

        this.checkPrice();
    }

    private void checkCapacity() {
        final int currentCapacity = calculateDistricts() + system.buildings().size();
        final int maxCapacity = system.capacity();

        this.capacityReached.set(currentCapacity >= maxCapacity);
    }

    private boolean checkNotMine() {
        return (this.system.owner() == null) || (!system.owner().equals(empire._id()));
    }

    private int calculateDistricts() {
        int districtSum = 0;
        for (Map.Entry<String, Integer> stringIntegerEntry : system.districts().entrySet()) {
            districtSum += stringIntegerEntry.getValue();
        }
        return districtSum;
    }

    @OnRender
    public void render() {
        this.updateDistricts();

        districtBuildButton.disableProperty().bind(selectedOwned.or(nothingSelected).or(capacityReached).or(waiting).or(cantAfford).or(isNotAbleToQueueJobs));
        districtDestroyButton.disableProperty().bind(selectedOwned.not().or(nothingSelected).or(waiting));
    }

    private void updateDistricts() {
        districtList.getItems().clear();

        subscriber.subscribe(presetsService.getCachedPreset("getDistricts"), presets -> {
            this.presets = (List<District>) presets;
            this.renderBarComponents();
        });
    }

    private void renderBarComponents() {
        for (String district : system.districtSlots().keySet()) {
            DistrictBarComponent districtBarComponent = app.initAndRender(districBarComponentProvider.get(),
                    Map.of("game", game, "empire", empire, "system", system, "district", district, "presets", presets, "parent", this, "notMySystem", checkNotMine(), "subscriber", subscriber),
                    subscriber
            );
            districtList.getItems().add(districtBarComponent);
        }
    }

    public void buildDistrict() {
        waiting.set(true);

        CreateJobDto createJobDto = new CreateJobDto(
                system._id(), 0, "district", null, selectedKey, null, null, null, null
        );

        subscriber.subscribe(jobsApiService.createJob(game._id(), empire._id(), createJobDto),
                result -> {
                    waiting.set(false);
                    deselect();
                }, error -> waiting.set(false)
        );

    }

    public void destroyDistrict() {
        waiting.set(true);

        TreeMap<String, Integer> newDistricts = new TreeMap<>(system.districtSlots());

        for (Map.Entry<String, Integer> stringIntegerEntry : newDistricts.entrySet()) {
            if (stringIntegerEntry.getKey().equals(selectedKey)) {
                stringIntegerEntry.setValue(-1);
            } else {
                stringIntegerEntry.setValue(0);
            }
        }
        updateSystem(newDistricts);
    }

    private void updateSystem(TreeMap<String, Integer> newDistricts) {
        UpdateSystemDto newSystem = new UpdateSystemDto(
                null, newDistricts,
                null, null, system.owner(), null
        );

        subscriber.subscribe(gameSystemsApiService.updateSystem(game._id(), system._id(), newSystem),
                result -> {
                    this.waiting.set(false);
                    this.system = result;
                    this.updateDistricts();
                    this.deselect();
                    this.waiting.set(false);
                }, error -> waiting.set(false)
        );
    }

    public void select(DistrictBarComponent component, String key, boolean owned) {
        if (this.selected != null) {
            this.selected.deselect();
        }

        this.selected = component;
        this.selectedKey = key;
        this.selectedOwned.set(owned);
        this.nothingSelected.set(false);
        this.checkJobCapacityExistingJob();

        if (owned || (previouslySelected != null && selectedKey.equals(previouslySelected.key))) {
            //dont check price/build tooltip if its already owned or if its in the same category as the price doesnt change
            return;
        }

        this.previouslySelected = this.selected;

        this.checkPrice();
        this.buildTooltip();
    }

    private void checkPrice() {
        final District district = getDistrict();
        if (district == null) {
            return;
        }

        district.cost().forEach((resource, amount) -> cantAfford.set(empire.resources().getOrDefault(resource, 0) < amount));
    }

    public void deselect() {
        if (selected == null) {
            return;
        }

        this.selected.deselect();
        this.nothingSelected.set(true);
    }

    private void buildTooltip() {
        final District district = getDistrict();
        if (district == null) {
            return;
        }

        Consumer<VBox> afterBuildingTooltip = getDurationConsumer(bundle, districtBuildTooltipOwner);
        final String localizedDistrictName = bundle.getString("districts.name." + district.id());
        this.explainedVariableService.buildExplainedVariableToolTip(localizedDistrictName, empire._id(), "districts", district.id(), district, List.of("cost", "upkeep", "production", "build_time"), afterBuildingTooltip, true);
    }

    private @Nullable District getDistrict() {
        if (presets == null) {
            return null;
        }

        return presets.stream().filter(d -> d.id().equals(selectedKey)).findFirst().orElse(null);
    }

    @OnDestroy
    void destroy() {
        explainedVariableService.destroy();
        jobsSubscription.unsubscribe();
        subscriber.dispose();
    }
}
