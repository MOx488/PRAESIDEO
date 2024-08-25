package de.uniks.stp24.component.war;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Player;
import de.uniks.stp24.model.War;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.WarsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.listview.ComponentListCell;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "Diplomacy.fxml")
public class DiplomacyComponent extends HBox {

    @FXML
    Pane diplomacyRoot;
    @FXML
    ChoiceBox<String> diplomacyFilter;
    @FXML
    StackPane listContainer;
    @FXML
    ImageView warImage;
    @FXML
    Text diplomacyTitle;
    @FXML
    VBox reasonBox;
    @FXML
    Label reasonName;
    @FXML
    ImageView reasonImage;
    @FXML
    ImageView unSeeReasonImage;

    @Inject
    public App app;
    @Inject
    public ImageCache imageCache;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public WarsApiService warsApiService;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;

    @Inject
    public Provider<PeaceComponent> peaceComponentProvider;
    @Inject
    public Provider<WarDefendsComponent> warDefendsComponentProvider;
    @Inject
    public Provider<WarAttacksComponent> warAttacksComponentProvider;
    @Inject
    public Provider<WarNotificationComponent> warNotificationComponentProvider;

    @Param("empire")
    Empire empire;

    @Param("game")
    Game game;

    @Param("parent")
    AnchorPane parentContainer;

    @Param("players")
    ObservableList<Player> players;

    private War previousWar;

    private final ObservableList<War> warsAttack = FXCollections.observableArrayList();
    private final ObservableList<War> warsDefend = FXCollections.observableArrayList();
    private ObservableList<ReadEmpireDto> empires = FXCollections.observableArrayList();
    private final ObservableList<ReadEmpireDto> empiresFilteredByPeace = FXCollections.observableArrayList();

    private final ListView<War> warAttacksList = new ListView<>();
    private final ListView<War> warDefendsList = new ListView<>();
    private final ListView<ReadEmpireDto> empirePeaceList = new ListView<>();

    private Button lastClickedReasonButton;
    private War lastSelectedWar;

    private ChangeListener<String> diplomacyFilterListener;

    @Inject
    public DiplomacyComponent() {

    }

    @OnInit
    public void onInit() {
        subscriber.subscribe(warsApiService.getWars(game._id(), empire._id()), wars -> {
            warsAttack.clear();
            warsDefend.clear();
            for (War war : wars) {
                if (war.attacker().equals(empire._id())) {
                    warsAttack.add(war);
                } else {
                    warsDefend.add(war);
                }
            }

            subscriber.subscribe(gameEmpiresApiService.getEmpires(game._id()), readEmpireDtos -> {
                empires = FXCollections.observableArrayList(readEmpireDtos);
                for (ReadEmpireDto readEmpireDto : empires) {
                    if (!readEmpireDto._id().equals(empire._id())) {
                        if (!isInWar(readEmpireDto)) {
                            empiresFilteredByPeace.add(readEmpireDto);
                        }
                    }
                }

                render();
            });
        });


        subscriber.subscribe(eventListener.listen("games." + game._id() + ".wars.*.*", War.class), warEvent -> {

            if(!warEvent.suffix().equals("deleted") && warEvent.data().equals(previousWar)) {
                return;
            }

            previousWar = warEvent.data();

            switch (warEvent.suffix()) {
                case "created" -> {
                    War war = warEvent.data();
                    if (war.attacker().equals(empire._id())) {
                        warsAttack.add(war);
                        removePeaceEmpire(war.defender());
                    } else if (war.defender().equals(empire._id())) {
                        warsDefend.add(war);
                        removePeaceEmpire(war.attacker());
                        parentContainer.getChildren().add(app.initAndRender(warNotificationComponentProvider.get(), Map.of("war", war, "empires", empires, "players", players)));
                    }
                }
                case "deleted" -> {
                    War war = warEvent.data();
                    if (war.attacker().equals(empire._id())) {
                        warsAttack.remove(war);
                        addPeaceEmpire(war.defender());
                    } else if (war.defender().equals(empire._id())) {
                        if (lastSelectedWar != null && lastSelectedWar._id().equals(war._id())) {
                            unSeeReason();
                        }
                        warsDefend.remove(war);
                        addPeaceEmpire(war.attacker());
                    }
                }
            }
        });
    }

    private void addPeaceEmpire(String enemyId) {
        for (ReadEmpireDto readEmpireDto : empires) {
            if (readEmpireDto._id().equals(enemyId)) {
                if (empiresFilteredByPeace.contains(readEmpireDto)) {
                    return;
                }
                empiresFilteredByPeace.add(readEmpireDto);
                return;
            }
        }
    }

