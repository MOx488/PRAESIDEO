package de.uniks.stp24.component.players;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Player;
import de.uniks.stp24.model.War;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.service.ImageCache;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.setWarStatus;

@Component(view = "ContactsListComponent.fxml")
public class ContactsListComponent extends HBox implements ReusableItemComponent<Player> {
    @FXML
    HBox contactsBox;
    @FXML
    ImageView contactFlag;
    @FXML
    Circle contactColor;
    @FXML
    Label contactName;
    @FXML
    Label contactAveragePower;
    @FXML
    ImageView contactWarStatus;

    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public Subscriber subscriber;
    @Inject
    public App app;
    @Inject
    public GameLogicApiService gameLogicApiService;

    @Param("wars")
    ObservableList<War> wars;
    @Param("empire")
    Empire empire;
    @Param("game")
    Game game;
    @Param("empiresInGame")
    List<ReadEmpireDto> empiresInGame;

    public final List<String> stats = Arrays.asList("pathetic", "inferior", "equal", "superior", "overwhelming");

    @Inject
    public ContactsListComponent() {
    }

    @Override
    public void setItem(@NotNull Player player) {
        ReadEmpireDto empireOfPlayer = empiresInGame.stream()
                .filter(empire -> empire._id().equals(player.empireId()))
                .findFirst()
                .orElse(null);

        contactFlag.setImage(imageCache.get("image/flags/" + player.flag() + ".png"));
        contactColor.setStyle("-fx-fill: " + player.color());
        contactName.setText(player.name() + " / " + Objects.requireNonNull(empireOfPlayer).name());

        checkPower(player);
        setWarStatus(player, wars, empire, contactWarStatus, imageCache);
    }

    private void checkPower(Player player) {
        int average = stats.indexOf(player.military()) + stats.indexOf(player.military()) + stats.indexOf(player.military());

        switch (average) {
            case 0, 1:
                setText("pathetic");
                break;
            case 2, 3, 4:
                setText("inferior");
                break;
            case 5, 6, 7:
                setText("equal");
                break;
            case 8, 9, 10:
                setText("superior");
                break;
            case 11, 12:
                setText("overwhelming");
                break;
        }
    }

    public void setText(String type) {
        contactAveragePower.setText(bundle.getString(type));
        while (contactAveragePower.getStyleClass().size() > 2) {
            contactAveragePower.getStyleClass().removeLast();
        }
        contactAveragePower.getStyleClass().add(type);
    }
}
