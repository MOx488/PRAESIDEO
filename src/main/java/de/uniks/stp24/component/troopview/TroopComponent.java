package de.uniks.stp24.component.troopview;

import de.uniks.stp24.model.Fleet;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@Component(view = "Troop.fxml")
public class TroopComponent extends VBox implements ReusableItemComponent<Fleet> {
    @FXML
    VBox troopRootBox;
    @FXML
    Label troopLabel;

    @Param("listView")
    ListView<Fleet> listView;

    @Inject
    public TroopComponent() {
    }

    @Override
    public void setItem(@NotNull Fleet fleet) {
        troopLabel.setText(fleet.name());

        // Responsive design
        troopRootBox.prefWidthProperty().bind(listView.widthProperty().multiply(0.8));
    }
}
