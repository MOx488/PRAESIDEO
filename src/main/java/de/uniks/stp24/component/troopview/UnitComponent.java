package de.uniks.stp24.component.troopview;

import de.uniks.stp24.dto.ReadShipDto;
import de.uniks.stp24.model.ShipType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.getUnitInfo;

@Component(view = "Unit.fxml")
public class UnitComponent extends VBox implements ReusableItemComponent<ReadShipDto> {
    @FXML
    VBox unitRootBox;
    @FXML
    Label typeLabel;
    @FXML
    Label xpLabel;
    @FXML
    Label hpLabel;
    @FXML
    ProgressBar healthBar;

    @Inject
    public Subscriber subscriber;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("listView")
    ListView<ReadShipDto> listView;
    @Param("unitTypeInfo")
    List<ShipType> unitTypeInfo;

    @Inject
    public UnitComponent() {
    }

    @Override
    public void setItem(@NotNull ReadShipDto ship) {
        ShipType shipType = getUnitInfo(unitTypeInfo, ship.type());
        double maxHealth = shipType != null ? shipType.health() : -1;

        // Fill information
        typeLabel.setText(bundle.getString(ship.type()));
        xpLabel.setText("XP: " + ship.experience());
        hpLabel.setText("HP: " + ship.health() + "/" + maxHealth);
        healthBar.setProgress(ship.health() / maxHealth);

        // Responsive design
        unitRootBox.prefWidthProperty().bind(listView.widthProperty().multiply(0.8));
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
    }
}
