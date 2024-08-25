package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.component.enhancements.EnhancementComponent;
import de.uniks.stp24.component.war.DiplomacyComponent;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.PresetsService;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

import static de.uniks.stp24.util.Methods.onTaskEvent;

@Component(view = "SideButtons.fxml")
public class SideButtonsComponent extends VBox {
    @FXML
    VBox sideButtonRoot;
    @FXML
    Label taskToolTipLabel;
    @FXML
    ImageView taskButton;
    @FXML
    Text taskText;
    @FXML
    Label marketToolTipLabel;
    @FXML
    ImageView marketButton;
    @FXML
    Text marketText;
    @FXML
    Label enhancementsToolTipLabel;
    @FXML
    ImageView enhancementsButton;
    @FXML
    Text enhancementsText;
    @FXML
    StackPane sideContainer;
    @FXML
    HBox taskBox;
    @FXML
    HBox marketBox;
    @FXML
    HBox enhancementsBox;
    @FXML
    HBox buildFleetsBox;
    @FXML
    Label buildFleetsTooltipLabel;
    @FXML
    ImageView buildFleetsButton;
    @FXML
    Text buildFleetsText;
    @FXML
    HBox diplomacyBox;
    @FXML
    ImageView diplomacyButton;
    @FXML
    Text diplomacyText;
    @FXML
    Label diplomacyToolTipLabel;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public ImageCache imageCache;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public EventListener eventListener;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public PresetsService presetsService;
    @Inject
    public Provider<MarketComponent> marketComponentProvider;
    @Inject
    public Provider<EnhancementComponent> enhancementComponentProvider;
    @Inject
    public Provider<TasksViewComponent> tasksViewComponentProvider;
    @Inject
    public Provider<BuildFleetComponent> buildFleetComponentProvider;
    @Inject
    public Provider<DiplomacyComponent> diplomacyComponentProvider;

    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("game")
    Game game;
    @Param("empire")
    Empire empire;
    @Param("systems")
    ObservableList<GameSystem> systems;
    @Param("troopsListContainer")
    VBox troopsListContainer;
    @Param("parent")
    AnchorPane parentContainer;
    @Param("players")
    ObservableList<Player> players;

    public List<Technology> enhancements;
    private final ObservableList<Job> jobs = FXCollections.observableArrayList();
    public final List<HBox> boxes = new ArrayList<>();
    private EnhancementComponent enhancementComponent;
    private TasksViewComponent taskComponent;
    private MarketComponent marketComponent;
    private BuildFleetComponent buildFleetComponent;
    private DiplomacyComponent diplomacyComponent;

    @Inject
    public SideButtonsComponent() {
    }

