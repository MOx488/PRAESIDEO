package de.uniks.stp24.component.war;

import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Player;
import de.uniks.stp24.model.War;
import de.uniks.stp24.rest.WarsApiService;
import de.uniks.stp24.service.ImageCache;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Objects;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.initWarComponent;

@Component(view = "WarAttacks.fxml")
public class WarAttacksComponent extends AnchorPane implements ReusableItemComponent<War> {

    @FXML
    VBox avatarBox;
    @FXML
    ImageView avatarImage;
    @FXML
    Label enemyName;
    @FXML
    Button stopWarButton;
    @FXML
    Button reasonButton;
    @FXML
    Label warName;

    @Param("game")
    Game game;

    @Param("empires")
    ObservableList<ReadEmpireDto> empires;

    @Param("diplomacyRoot")
    DiplomacyComponent diplomacyRoot;

    @Param("players")
    ObservableList<Player> players;

    @Inject
    public WarsApiService warsApiService;

    @Inject
    public ImageCache imageCache;

    @Inject
    public Subscriber subscriber;

    @Inject
    @Resource
    public ResourceBundle bundle;

    private War war;

    @Inject
    public WarAttacksComponent() {
    }

    @Override
    public void setItem(@NotNull War war) {
        ReadEmpireDto enemy = getEnemy(war.defender());
        Player player = players.stream().filter(p -> p.empireId().equals(war.defender())).findFirst().orElse(null);

        this.war = war;
        initWarComponent(war, Objects.requireNonNull(enemy), Objects.requireNonNull(player), warName, enemyName,
                avatarImage, avatarBox, reasonButton, imageCache, bundle);
        stopWarButton.setText(bundle.getString("stop"));
    }

    private ReadEmpireDto getEnemy(String defender) {
        for (ReadEmpireDto empire : empires) {
            if (empire._id().equals(defender)) {
                return empire;
            }
        }
        return null;
    }

    public void stopWar() {
        diplomacyRoot.unSeeReason();
        subscriber.subscribe(warsApiService.deleteWar(game._id(), war._id()));
    }

    public void seeReason() {
        diplomacyRoot.setLastClickedReasonButton(reasonButton, war);
        diplomacyRoot.seeReason(war);
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }
}
