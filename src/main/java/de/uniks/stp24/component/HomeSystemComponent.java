package de.uniks.stp24.component;

import de.uniks.stp24.model.SystemType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

@Component(view = "HomeSystem.fxml")
public class HomeSystemComponent extends ToggleButton {
    @FXML
    Label typeLabel;
    @FXML
    Label capacityLabel;

    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("type")
    public SystemType type;

    @Inject
    public HomeSystemComponent() {
    }

    @OnRender
    public void onRender() {
        typeLabel.setText(bundle.getString(type.id()));
        List<Integer> range = type.capacity_range();
        capacityLabel.setText(bundle.getString("capacity.range") + " " + range.getFirst() + " - " + range.getLast());
    }
}
