package de.uniks.stp24.component.war;

import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Player;
import de.uniks.stp24.model.War;
import de.uniks.stp24.service.ImageCache;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;
import java.util.ResourceBundle;

@Component(view = "WarNotification.fxml")
public class WarNotificationComponent extends AnchorPane {

    @FXML
    AnchorPane warNotificationRoot;
    @FXML
    VBox enemyBox;
    @FXML
    ImageView enemyImage;
    @FXML
    ImageView reasonImage;
    @FXML
    Label reasonLabel;
    @FXML
    Label warNotificationLabel;
    @FXML
    Button okButton;

    @Inject
    public ImageCache imageCache;

    @Inject
    public ResourceBundle bundle;

    @Param("war")
    War war;

    @Param("empires")
    ObservableList<ReadEmpireDto> empires;

    @Param("players")
    ObservableList<Player> players;

    @Inject
    public WarNotificationComponent() {
    }

    @OnInit
    public void init() {
        AnchorPane.setTopAnchor(this, 100.0);
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
    }

    @OnRender
    public void render() {
        ReadEmpireDto enemy = getEnemy();
        Player enemyPlayer = players.stream()
                .filter(player -> player.empireId().equals(enemy._id()))
                .findFirst()
                .orElseThrow();

        enemyImage.setImage(imageCache.get("image/portraits/" + enemy.portrait() + ".png"));
        enemyBox.setStyle("-fx-effect: dropshadow(three-pass-box, " + enemy.color() + ", 15, 0, 0, 0);");

        String reason = (String) war._public().get("reason");
        if (reason != null) {
            reasonImage.setImage(imageCache.get("image/warreasons/" + reason + ".png"));
            reasonLabel.setText(bundle.getString("reason") + ": " + bundle.getString(reason));
        } else {
            reasonLabel.setText(bundle.getString("noReason"));
        }
        warNotificationLabel.setStyle("-fx-font-size: 20px;");
        warNotificationLabel.setText(enemy.name() + " (" + enemyPlayer.name() + ") " + bundle.getString("war.notification"));
    }

    private ReadEmpireDto getEnemy() {
        return empires.stream()
                .filter(empire -> empire._id().equals(war.defender()))
                .findFirst()
                .orElseThrow();
    }

    public void ok() {
        removeWarNotification();
    }

    private void removeWarNotification() {
        AnchorPane parent = (AnchorPane) this.getParent();
        parent.getChildren().remove(this);
    }

}
