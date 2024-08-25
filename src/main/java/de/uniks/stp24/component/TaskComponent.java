package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.model.Job;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.JobService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.showNode;

@Component(view = "Task.fxml")
public class TaskComponent extends HBox implements ReusableItemComponent<Job> {
    @FXML
    HBox jobDetailsBox;
    @FXML
    Label taskName;
    @FXML
    Label taskType;
    @FXML
    Label systemName;
    @FXML
    ProgressBar progressBar;
    @FXML
    Label progressPercentage;
    @FXML
    HBox progressHBox;
    @FXML
    Label inQueueLabel;
    @FXML
    ImageView crossImageView;
    @FXML
    Text tooltipText;
    @FXML
    Label cancelTooltip;
    @FXML
    VBox tooltipVBox;

    @Param("systems")
    ObservableList<GameSystem> systems;
    @Param("dontShowDetails")
    boolean dontShowDetails;
    @Param("dontShowCancel")
    boolean dontShowCancel;
    @Param("listView")
    ListView<Job> listView;
    @Param("parent")
    Node parent;

    @Inject
    public App app;
    @Inject
    public ImageCache imageCache;
    @Inject
    public Subscriber subscriber;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public JobService jobService;
    @Inject
    @Resource
    public ResourceBundle bundle;

    private final Map<String, String> nextSystemUpgrade = Map.of(
            "unexplored", "explored",
            "explored", "colonized",
            "colonized", "upgraded",
            "upgraded", "developed",
            "developed", "no upgrade left"
    );

    @Inject
    public TaskComponent() {
    }

    @Override
    public void setItem(@NotNull Job job) {
        if (job.progress() == 0) { // check if job is in queue or in progress
            taskInQueue();
        } else {
            taskInProgress();
        }

        // initialize cancel button
        initCancelButton(job);
        switch (job.type()) { // switch statement for different job types
            case "upgrade" -> whenTypeUpgrade(job);
            case "building" -> whenTypeBuilding(job);
            case "district" -> whenTypeDistrict(job);
            case "technology" -> whenTypeTechnology(job);
            case "ship" -> whenTypeShip(job);
            case "travel" -> whenTypeTravel(job);
        }
        taskType.setText(bundle.getString(job.type()));
        // set progress bar and progress percentage
        double progress = (double) job.progress() / job.total();
        progressBar.setProgress(progress);

        showNode(jobDetailsBox, !dontShowDetails);
        showNode(cancelTooltip, !dontShowCancel);

        this.progressPercentage.setText(this.jobService.getJobEndDate(job));

        // Responsive design
        if (listView != null) {
            this.prefWidthProperty().bind(listView.widthProperty().multiply(0.8));
        } else if (parent != null && parent instanceof Pane) {
            this.prefWidthProperty().bind(((Pane) parent).prefWidthProperty());
        }
        progressBar.prefWidthProperty().bind(this.widthProperty().multiply(0.5));
    }

    private void initCancelButton(Job job) {
        // set cancel button tooltip
        cancelButtonTooltip(job);
        // when cancel button is clicked, delete job
        cancelTooltip.setOnMouseClicked(event -> subscriber.subscribe(jobsApiService.deleteJob(job.game(), job.empire(), job._id())));
    }

    private void cancelButtonTooltip(Job job) {
        cancelTooltip.getTooltip().setShowDelay(new Duration(0));
        crossImageView.setImage(imageCache.get("image/cross_red.png"));
        tooltipText.setText(bundle.getString("refund.on.cancel"));
        tooltipVBox.getChildren().clear();
        for (Map.Entry<String, Integer> entry : job.cost().entrySet()) {
            if (entry.getValue() > 0) {
                HBox hBox = new HBox(); // create HBox for each resource
                hBox.setSpacing(10);
                ImageView resourceImage = new ImageView();
                resourceImage.setImage(imageCache.get("image/game_resources/" + entry.getKey() + ".png"));
                resourceImage.setFitHeight(30);
                resourceImage.setFitWidth(30);
                hBox.getChildren().add(resourceImage);
                Label resourceAmount = new Label();
                resourceAmount.getStyleClass().add("small-large");
                resourceAmount.setText("+" + entry.getValue());
                hBox.getChildren().add(resourceAmount);
                tooltipVBox.getChildren().add(hBox);
            }
        }
    }

    private void taskInQueue() {
        progressHBox.setVisible(false);
        inQueueLabel.setVisible(true);
        inQueueLabel.setText(bundle.getString("in.queue"));
        inQueueLabel.setStyle("-fx-text-fill: #D3D3D3;");
        taskName.setStyle("-fx-text-fill: #D3D3D3;");
        taskType.setStyle("-fx-text-fill: #D3D3D3;");
        systemName.setStyle("-fx-text-fill: #D3D3D3;");
    }

    private void taskInProgress() {
        progressHBox.setVisible(true);
        inQueueLabel.setVisible(false);
        taskName.setStyle("-fx-text-fill: #FF8000;");
        taskType.setStyle("-fx-text-fill: #FF8000;");
        systemName.setStyle("-fx-text-fill: #FF8000;");
    }

    private void whenTypeUpgrade(Job job) {
        final GameSystem systemWithName = this.systems.stream().filter(gameSystem -> gameSystem._id().equals(job.system())).findFirst().orElse(null);
        if (systemWithName == null) {
            return;
        }
        systemName.setText("");
        String currentUpgradeState = systemWithName.upgrade();
        String nextUpgradeState = nextSystemUpgrade.get(currentUpgradeState);
        switch (nextUpgradeState) {
            case "explored" -> taskName.setText(bundle.getString("explore") + " " + systemWithName.name());
            case "colonized" -> taskName.setText(bundle.getString("colonize") + " " + systemWithName.name());
            case "upgraded" -> taskName.setText(bundle.getString("upgrade") + " " + systemWithName.name());
            case "developed" -> taskName.setText(bundle.getString("develop") + " " + systemWithName.name());
        }
    }

    private void whenTypeBuilding(Job job) {
        taskName.setText(bundle.getString("building." + job.building()));
        final GameSystem systemWithName = this.systems.stream().filter(gameSystem -> gameSystem._id().equals(job.system())).findFirst().orElse(null);
        if (systemWithName == null) {
            return;
        }
        systemName.setText(systemWithName.name());
    }

    private void whenTypeDistrict(Job job) {
        taskName.setText(bundle.getString("districts.name." + job.district()));
        final GameSystem systemWithName = this.systems.stream().filter(gameSystem -> gameSystem._id().equals(job.system())).findFirst().orElse(null);
        if (systemWithName == null) {
            return;
        }
        systemName.setText(systemWithName.name());
    }

    private void whenTypeTechnology(Job job) {
        systemName.setText("");
        taskName.setText(bundle.getString(job.technology()));
    }

    private void whenTypeShip(Job job) {
        taskName.setText(bundle.getString(job.ship()));
        if (this.systems == null) {
            return;
        }
        final GameSystem systemWithName = this.systems.stream().filter(gameSystem -> gameSystem._id().equals(job.system())).findFirst().orElse(null);
        if (systemWithName == null) {
            return;
        }
        systemName.setText(systemWithName.name());
    }

    private void whenTypeTravel(Job job) {
        final String destinationId = job.path().getLast();
        final String destinationName = systems.stream().filter(s -> s._id().equals(destinationId)).findFirst().map(GameSystem::name).orElse("Unknown");
        taskName.setText(destinationName);
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }
}
