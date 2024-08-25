package de.uniks.stp24.component.districts;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;

@Component(view = "DistrictSquare.fxml")
public class DistrictSquareComponent extends Pane {
    @FXML
    Pane square;
    @FXML
    Pane backgroundPane;

    @Param("parent")
    DistrictBarComponent parent;
    @Param("owned")
    boolean owned;
    @Param("notMySystem")
    boolean notMySystem;

    private boolean selected = false;

    @Inject
    public DistrictSquareComponent() {
    }

    public void select() {
        if (notMySystem) {
            return;
        }
        if (selected) {
            deselect();
        } else {
            parent.select(this, owned);
            selected = true;
            backgroundPane.setStyle("-fx-background-color: #FFFF00;");
        }
    }

    public void deselect() {
        if (selected) {
            selected = false;
            parent.deselect();
            backgroundPane.setStyle("-fx-background-color: transparent;");
        }
    }
}
