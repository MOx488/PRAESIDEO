package de.uniks.stp24.component.players;

import de.uniks.stp24.model.Player;
import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@Component(view = "Player.fxml")
public class PlayerComponent extends HBox implements ReusableItemComponent<Player> {
    @FXML
    ImageView playerFlag;
    @FXML
    Circle playerColor;
    @FXML
    Label playerName;

    @Inject
    public ImageCache imageCache;

    @Inject
    public PlayerComponent() {
    }

    @Override
    public void setItem(@NotNull Player player) {
        playerFlag.setImage(imageCache.get("image/flags/" + player.flag() + ".png"));
        playerColor.setStyle("-fx-fill: " + player.color());
        playerName.setText(player.name());
    }
}
