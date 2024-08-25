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
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Objects;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.initWarComponent;

@Component(view = "WarDefends.fxml")
public class WarDefendsComponent extends AnchorPane implements ReusableItemComponent<War> {

    @FXML
    VBox avatarBox;
    @FXML
    ImageView enemyImage;
    @FXML
    Label enemyName;
    @FXML
    Button reasonButton;
    @FXML
    Label warName;

    @Param("empires")
    ObservableList<ReadEmpireDto> empires;

    @Param("diplomacyRoot")
    DiplomacyComponent diplomacyRoot;

    @Param("players")
    ObservableList<Player> players;

    @Inject
    public ImageCache imageCache;

    @Inject
    @Resource
    public ResourceBundle bundle;

    private War war;

    @Inject
    public WarDefendsComponent() {

    }

    @Override
    public void setItem(@NotNull War war) {
        ReadEmpireDto enemy = getEnemy(war.attacker());
        Player player = players.stream().filter(p -> p.empireId().equals(war.attacker())).findFirst().orElse(null);

        initWarComponent(war, Objects.requireNonNull(enemy), Objects.requireNonNull(player), warName, enemyName,
                enemyImage, avatarBox, reasonButton, imageCache, bundle);
        this.war = war;
        warName.setText(war.name());
        enemyName.setText(Objects.requireNonNull(enemy).name() + " (" + Objects.requireNonNull(player).name() + ")");
        enemyImage.setImage(imageCache.get("image/portraits/" + enemy.portrait() + ".png"));
        avatarBox.setStyle("-fx-effect: dropshadow(three-pass-box, " + enemy.color() + ", 15, 0, 0, 0);");
        reasonButton.setText(bundle.getString("reason"));
    }

    private ReadEmpireDto getEnemy(String attacker) {
        for (ReadEmpireDto empire : empires) {
            if (empire._id().equals(attacker)) {
                return empire;
            }
        }
        return null;
    }

    public void seeReason() {
        diplomacyRoot.setLastClickedReasonButton(reasonButton, war);
        diplomacyRoot.seeReason(war);
    }
}