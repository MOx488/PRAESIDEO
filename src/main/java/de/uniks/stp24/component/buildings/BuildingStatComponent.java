package de.uniks.stp24.component.buildings;

import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;

@Component(view = "BuildingStat.fxml")
public class BuildingStatComponent extends HBox {

    @FXML
    ImageView resourceImage;
    @FXML
    Label resourceLabel;

    @Param("amount")
    double amount;
    @Param("imagePath")
    String resourcePath;
    @Param("prefix")
    String prefix;

    @Inject
    public ImageCache imageCache;

    @Inject
    public BuildingStatComponent() {
    }

    @OnRender
    public void render() {
        // check if double has a different value after . then 0
        if (amount % 1 == 0) {
            resourceLabel.setText(prefix + " " + (int) amount);
        } else {
            resourceLabel.setText(prefix + " " + amount);
        }
        resourceImage.setImage(imageCache.get(resourcePath));
    }
}
