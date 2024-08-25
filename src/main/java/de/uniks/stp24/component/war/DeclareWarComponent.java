package de.uniks.stp24.component.war;


import de.uniks.stp24.App;
import de.uniks.stp24.dto.CreateWarDto;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Player;
import de.uniks.stp24.rest.WarsApiService;
import de.uniks.stp24.service.ImageCache;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "DeclareWar.fxml")
public class DeclareWarComponent extends AnchorPane {

    @FXML
    AnchorPane declareWarRoot;
    @FXML
    VBox empireBox;
    @FXML
    ImageView empireImage;
    @FXML
    Button declareWarButton1;
    @FXML
    TextField warNameTextField;
    @FXML
    ImageView enemyFlag;
    @FXML
    ImageView ownFlag;
    @FXML
    Label youLabel;
    @FXML
    Label enemyName;
    @FXML
    Button backButton;
    @FXML
    Label declareWarTitle;

    @FXML
    VBox enemyBox;
    @FXML
    ImageView enemyImage;

    @FXML
    TabPane warReasonTabPane;
    @FXML
    Tab plunderTab;
    @FXML
    Tab conquestTab;
    @FXML
    Tab vengeanceTab;
    @FXML
    Tab funTab;

    @FXML
    ImageView plunderImage;
    @FXML
    ImageView conquestImage;
    @FXML
    ImageView vengeanceImage;
    @FXML
    ImageView funImage;

    @FXML
    Label attackerLabel;
    @FXML
    Label defenderLabel;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public WarsApiService warsApiService;

    @Param("enemy")
    ReadEmpireDto enemy;
    @Param("empire")
    Empire empire;
    @Param("game")
    Game game;
    @Param("diplomacyRoot")
    Pane diplomacyRoot;
    @Param("players")
    ObservableList<Player> players;

    private Map<Tab, String> warReasons;

    @Inject
    public DeclareWarComponent() {
    }

    @OnInit
    public void onInit() {
        AnchorPane.setTopAnchor(this, 100.0);
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
    }

    @OnRender
    public void onRender() {
        warReasons = Map.of(
                plunderTab, "plunder",
                conquestTab, "conquest",
                vengeanceTab, "vengeance",
                funTab, "fun"
        );

        declareWarTitle.setText(bundle.getString("declare.war"));
        declareWarButton1.setText(bundle.getString("declare.war"));
        backButton.setText(bundle.getString("back"));

        youLabel.setText(bundle.getString("you"));
        enemyName.setText(enemy.name());

        plunderTab.setText(bundle.getString("plunder"));
        conquestTab.setText(bundle.getString("conquest"));
        vengeanceTab.setText(bundle.getString("vengeance"));
        funTab.setText(bundle.getString("fun"));

        plunderImage.setImage(imageCache.get("image/warreasons/plunder.png"));
        conquestImage.setImage(imageCache.get("image/warreasons/conquest.png"));
        vengeanceImage.setImage(imageCache.get("image/warreasons/vengeance.png"));
        funImage.setImage(imageCache.get("image/warreasons/fun.png"));

        defenderLabel.setText(bundle.getString("defender"));
        attackerLabel.setText(bundle.getString("attacker"));

        // set empire
        empireImage.setImage(imageCache.get("image/portraits/" + empire.portrait() + ".png"));
        ownFlag.setImage(imageCache.get("image/flags/" + empire.flag() + ".png"));
        empireBox.setStyle("-fx-effect: dropshadow(three-pass-box, " + enemy.color() + ", 15, 0, 0, 0);");

        // set enemy
        enemyImage.setImage(imageCache.get("image/portraits/" + enemy.portrait() + ".png"));
        enemyFlag.setImage(imageCache.get("image/flags/" + enemy.flag() + ".png"));
        enemyBox.setStyle("-fx-effect: dropshadow(three-pass-box, " + enemy.color() + ", 15, 0, 0, 0);");

        // Create BooleanBindings for the war name conditions
        BooleanBinding nameEmpty = warNameTextField.textProperty().isEmpty();
        BooleanBinding nameTooLong = warNameTextField.textProperty().length().greaterThan(18);
        BooleanBinding tabNotSelected = Bindings.createBooleanBinding(
                () -> warReasonTabPane.getSelectionModel().getSelectedItem() == null,
                warReasonTabPane.getSelectionModel().selectedItemProperty()
        );

        declareWarButton1.disableProperty().bind(
                nameEmpty.or(nameTooLong).or(tabNotSelected)
        );
    }

    public void declareWar() {
        String warReason = warReasons.get(warReasonTabPane.getSelectionModel().getSelectedItem());
        CreateWarDto createWarDto = new CreateWarDto(
                empire._id(),
                enemy._id(),
                warNameTextField.getText(),
                Map.of("reason", warReason)
        );

        subscriber.subscribe(warsApiService.createWar(game._id(), createWarDto), response -> back());
    }

    public void back() {
        removeDeclareWar();
        diplomacyRoot.setVisible(true);
    }

    private void removeDeclareWar() {
        AnchorPane parent = (AnchorPane) this.getParent();
        parent.getChildren().remove(this);
    }

}
