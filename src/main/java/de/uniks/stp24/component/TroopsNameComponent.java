package de.uniks.stp24.component;

import de.uniks.stp24.model.Fleet;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@Component(view = "TroopsName.fxml")
public class TroopsNameComponent extends HBox implements ReusableItemComponent<Fleet> {

    @FXML
    Label troopsName;

    @Inject
    public TroopsNameComponent() {
    }

    @Override
    public void setItem(@NotNull Fleet fleet) {
        troopsName.setText(fleet.name());
    }

}