    @OnInit
    public void init() {
        subscriber.subscribe(jobsApiService.getFilteredJobs(game._id(), empire._id(), "technology", null, null), this.jobs::setAll);

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".jobs.*.*", Job.class),
                jobsEvent -> onTaskEvent(jobsEvent, jobs)
        );

        subscriber.subscribe(presetsService.getCachedPreset("getTechnologies"), presets -> {
            this.enhancements = (List<Technology>) presets;
            this.initEnhancementComponent();
        });
    }

    @OnRender
    public void renderImages() {
        boxes.addAll(Arrays.asList(taskBox, marketBox, enhancementsBox, diplomacyBox, buildFleetsBox));

        this.taskButton.setImage(imageCache.get("image/icons/tasks.png"));
        this.taskText.setText(bundle.getString("tasks.tooltip"));
        this.taskToolTipLabel.getTooltip().setShowDelay(new Duration(0));
        this.initTaskComponent();

        this.marketButton.setImage(imageCache.get("image/icons/market.png"));
        this.marketText.setText(bundle.getString("market.tooltip"));
        this.marketToolTipLabel.getTooltip().setShowDelay(new Duration(0));
        this.initMarketComponent();

        this.enhancementsButton.setImage(imageCache.get("image/icons/enhancements.png"));
        this.enhancementsText.setText(bundle.getString("enhancement.tooltip"));
        this.enhancementsToolTipLabel.getTooltip().setShowDelay(new Duration(0));

        this.buildFleetsButton.setImage(imageCache.get("image/buildFleetIcon.png"));
        this.buildFleetsText.setText(bundle.getString("build.fleet.tooltip"));
        this.buildFleetsTooltipLabel.getTooltip().setShowDelay(new Duration(0));
        this.initBuildFleetComponent();

        this.diplomacyButton.setImage(imageCache.get("image/icons/diplomacy.png"));
        this.diplomacyText.setText(bundle.getString("diplomacy.tooltip"));
        this.diplomacyToolTipLabel.getTooltip().setShowDelay(new Duration(0));
        this.initDiplomacyComponent();
    }

    public void openTasks() {
        openClickedView(taskBox);
    }

    public void openMarket() {
        openClickedView(marketBox);
    }

    public void openEnhancements() {
        openClickedView(enhancementsBox);
    }

    public void openBuildFleet() {
        openClickedView(buildFleetsBox);
    }

    public void openDiplomacy() {
        openClickedView(diplomacyBox);
    }

    public void openClickedView(HBox clickedBox) {
        for (HBox box : boxes) {
            if (!box.getStyleClass().contains("enhancement-selected")) {
                continue;
            }

            box.getStyleClass().remove("enhancement-selected");
            box.getStyleClass().add("enhancement-not-selected");
        }

        openView(clickedBox);
    }

    public void openView(HBox box) {
        box.getStyleClass().remove("enhancement-not-selected");
        box.getStyleClass().add("enhancement-selected");

        final boolean isTaskBox = box.equals(taskBox);
        final boolean isEnhancementsBox = box.equals(enhancementsBox);
        final boolean isMarketBox = box.equals(marketBox);
        final boolean isBuildFleetBox = box.equals(buildFleetsBox);
        final boolean isDiplomacyBox = box.equals(diplomacyBox);

        taskComponent.setVisible(isTaskBox && !taskComponent.isVisible());
        enhancementComponent.setVisible(isEnhancementsBox && !enhancementComponent.isVisible());
        marketComponent.setVisible(isMarketBox && !marketComponent.isVisible());
        buildFleetComponent.setVisible(isBuildFleetBox && !buildFleetComponent.isVisible());
        diplomacyComponent.setVisible(isDiplomacyBox && !diplomacyComponent.isVisible());

        if (!taskComponent.isVisible() && !enhancementComponent.isVisible() && !marketComponent.isVisible() && !buildFleetComponent.isVisible() && !diplomacyComponent.isVisible()) {
            box.getStyleClass().remove("enhancement-selected");
            box.getStyleClass().add("enhancement-not-selected");
            troopsListContainer.setVisible(true);
            return;
        }
        troopsListContainer.setVisible(false);
    }

    private void initTaskComponent() {
        this.taskComponent = app.initAndRender(tasksViewComponentProvider.get(),
                Map.of("game", game, "empire", empire, "systems", systems), subscriber);
        this.sideContainer.getChildren().add(taskComponent);
        this.taskComponent.setVisible(false);
    }

    private void initEnhancementComponent() {
        this.enhancementComponent = app.initAndRender(enhancementComponentProvider.get(),
                Map.of("game", game, "empire", empire, "jobs", jobs, "allEnhancements", enhancements), subscriber);
        this.sideContainer.getChildren().add(this.enhancementComponent);
        this.enhancementComponent.setVisible(false);
    }

    private void initMarketComponent() {
        this.marketComponent = app.initAndRender(marketComponentProvider.get(),
                Map.of("game", game, "empire", empire), subscriber);
        this.sideContainer.getChildren().add(this.marketComponent);
        this.marketComponent.setVisible(false);
    }

    private void initBuildFleetComponent() {
        this.buildFleetComponent = app.initAndRender(buildFleetComponentProvider.get(),
                Map.of("game", game, "empire", empire, "systems", systems), subscriber);
        this.sideContainer.getChildren().add(this.buildFleetComponent);
        this.buildFleetComponent.setVisible(false);
    }

    private void initDiplomacyComponent() {
        this.diplomacyComponent = app.initAndRender(diplomacyComponentProvider.get(),
                Map.of("game", game, "empire", empire, "parent", parentContainer, "players", players), subscriber);
        this.sideContainer.getChildren().add(this.diplomacyComponent);
        this.diplomacyComponent.setVisible(false);
    }

    public boolean unhideTroopsListIfAllowed() {
        return !taskComponent.isVisible() && !enhancementComponent.isVisible() && !marketComponent.isVisible() && !buildFleetComponent.isVisible() && !diplomacyComponent.isVisible();
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }
}