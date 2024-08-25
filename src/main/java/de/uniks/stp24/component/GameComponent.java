package de.uniks.stp24.component;

import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.User;
import de.uniks.stp24.rest.UsersApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@Component(view = "Game.fxml")
public class GameComponent extends HBox implements ReusableItemComponent<Game> {
    @FXML
    Circle gameCircle;
    @FXML
    Text nameText;
    @FXML
    Text hostText;
    @FXML
    Text playercountText;

    @Inject
    public UsersApiService usersApiService;

    @Param("users")
    public ObservableList<User> users = FXCollections.observableArrayList();
    @Param("games")
    public ObservableList<Game> games = FXCollections.observableArrayList();

    @Inject
    public GameComponent() {
    }

    @Override
    public void setItem(@NotNull Game game) {
        // set the game name
        nameText.setText(game.name());
        // set the game owner
        User owner = users.stream().filter(user -> user._id().equals(game.owner())).findFirst().orElse(null);
        hostText.setText(owner != null ? owner.name() : "NULL");
        // set the player count
        if (game.maxMembers() == 0) {
            playercountText.setText(game.members() + "/100");
        } else {
            playercountText.setText(game.members() + "/" + game.maxMembers());
        }
    }

}
