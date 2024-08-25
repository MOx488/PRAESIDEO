package de.uniks.stp24.component.enhancements;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Job;
import de.uniks.stp24.model.Technology;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.JobService;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "EnhancementItemComponent.fxml")
public class EnhancementItemComponent extends HBox {
    @FXML
    Label itemTag;
    @FXML
    VBox scientistsBoarder;
    @FXML
    ImageView scientistsImage;
    @FXML
    Label scientistsName;
    @FXML
    HBox itemBox;
    @FXML
    public Label itemNoJob;
    @FXML
    public HBox itemClickedBox;
    @FXML
    public Label itemTechnologie;
    @FXML
    public ProgressBar itemProgressBar;
    @FXML
    public Label itemDate;
    @FXML
    public ImageView itemCross;
    @FXML
    public Button itemViewEnhancementsButton;

    @Inject
    public App app;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;
    @Inject
    public Subscriber subscriber;
    @Inject
    public JobService jobService;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public EventListener eventListener;

    @Param("jobs")
    ObservableList<Job> jobs;
    @Param("allEnhancements")
    List<Technology> allEnhancements;
    @Param("empire")
    Empire empire;
    @Param("enhancements")
    ObservableList<Technology> enhancements;
    @Param("game")
    Game game;
    @Param("box")
    HBox enhancementBox;
    @Param("enhancementSubject")
    String enhancementSubject;
    @Param("enhancementComponent")
    EnhancementComponent enhancementComponent;

    @Inject
    public Provider<EnhancementSelectedComponent> enhancementSelectedComponentProvider;
    @Inject
    public Provider<EnhancementListComponent> enhancementListComponentProvider;

    private Technology itemEnhancement;

    @Inject
    public EnhancementItemComponent() {
    }

    @OnInit
    public void onInit() {
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), empireEvent -> this.empire = empireEvent.data());
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".jobs.*.*", Job.class), this::handleJobEvent);
    }

    private void handleJobEvent(Event<Job> event) {
        switch (event.suffix()) {
            case "created" -> {
                if (jobs.stream().noneMatch(j -> j._id().equals(event.data()._id()))) {
                    jobs.add(event.data());
                }
            }
            case "updated" -> {
                jobs.removeIf(j -> j._id().equals(event.data()._id()));
                jobs.add(event.data());
            }
            case "deleted" -> {
                jobs.removeIf(j -> j._id().equals(event.data()._id()));
                Technology tech = enhancementComponent.getTech(event.data());
                if (tech != null && tech.tags().contains(enhancementSubject)) resetEnhancement();
            }
        }
        // refresh the shown enhancements
        loadEnhancement(jobs);
    }

    @OnRender
    public void onRender() {
        itemCross.setImage(imageCache.get("image/cross_red.png"));
        scientistsBoarder.getStyleClass().add(bundle.getString("tag." + enhancementSubject).toLowerCase().replace(" ", ""));
        itemTag.getStyleClass().add(bundle.getString("tag." + enhancementSubject).toLowerCase().replace(" ", ""));
        itemTag.setText(bundle.getString("tag." + enhancementSubject));
        loadEnhancement(jobs);
    }

    public void loadEnhancement(ObservableList<Job> jobs) {
        for (Job job : jobs) {
            for (Technology tech : allEnhancements) {
                if (tech.id().equals(job.technology()) && tech.tags().contains(enhancementSubject)) {
                    itemEnhancement = tech;
                    loadCurrentEnhancement(job);
                    return;
                }
            }
        }
        this.loadEmptyEnhancement();
    }

    private void loadEmptyEnhancement() {
        if (itemEnhancement == null) {
            itemCross.setVisible(false);
            itemDate.setVisible(false);
            itemProgressBar.setVisible(false);
            itemTechnologie.setVisible(false);
            itemNoJob.setVisible(true);
        }
    }

    private void loadCurrentEnhancement(Job job) {
        itemNoJob.setVisible(false);
        itemCross.setVisible(true);
        itemDate.setVisible(true);
        itemProgressBar.setVisible(true);
        itemTechnologie.setVisible(true);
        itemTechnologie.setText(bundle.getString(itemEnhancement.id()));

        itemProgressBar.setProgress((double) job.progress() / job.total());

        itemDate.setText(jobService.getJobEndDate(job));
    }

    public void resetEnhancement() {
        // delete old enhancement
        if (itemEnhancement != null) {
            itemEnhancement = null;
            itemClickedBox.getStyleClass().remove("enhancement-selected");
            enhancementComponent.deselectBoxes();
            if (enhancementBox.getChildren().size() > 1 && enhancementBox.getChildren().get(1) instanceof EnhancementSelectedComponent) {
                enhancementBox.getChildren().remove(1);
            }
        }
    }

    public void openList() {
        // deselect enhancements in progress and close all existing components
        enhancementComponent.deselectBoxes();
        enhancementComponent.checkForEnhancementSelectedComponent();

        // initialize selected List
        if (!enhancements.isEmpty()) enhancements.clear();
        enhancements.addAll(
                allEnhancements.stream()
                        .filter(technology -> technology.tags().contains(enhancementSubject))
                        .toList()
        );
        EnhancementListComponent enhancementListComponent = app.initAndRender(enhancementListComponentProvider.get(),
                Map.of("enhancementComponent", enhancementComponent, "enhancementSubject", enhancementSubject, "enhancements", enhancements, "allEnhancements", allEnhancements, "empire", empire, "game", game, "jobs", jobs), subscriber);
        this.enhancementBox.getChildren().add(enhancementListComponent);
        enhancementListComponent.setParentContainer(enhancementBox);
    }

    public void showTask() {
        if (itemEnhancement != null) {
            // deselect all other boxes and change selected Box Boarder to yellow
            enhancementComponent.deselectBoxes();
            enhancementComponent.checkForEnhancementSelectedComponent();
            itemClickedBox.getStyleClass().add("enhancement-selected");

            // initialize selected Enhancement Component
            EnhancementSelectedComponent enhancementSelectedComponent = app.initAndRender(enhancementSelectedComponentProvider.get(),
                    Map.of("enhancementComponent", enhancementComponent, "enhancement", itemEnhancement, "allEnhancements", allEnhancements, "empire", empire, "game", game), subscriber);
            this.enhancementBox.getChildren().add(enhancementSelectedComponent);
            enhancementSelectedComponent.setParentContainer(enhancementBox);
        } else {
            // if no enhancement is selected, open the list
            openList();
        }
    }

    public void clickCross() {
        Job job = getJob(enhancementSubject);
        subscriber.subscribe(jobsApiService.deleteJob(game._id(), empire._id(), job._id()));
    }

    public Job getJob(String tag) {
        return jobs.stream()
                .filter(job -> allEnhancements.stream()
                        .anyMatch(tech -> tech.id().equals(job.technology()) && tech.tags().contains(tag)))
                .findFirst()
                .orElse(null);
    }

    public void addButton() {
        if (itemBox != null && itemBox.getChildren().size() < 2) {
            itemBox.getChildren().add(itemViewEnhancementsButton);
        }
    }

    public void removeButton() {
        itemBox.getChildren().remove(itemViewEnhancementsButton);
    }

    public void removeStyle() {
        itemClickedBox.getStyleClass().remove("enhancement-selected");
    }

    public void loadImage(String scientistType, Integer imageIndex) {
        scientistsImage.setImage(imageCache.get("image/scientists/" + scientistType + "_" + imageIndex + ".jpg"));
        scientistsName.setText(bundle.getString(scientistType + "_" + imageIndex));
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }
}
