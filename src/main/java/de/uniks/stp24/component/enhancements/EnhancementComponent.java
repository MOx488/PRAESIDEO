package de.uniks.stp24.component.enhancements;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Job;
import de.uniks.stp24.model.Technology;
import de.uniks.stp24.service.EnhancementService;
import de.uniks.stp24.service.JobService;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ListView;
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

@Component(view = "Enhancement.fxml")
public class EnhancementComponent extends HBox {
    @FXML
    HBox enhancementBox;
    @FXML
    VBox enhancementParentBox;
    @FXML
    ListView<EnhancementItemComponent> itemListView;

    @Inject
    public Provider<EnhancementItemComponent> enhancementItemComponentProvider;
    @Inject
    public App app;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public Subscriber subscriber;
    @Inject
    public JobService jobService;
    @Inject
    public EventListener eventListener;
    @Inject
    public EnhancementService enhancementService;

    @Param("enhancementSubject")
    String enhancementSubject;
    @Param("empire")
    Empire empire;
    @Param("enhancement")
    Technology enhancement;
    @Param("game")
    Game game;
    @Param("jobs")
    ObservableList<Job> jobs;
    @Param("allEnhancements")
    List<Technology> allEnhancements;

    private final ObservableList<Technology> enhancements = FXCollections.observableArrayList();
    private ListChangeListener<Node> enhancementBoxListener;

    @Inject
    public EnhancementComponent() {
    }

    @OnInit
    public void init() {
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".updated", Game.class), event -> this.game = event.data());

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), empireEvent -> this.empire = empireEvent.data());

        this.jobs = jobService.init(empire, game);

        enhancementService.initializeService(this::onNewScientist);
    }

    public Technology getTech(Job data) {
        // return Tech to get the Tags for the Job
        return allEnhancements.stream().filter(tech -> tech.id().equals(data.technology())).findFirst().orElse(null);
    }

    @OnRender
    public void render() {
        EnhancementItemComponent physics = app.initAndRender(enhancementItemComponentProvider.get(), Map.of("enhancementSubject", "physics", "jobs", jobs, "allEnhancements", allEnhancements, "enhancements", enhancements, "empire", empire, "game", game, "box", enhancementBox, "enhancementComponent", this), subscriber);
        itemListView.getItems().add(physics);
        EnhancementItemComponent society = app.initAndRender(enhancementItemComponentProvider.get(), Map.of("enhancementSubject", "society", "jobs", jobs, "allEnhancements", allEnhancements, "enhancements", enhancements, "empire", empire, "game", game, "box", enhancementBox, "enhancementComponent", this), subscriber);
        itemListView.getItems().add(society);
        EnhancementItemComponent craftsmanship = app.initAndRender(enhancementItemComponentProvider.get(), Map.of("enhancementSubject", "engineering", "jobs", jobs, "allEnhancements", allEnhancements, "enhancements", enhancements, "empire", empire, "game", game, "box", enhancementBox, "enhancementComponent", this), subscriber);
        itemListView.getItems().add(craftsmanship);

        this.enhancementService.initializeScientistHandling(game, empire);

        // check if Buttons to open list should be visible when enhancementBox size changes
        enhancementBox.getChildren().addListener(enhancementBoxListener = change -> {
            while (change.next()) {
                loadButtons();
            }
        });
    }

    private void onNewScientist(String scientistType, Integer imageIndex) {
        switch (scientistType) {
            case "physics" -> itemListView.getItems().getFirst().loadImage(scientistType, imageIndex);
            case "society" -> itemListView.getItems().get(1).loadImage(scientistType, imageIndex);
            case "engineering" -> itemListView.getItems().getLast().loadImage(scientistType, imageIndex);
        }
    }

    public void loadButtons() {
        if (enhancementBox != null && (enhancementBox.getChildren().size() > 1 && enhancementBox.getChildren().get(1) instanceof EnhancementListComponent)) {
            hideViewEnhancementsButton();
        } else {
            for (EnhancementItemComponent item : itemListView.getItems()) {
                item.addButton();
            }
            enhancementParentBox.setMinWidth(710);
            enhancementParentBox.setMaxWidth(710);
        }
    }

    public void hideViewEnhancementsButton() {
        for (EnhancementItemComponent item : itemListView.getItems()) {
            item.removeButton();
        }
        enhancementParentBox.setMinWidth(540);
        enhancementParentBox.setMaxWidth(540);
    }

    public void checkForEnhancementSelectedComponent() {
        // check if another component is already open and close it
        if (enhancementBox.getChildren().size() > 1) {
            enhancementBox.getChildren().remove(1);
        }
    }

    public void deselectBoxes() {
        for (EnhancementItemComponent item : itemListView.getItems()) {
            item.removeStyle();
        }
    }

    @OnDestroy
    public void onDestroy() {
        if (enhancementBoxListener != null) {
            enhancementBox.getChildren().removeListener(enhancementBoxListener);
        }
        subscriber.dispose();
    }
}