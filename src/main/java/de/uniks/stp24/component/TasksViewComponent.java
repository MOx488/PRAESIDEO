package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.model.Job;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.JobService;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
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
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.initListView;
import static de.uniks.stp24.util.Methods.updateOurCastles;

@Component(view = "TasksViewComponent.fxml")
@Singleton
public class TasksViewComponent extends Pane {
    @FXML
    Pane taskRoot;
    @FXML
    Text taskstitle;
    @FXML
    ListView<Job> taskList;
    @FXML
    ChoiceBox<String> taskFilter;

    @Inject
    public ImageCache imageCache;
    @Inject
    public Provider<TaskComponent> taskComponentProvider;
    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public JobService jobService;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public NotificationService notificationService;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;

    @Param("empire")
    Empire empire;

    @Param("game")
    Game game;
    @Param("systems")
    ObservableList<GameSystem> systems;


    private ObservableList<GameSystem> ourCastles = FXCollections.observableArrayList();
    private ObservableList<Job> jobs = FXCollections.observableArrayList();
    private final ObservableList<Job> filteredJobs = FXCollections.observableArrayList();
    private final HashMap<String, Boolean> jobsDone = new HashMap<>();

    private ChangeListener<String> taskFilterListener;

    private GameSystem previousWebsocketSystem;

    @Inject
    public TasksViewComponent() {

    }

    @OnInit
    public void onInit() {
        ourCastles = FXCollections.observableArrayList(systems);
        ourCastles.removeIf(system -> system.owner() == null || !system.owner().equals(empire._id()));

        this.jobs = jobService.init(empire, game);

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".updated", Game.class), event -> this.game = event.data());

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".systems.*.updated", GameSystem.class),
                event -> {
                    final GameSystem newSystem = event.data();
                    updateOurCastles(previousWebsocketSystem, newSystem, empire, ourCastles, systems, this::populateChoiceBox);
                    this.previousWebsocketSystem = newSystem;
                }
        );

        subscriber.subscribe(jobService.listenForJobEvent("games." + game._id() + ".empires." + empire._id() + ".jobs.*.*"),
                this::onJobEvent
        );
    }

    @OnRender
    public void onRender() {
        taskstitle.setText(bundle.getString("tasks.title"));
        populateChoiceBox();

        initListView(taskList, jobs, app, taskComponentProvider, Map.of("game", game, "systems", systems));
    }

    private void onJobEvent(Event<Job> event) {
        final Job job = event.data();

        switch (event.suffix()) {
            case "created" -> filteredJobs.add(job);
            case "updated" -> {
                filteredJobs.replaceAll(u -> u._id().equals(job._id()) ? job : u);

                final boolean taskNotDone = job.result() == null
                        || !job.result().containsValue(200)
                        || jobsDone.get(job._id()) != null;
                if (taskNotDone) {
                    return;
                }

                // Show notification if the task is done
                jobsDone.put(job._id(), true);
                final Label taskLabel = (Label) app.stage().getScene().lookup("#taskName");
                if (taskLabel != null) {
                    this.notificationService.displayNotification(bundle.getString("task") + " " + "\"" + taskLabel.getText() + "\"" + " " + bundle.getString("task.done"), true);
                } else {
                    this.notificationService.displayNotification(bundle.getString("task.done"), true);
                }
            }
            case "deleted" -> {
                jobs.removeIf(u -> u._id().equals(job._id()));
                filteredJobs.removeIf(u -> u._id().equals(job._id()));
            }
        }
    }

    private void populateChoiceBox() {
        taskFilter.getItems().clear();// Clear existing items
        taskFilter.getItems().add(bundle.getString("all.owned.castles"));
        for (GameSystem system : ourCastles) {
            taskFilter.getItems().add(system.name()); // Add system names to the ChoiceBox
        }
        taskFilter.getSelectionModel().selectFirst(); // Select the first system by default
        taskFilter.getSelectionModel().selectedItemProperty().addListener(taskFilterListener = (obs, oldSelection, newSelection) -> {
            if (newSelection == null) return;

            updateTaskList(newSelection); // Update the task list based on the selected system
        });
    }

    private void updateTaskList(String selectedSystemName) {
        // Clear the ListView
        if (selectedSystemName.equals(bundle.getString("all.owned.castles"))) {
            taskList.setItems(jobs); // Show all jobs if "All Owned Castles" is selected
        } else {
            filteredJobs.clear(); // Clear the filtered jobs list
            for (Job job : jobs) {
                if (job.system() != null && job.system().equals(findSystemIdByName(selectedSystemName))) {
                    filteredJobs.add(job);
                }
            }
            taskList.setItems(filteredJobs); // Update the ListView with filtered jobs
        }
    }

    private String findSystemIdByName(String systemName) {
        for (GameSystem system : ourCastles) {
            if (system.name().equals(systemName)) {
                return system._id(); // Return the ID of the system matching the name
            }
        }
        return null; // Return null if no matching system is found
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
        if (taskFilterListener != null) {
            taskFilter.getSelectionModel().selectedItemProperty().removeListener(taskFilterListener);
        }
    }
}