    private void removePeaceEmpire(String enemyId) {
        for (ReadEmpireDto readEmpireDto : empires) {
            if (readEmpireDto._id().equals(enemyId)) {
                empiresFilteredByPeace.remove(readEmpireDto);
                return;
            }
        }
    }

    private boolean isInWar(ReadEmpireDto readEmpireDto) {
        for (War war : warsAttack) {
            if (war.defender().equals(readEmpireDto._id())) {
                return true;
            }
        }
        for (War war : warsDefend) {
            if (war.attacker().equals(readEmpireDto._id())) {
                return true;
            }
        }
        return false;
    }

    public void render() {
        diplomacyTitle.setText(bundle.getString("diplomacy"));

        warAttacksList.getStyleClass().add("container");
        warAttacksList.getStyleClass().add("light-container");
        warDefendsList.getStyleClass().add("container");
        warDefendsList.getStyleClass().add("light-container");
        empirePeaceList.getStyleClass().add("container");
        empirePeaceList.getStyleClass().add("light-container");


        unSeeReasonImage.setImage(imageCache.get("image/circle-xmark-regular.png"));
        reasonBox.setVisible(false);
        if (lastClickedReasonButton != null) {
            lastClickedReasonButton.setStyle("");
        }
        lastClickedReasonButton = null;


        // peace
        empirePeaceList.setItems(empiresFilteredByPeace);
        empirePeaceList.setCellFactory(list -> new ComponentListCell<>(app, peaceComponentProvider, Map.of("empire", empire, "game", game, "parent", parentContainer, "diplomacyRoot", diplomacyRoot, "players", players)));

        // war defend
        warDefendsList.setItems(warsDefend);
        warDefendsList.setCellFactory(list -> new ComponentListCell<>(app, warDefendsComponentProvider, Map.of("empires", empires, "diplomacyRoot", diplomacyRoot, "players", players)));

        // war attack
        warAttacksList.setItems(warsAttack);
        warAttacksList.setCellFactory(list -> new ComponentListCell<>(app, warAttacksComponentProvider, Map.of("game", game, "empires", empires, "diplomacyRoot", diplomacyRoot, "players", players)));

        populateChoiceBox();
    }

    private void populateChoiceBox() {
        diplomacyFilter.getItems().clear();
        diplomacyFilter.getItems().add(bundle.getString("diplomacy.filter.attack"));
        diplomacyFilter.getItems().add(bundle.getString("diplomacy.filter.defend"));
        diplomacyFilter.getItems().add(bundle.getString("diplomacy.filter.peace"));
        diplomacyFilter.getSelectionModel().selectFirst();

        listContainer.getChildren().clear();
        listContainer.getChildren().add(warAttacksList);

        setDiplomacyImage("attack");

        diplomacyFilter.getSelectionModel().selectedItemProperty().addListener(diplomacyFilterListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            listContainer.getChildren().clear();
            if (newValue.equals(bundle.getString("diplomacy.filter.attack"))) {
                unSeeReason();
                listContainer.getChildren().add(warAttacksList);
                setDiplomacyImage("attack");
            } else if (newValue.equals(bundle.getString("diplomacy.filter.defend"))) {
                unSeeReason();
                listContainer.getChildren().add(warDefendsList);
                setDiplomacyImage("defend");
            } else { // peace
                unSeeReason();
                listContainer.getChildren().add(empirePeaceList);
                setDiplomacyImage("peace");
            }
        });
    }

    private void setDiplomacyImage(String choiceDiplomacy) {
        warImage.setImage(imageCache.get("image/icons/" + choiceDiplomacy + ".png"));
    }

    public void setLastClickedReasonButton(Button lastClickedReasonButton, War war) {
        if (this.lastClickedReasonButton != null) {
            this.lastClickedReasonButton.setStyle("");
        }
        this.lastClickedReasonButton = lastClickedReasonButton;
        this.lastSelectedWar = war;
    }

    public void seeReason(War war) {
        lastClickedReasonButton.setStyle("-fx-border-color: yellow; -fx-border-width: 2px;");
        Map<String, Object> params = war._public();
        if (params == null) {
            reasonName.setText(bundle.getString("noReason"));
            return;
        }
        String reason = (String) params.get("reason");
        reasonName.setText(bundle.getString(reason));
        reasonImage.setImage(imageCache.get("image/warreasons/" + reason + ".png"));
        reasonBox.setVisible(true);
    }

    public void unSeeReason() {
        if (lastClickedReasonButton != null) {
            lastClickedReasonButton.setStyle("");
        }
        reasonBox.setVisible(false);
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
        if (diplomacyFilterListener != null) {
            diplomacyFilter.getSelectionModel().selectedItemProperty().removeListener(diplomacyFilterListener);
        }
    }
}
