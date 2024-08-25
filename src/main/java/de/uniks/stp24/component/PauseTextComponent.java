package de.uniks.stp24.component;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;

import javax.inject.Inject;
import java.util.ResourceBundle;

@Component(view = "PauseText.fxml")
public class PauseTextComponent extends HBox {
    @FXML
    Text pauseText;

    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public PauseTextComponent() {
    }

}
