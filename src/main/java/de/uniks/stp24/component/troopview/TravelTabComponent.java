package de.uniks.stp24.component.troopview;

import de.uniks.stp24.App;
import de.uniks.stp24.component.TaskComponent;
import de.uniks.stp24.model.Fleet;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.model.Job;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
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

import static de.uniks.stp24.util.Methods.createLabel;

@Component(view = "TravelTab.fxml")
public class TravelTabComponent extends AnchorPane {
    @FXML
    AnchorPane travelTabRoot;
    @FXML
    VBox travelTaskBox;

    @Inject
    public App app;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public Provider<TaskComponent> taskComponentProvider;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("tabPane")
    TabPane tabPane;
    @Param("troop")
    Fleet troop;
    @Param("systems")
    ObservableList<GameSystem> systems;

    private Job currentTravelJob;
    private Label noTaskLabel;

    @Inject
    public TravelTabComponent() {
    }

    @OnInit
    void onInit() {
        // Get the current travel job of this troop
        subscriber.subscribe(jobsApiService.getFilteredJobs(troop.game(), troop.empire(), "travel", troop._id(), null), jobs -> {
            // There can only be one travel job at a time, so utilise getFirst()
            currentTravelJob = !jobs.isEmpty() ? jobs.getFirst() : null;
            fillTaskBox();
        });

        // Update travel job progress
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".empires." + troop.empire() + ".jobs.*.*", Job.class),
                event -> {
                    Job job = event.data();

                    // Ignore identical events
                    if (currentTravelJob != null && currentTravelJob.equals(job)) {
                        return;
                    }

                    // Only look at travel jobs from this troop
                    if (!troop._id().equals(job.fleet()) || !job.type().equals("travel")) {
                        return;
                    }

                    currentTravelJob = job;

                    switch (event.suffix()) {
                        case "created" -> fillTaskBox();
                        case "updated" -> updateTask();
                        case "deleted" -> {
                            travelTaskBox.getChildren().clear();
                            travelTaskBox.getChildren().add(noTaskLabel);
                        }
                    }
                }
        );
    }

    @OnRender
    void onRender() {
        travelTabRoot.prefWidthProperty().bind(tabPane.widthProperty());
        travelTabRoot.prefHeightProperty().bind(tabPane.heightProperty());

        // Show "No task" message if there is no travel job
        noTaskLabel = createLabel(bundle.getString("no.travel"), "medium");
        travelTaskBox.getChildren().add(noTaskLabel);
    }

    private void fillTaskBox() {
        if (currentTravelJob == null) {
            return;
        }

        travelTaskBox.getChildren().clear();
        TaskComponent taskComponent = app.initAndRender(
                taskComponentProvider.get(),
                Map.of("systems", systems, "dontShowDetails", true, "dontShowCancel", true, "parent", travelTaskBox),
                subscriber
        );
        taskComponent.setItem(currentTravelJob);
        travelTaskBox.getChildren().add(taskComponent);
    }

    private void updateTask() {
        ((TaskComponent) travelTaskBox.getChildren().getFirst()).setItem(currentTravelJob);
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
    }
}
