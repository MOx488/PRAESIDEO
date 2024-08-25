package de.uniks.stp24.component;

import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;

@Component(view = "ResourceAmount.fxml")
public class ResourceAmountComponent extends HBox {
    @FXML
    Label resourceLabel;
    @FXML
    ImageView resourceImage;

    @Inject
    public ImageCache imageCache;

    @Param("amount")
    int amount;
    @Param("resource")
    String resource;


    @Inject
    public ResourceAmountComponent() {
    }

    @OnRender
    public void render() {
        this.resourceLabel.setText(String.valueOf(amount));
        this.resourceImage.setImage(imageCache.get("image/game_resources/" + resource + ".png"));
    }

    public int getAmount() {
        return amount;
    }

    public String getResource() {
        return resource;
    }
}
