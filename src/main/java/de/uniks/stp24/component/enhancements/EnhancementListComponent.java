package de.uniks.stp24.component.enhancements;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Job;
import de.uniks.stp24.model.Technology;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.listview.ComponentListCell;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "EnhancementList.fxml")
public class EnhancementListComponent extends HBox {
    @FXML
    HBox enhancementListBox;
    @FXML
    Label enhancementsLabel;
    @FXML
    ImageView listCross;
    @FXML
    ListView<Technology> enhancementsList;
    @FXML
    CheckBox checkBoxCompletedEnhancements;

    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;
    @Inject
    public App app;
    @Inject
    public JobsApiService jobsApiService;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;

    @Inject
    public Provider<EnhancementBoxComponent> enhancementBoxComponentProvider;
    @Inject
    public Provider<EnhancementSelectedComponent> enhancementSelectedComponentProvider;

    private HBox parentContainer;

    @Param("enhancementSubject")
    String enhancementSubject;
    @Param("enhancements")
    ObservableList<Technology> enhancements;
    @Param("allEnhancements")
    List<Technology> allEnhancements;
    @Param("enhancement")
    Technology enhancement;
    @Param("empire")
    Empire empire;
    @Param("game")
    Game game;
    @Param("jobs")
    ObservableList<Job> jobs;
    @Param("enhancementComponent")
    EnhancementComponent enhancementComponent;

    private final List<Technology> allTechs = FXCollections.observableArrayList();
    private final List<Technology> compTechs = FXCollections.observableArrayList();
    private ChangeListener<Technology> selectedBoxListener;

    @Inject
    public EnhancementListComponent() {
    }

    @OnRender
    public void render() {
        allTechs.addAll(enhancements);
        compTechs.addAll(enhancements);
        loadList();
        enhancementsList.setCellFactory(param -> new ComponentListCell<>(app, enhancementBoxComponentProvider, Map.of("enhancementSubject", enhancementSubject)));
        selectedCheckBox();

        loadTitle();
        this.listCross.setImage(imageCache.get("image/circle-xmark-regular.png"));

        selectedBox();

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".jobs.*.*", Job.class), event -> {
            if (event.data().technology() != null) {
                switch (event.suffix()) {
                    case "created", "updated" ->
                            allTechs.stream().filter(technology -> technology.id().equals(event.data().technology())).findFirst().ifPresent(allTechs::remove);
                    case "deleted" -> {
                        if (allTechs.stream().noneMatch(technology -> technology.id().equals(event.data().technology()))) {
                            ObservableList<Technology> findIndex = FXCollections.observableArrayList();
                            findIndex.addAll(enhancements);
                            findIndex.removeIf(technology -> empire.technologies().contains(technology.id()));
                            Technology removedTech = allEnhancements.stream().filter(technology -> technology.id().equals(event.data().technology())).findFirst().orElse(null);
                            if (removedTech != null && findIndex.contains(removedTech)) {
                                int i = findIndex.indexOf(removedTech);
                                enhancements.stream().filter(technology -> technology.id().equals(event.data().technology())).findFirst().ifPresent(tech -> allTechs.add(i, tech));
                            } else {
                                enhancements.stream().filter(technology -> technology.id().equals(event.data().technology())).findFirst().ifPresent(allTechs::addFirst);
                            }
                        }
                    }
                }
            }
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".empires." + empire._id() + ".updated", Empire.class), event -> {
            this.empire = event.data();
            for (String techId : empire.technologies()) {
                Technology tech = enhancements.stream().filter(technology -> technology.id().equals(techId)).findFirst().orElse(null);
                if (tech != null && compTechs.stream().noneMatch(technology -> technology.id().equals(techId))) {
                    compTechs.add(tech);
                }
            }
            allTechs.removeIf(technology -> empire.technologies().contains(technology.id()));
        });
    }

    private void loadTitle() {
        switch (enhancementSubject) {
            case "physics":
                enhancementsLabel.setText(bundle.getString("physics.enhancements"));
                enhancementsLabel.getStyleClass().add("physics");
                break;
            case "society":
                enhancementsLabel.setText(bundle.getString("society.enhancements"));
                enhancementsLabel.getStyleClass().add("society");
                break;
            case "engineering":
                enhancementsLabel.setText(bundle.getString("craftsmanship.enhancements"));
                enhancementsLabel.getStyleClass().add("craftsmanship");
                break;
        }
    }

    public void selectedCheckBox() {
        // if check Box gets clicked
        if (enhancementListBox.getChildren().size() > 1) {
            enhancementListBox.getChildren().remove(1);
        }
        if (checkBoxCompletedEnhancements.isSelected()) {
            loadCompletedList();
        } else {
            loadList();
        }
    }

    private void loadCompletedList() {
        // Load completed enhancements for the selected category into the List
        compTechs.removeIf(technology -> !empire.technologies().contains(technology.id()));
        if (!new HashSet<>(enhancementsList.getItems()).containsAll(compTechs)) {
            enhancementsList.setItems((ObservableList<Technology>) compTechs);
        }
    }

    private void loadList() {
        // load the enhancements in the List for the selected subject
        allTechs.removeIf(technology -> empire.technologies().contains(technology.id()));
        allTechs.removeIf(technology -> jobs.stream().anyMatch(job -> {
            if (job.technology() == null) {
                return false;
            }
            return job.technology().contains(technology.id());
        }));
        if (!new HashSet<>(enhancementsList.getItems()).containsAll(allTechs)) {
            enhancementsList.setItems((ObservableList<Technology>) allTechs);
        }
    }

    private void selectedBox() {
        enhancementsList.getSelectionModel().selectedItemProperty().addListener(selectedBoxListener = (observable, oldValue, newValue) -> {
            if (oldValue != null) {
                if (enhancementListBox.getChildren().size() > 1) {
                    enhancementListBox.getChildren().remove(1);
                }
            }
            if (newValue != null) {
                this.enhancement = newValue;

                EnhancementSelectedComponent enhancementSelectedComponent = app.initAndRender(
                        enhancementSelectedComponentProvider.get(),
                        Map.of("enhancement", enhancement, "allEnhancements", allEnhancements, "empire", empire, "game", game),
                        subscriber
                );
                this.enhancementListBox.getChildren().add(enhancementSelectedComponent);
                enhancementSelectedComponent.setParentContainer(enhancementListBox);
                enhancementSelectedComponent.setParentList(enhancementsList);
            }
        });
    }

    public void onCrossClicked() {
        if (parentContainer != null) {
            parentContainer.getChildren().remove(this);
            enhancementComponent.loadButtons();
        }
    }

    public void setParentContainer(HBox parentContainer) {
        this.parentContainer = parentContainer;
    }

    @OnDestroy
    public void onDestroy() {
        if (selectedBoxListener != null) {
            enhancementsList.getSelectionModel().selectedItemProperty().removeListener(selectedBoxListener);
        }
        subscriber.dispose();
    }
}
