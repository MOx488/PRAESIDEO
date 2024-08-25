package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.component.buildings.BuildingsViewComponent;
import de.uniks.stp24.component.districts.DistrictComponent;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.model.Job;
import de.uniks.stp24.rest.GameSystemsApiService;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.DiscordActivityService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.onBasicListEvent;


@Component(view = "CastleView.fxml")
public class CastleViewComponent extends AnchorPane implements CloseableView {
    @FXML
    ImageView castleViewBackImage;
    @FXML
    AnchorPane statisticsContainer;
    @FXML
    AnchorPane districtsContainer;
    @FXML
    HBox castleViewDistrictBuildingContainer;
    @FXML
    VBox buildingsDistrictsContainer;
    @FXML
    Label castleNameLabel;
    @FXML
    HBox castleNameContainer;
    @FXML
    Pane exploreCastleContainer;
    @FXML
    AnchorPane buildingsContainer;
    @FXML
    Button castleViewBackButton;
    @Inject
    public DiscordActivityService discordActivityService;
    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public GameSystemsApiService gameSystemsApiService;
    @Inject
    public ImageCache imageCache;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public EventListener eventListener;
    @Inject
    public Provider<ExploreCastleComponent> exploreCastleComponentProvider;
    @Inject
    public Provider<BuildingsViewComponent> buildingsViewComponentProvider;
    @Inject
    public Provider<StatisticsComponent> statisticsComponentProvider;
    @Inject
    public Provider<DistrictComponent> districtComponentProvider;

    @Param("game")
    Game game;
    @Param("system")
    public GameSystem system;
    @Param("empire")
    Empire empire;
    @Param("sideBar")
    VBox sideBar;
    @Param("sideButtons")
    VBox sideButtons;
    @Param("troopsListContainer")
    VBox troopsListContainer;
    @Param("sideButtonsComponent")
    SideButtonsComponent sideButtonsComponent;

    private final ChangeListener<Bounds> castleViewWidthListener = (observable, oldValue, newValue) -> this.centerNameTag(newValue.getWidth());
    private final ObservableList<Job> systemJobs = FXCollections.observableArrayList();

    private Job lastJobUpdate;

    private void centerNameTag(Double castleViewWidth) {
        this.castleNameContainer.setLayoutX(buildingsDistrictsContainer.getBoundsInParent().getMinX() + castleViewWidth / 2 - this.castleNameContainer.getWidth() / 2);
    }

    @Inject
    public CastleViewComponent() {
    }

    @OnInit
    void onInit() {
        discordActivityService.setActivity(bundle.getString("discord.in.castle.uppercase"), "");

        subscriber.subscribe(jobsApiService.getFilteredJobs(game._id(), empire._id(), null, null, system._id()), jobs -> {
            systemJobs.setAll(jobs);
            this.renderSubComponents();
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".jobs.*.*", Job.class), event -> {
                    final Job job = event.data();
                    if (job.system() == null || !job.system().equals(system._id())) {
                        return;
                    }

                    // Ignore identical updates
                    if (lastJobUpdate != null && lastJobUpdate._id().equals(job._id())) {
                        return;
                    }

                    onBasicListEvent(event, systemJobs);

                    lastJobUpdate = job;
                }
        );

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems." + system._id() + ".updated", GameSystem.class), event -> {
            final GameSystem newSystem = event.data();

            // Ignore identical events
            if (this.system.equals(newSystem)) {
                return;
            }

            // If the system has been developed, the ExploreCastleComponent should disappear
            if (newSystem.upgrade().equals("developed")) {
                exploreCastleContainer.getChildren().clear();
            }

            this.system = newSystem;
        });
    }

    @OnRender
    public void onRender() {
        this.sideBar.setVisible(false);
        this.sideButtons.setVisible(false);
        this.troopsListContainer.setVisible(false);
        this.castleViewBackImage.setImage(imageCache.get("image/cross_red.png"));
        this.castleViewBackButton.setOnAction(event -> this.closeView());

        this.renderNameTag();
    }

    private void renderNameTag() {
        this.castleNameLabel.setText(system.name());
        this.centerNameTag(this.castleViewDistrictBuildingContainer.getBoundsInParent().getWidth());
        this.castleViewDistrictBuildingContainer.boundsInParentProperty().addListener(castleViewWidthListener);
    }

    private void renderSubComponents() {
        DistrictComponent districtComponent = app.initAndRender(districtComponentProvider.get(),
                Map.of("game", game, "empire", empire, "system", system, "jobs", systemJobs),
                subscriber
        );
        this.districtsContainer.getChildren().add(districtComponent);

        if (!system.upgrade().equals("developed")) {
            ExploreCastleComponent exploreCastleComponent = app.initAndRender(exploreCastleComponentProvider.get(),
                    Map.of("game", game, "empire", empire, "system", system, "jobs", systemJobs),
                    subscriber
            );
            this.exploreCastleContainer.getChildren().add(exploreCastleComponent);
        }

        BuildingsViewComponent buildingsViewComponent = app.initAndRender(buildingsViewComponentProvider.get(),
                Map.of("game", game, "empire", empire, "system", system, "jobs", systemJobs),
                subscriber
        );
        this.buildingsContainer.getChildren().add(buildingsViewComponent);

        StatisticsComponent statisticsComponent = app.initAndRender(
                statisticsComponentProvider.get(),
                Map.of("game", game, "empire", empire, "system", system),
                subscriber
        );

        this.statisticsContainer.getChildren().add(statisticsComponent);
    }

    @Override
    public void closeView() {
        //get parent and delete ourselves
        this.onDestroy();
        this.removeCastleView();

        //unhide player list / city list
        this.sideBar.setVisible(true);
        this.sideButtons.setVisible(true);

        if (sideButtonsComponent != null && sideButtonsComponent.unhideTroopsListIfAllowed()) {
            this.troopsListContainer.setVisible(true);
        }
    }

    private void removeCastleView() {
        AnchorPane parent = (AnchorPane) this.getParent();
        parent.getChildren().remove(this);
    }

    @OnDestroy
    public void onDestroy() {
        this.subscriber.dispose();
        this.boundsInParentProperty().removeListener(castleViewWidthListener);
    }
}
