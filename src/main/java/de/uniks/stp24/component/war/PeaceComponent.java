package de.uniks.stp24.component.war;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Player;
import de.uniks.stp24.service.ImageCache;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.Objects;

@Component(view = "Peace.fxml")
public class PeaceComponent extends AnchorPane implements ReusableItemComponent<ReadEmpireDto> {

    @FXML
    ImageView portraitImage;
    @FXML
    Label empireName;
    @FXML
    Button declareWarButton;
    @FXML
    VBox avatarBox;

    @Inject
    public App app;

    @Inject
    public ImageCache imageCache;

    @Inject
    public Subscriber subscriber;

    @Inject
    public Provider<DeclareWarComponent> declareWarComponentProvider;

    @Param("empire")
    Empire empire;

    @Param("game")
    Game game;

    @Param("parent")
    AnchorPane parentContainer;

    @Param("diplomacyRoot")
    Pane diplomacyRoot;

    @Param("players")
    ObservableList<Player> players;

    ReadEmpireDto enemy;


    @Inject
    public PeaceComponent() {
    }

    @Override
    public void setItem(@NotNull ReadEmpireDto enemy) {
        this.enemy = enemy;
        Player player = players.stream().filter(p -> p.empireId().equals(enemy._id())).findFirst().orElse(null);
        empireName.setText(enemy.name() + " (" + Objects.requireNonNull(player).name() + ")");
        portraitImage.setImage(imageCache.get("image/portraits/" + enemy.portrait() + ".png"));
        avatarBox.setStyle("-fx-effect: dropshadow(three-pass-box, " + enemy.color() + ", 15, 0, 0, 0);");
    }

    public void declareWar() {
        diplomacyRoot.setVisible(false);
        DeclareWarComponent declareWarComponent = app.initAndRender(declareWarComponentProvider.get(), Map.of("enemy", enemy, "empire", empire, "game", game, "diplomacyRoot", diplomacyRoot, "players", players), subscriber);
        parentContainer.getChildren().add(declareWarComponent);
    }

}
