package de.uniks.stp24.component;

import de.uniks.stp24.model.GameSystem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@Component(view = "CastleName.fxml")
public class CastleNameComponent extends HBox implements ReusableItemComponent<GameSystem> {

    @FXML
    Label castleName;

    @Inject
    public CastleNameComponent() {
    }

    @Override
    public void setItem(@NotNull GameSystem castle) {
        castleName.setText(castle.name());
    }

}
