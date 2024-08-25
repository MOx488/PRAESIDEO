package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.controller.IngameController;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.model.User;
import de.uniks.stp24.service.ImageCache;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;
import java.util.ResourceBundle;

@Component(view = "BattleResult.fxml")
public class BattleResult extends VBox {

    @FXML
    public Text battle;
    @FXML
    public Text battleText;
    @FXML
    public HBox mainResultHBox;
    @FXML
    public Text mainResultTitle;
    @FXML
    public VBox avatarContainerLeft;
    @FXML
    public ImageView avatarLeft;
    @FXML
    public VBox avatarContainerMiddle;
    @FXML
    public ImageView avatarMiddle;
    @FXML
    public Text enemyName;
    @FXML
    public ImageView flagImage;
    @FXML
    public Button lobbyButton;

    @Param("game")
    Game game;
    @Param("empire")
    Empire empire;
    @Param("updatedSystems")
    ObservableList<GameSystem> updatedSystems;
    @Param("battleLost")
    boolean battleLost;
    @Param("IngameController")
    IngameController ingameController;
    @Param("isHost")
    boolean isHost;
    @Param("enemyEmpire")
    ReadEmpireDto enemyEmpire;
    @Param("enemyUser")
    User enemyUser;

    @Inject
    public ImageCache imageCache;
    @Inject
    public App app;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public BattleResult() {
    }

    @OnRender
    public void onRender() {
        if (battleLost) {
            setLostGame();
        } else {
            setWonGame();
        }
    }

    private void setLostGame() {
        if (hasPlayerLost()) { // totally lost
            mainResultTitle.setText(bundle.getString("lost"));
            mainResultHBox.setVisible(true);
            battle.setVisible(false);
            battleText.setText(bundle.getString("you.have.lost.your.empire"));
            avatarContainerLeft.setStyle("-fx-effect: dropshadow(three-pass-box, " + empire.color() + ", 15, 0, 0, 0);");
            avatarLeft.setImage(imageCache.get("image/portraits/" + empire.portrait() + ".png"));
            enemyName.setVisible(false);
            avatarContainerMiddle.getStyleClass().remove("avatar-image-container");
            avatarMiddle.setImage(imageCache.get("image/emojis/skull.png"));
            flagImage.setImage(imageCache.get("image/flags/" + empire.flag() + ".png"));
            lobbyButton.setText(bundle.getString("return.to.lobby"));
            if (isHost) {
                lobbyButton.setDisable(true);
            }
        } else { // lost a battle
            setWonOrLostBattle();
        }
    }

    private boolean hasPlayerLost() {
        // for every system that has an owner -> if the owner is us, we haven't lost yet
        return updatedSystems.stream().noneMatch(system -> system.owner() != null && system.owner().equals(empire._id()));
    }

    private boolean hasPlayerWon() {
        // for every system that has an owner -> if the owner isn't us there is still another player in the game
        return updatedSystems.stream().noneMatch(system -> system.owner() != null && !system.owner().equals(empire._id()));
    }

    private void setWonGame() {
        if (hasPlayerWon()) { // totally won
            mainResultTitle.setText(bundle.getString("won"));
            battle.setVisible(false);
            battleText.setText(bundle.getString("you.won.the.game"));
            avatarContainerLeft.setStyle("-fx-effect: dropshadow(three-pass-box, " + empire.color() + ", 15, 0, 0, 0);");
            avatarLeft.setImage(imageCache.get("image/portraits/" + empire.portrait() + ".png"));
            enemyName.setVisible(false);
            avatarContainerMiddle.getStyleClass().remove("avatar-image-container");
            avatarMiddle.setImage(imageCache.get("image/emojis/slightly_smiling_face.png"));
            flagImage.setImage(imageCache.get("image/flags/" + empire.flag() + ".png"));
            lobbyButton.setText(bundle.getString("return.to.lobby"));
        } else { // won a battle
            setWonOrLostBattle();
        }
    }

    private void setWonOrLostBattle() {
        mainResultHBox.setVisible(false);
        battle.setText(bundle.getString("battle"));
        if (battleLost) {
            battleText.setText(bundle.getString("you.lost.a.battle.against"));
        } else {
            battleText.setText(bundle.getString("you.won.a.battle.against"));
        }
        avatarContainerLeft.setVisible(false);
        avatarContainerMiddle.setStyle("-fx-effect: dropshadow(three-pass-box, " + enemyEmpire.color() + ", 15, 0, 0, 0);");
        avatarMiddle.setImage(imageCache.get("image/portraits/" + enemyEmpire.portrait() + ".png"));
        enemyName.setText(enemyUser.name());
        flagImage.setVisible(false);
        lobbyButton.setText("OK");

    }

    public void lobbyButtonClicked() {
        if (lobbyButton.getText().equals("OK")) {
            ingameController.battleResult.getChildren().clear();
            ingameController.battleResult.setVisible(false);
        } else {
            app.show("/lobby");
        }
    }
}