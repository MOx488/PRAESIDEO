package de.uniks.stp24.component.buildings;

import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ResourceBundle;

@Component(view = "Building.fxml")
public class BuildingComponent extends HBox implements ReusableItemComponent<String> {

    @FXML
    ImageView buildingImage;
    @FXML
    Label buildingName;
    @FXML
    VBox buildingIconVBox;

    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    @Named("building-icons")
    public ResourceBundle buildingIcons;
    @Inject
    public ImageCache imageCache;


    @Inject
    public BuildingComponent() {
    }

    @Override
    public void setItem(@NotNull String buildingID) {
        String imageName = buildingIcons.getString("building.icon." + buildingID);
        buildingImage.setImage(imageCache.get("image/game_resources/" + imageName + ".png"));
        String buildingName = bundle.getString("building." + buildingID);
        this.buildingName.setText(buildingName);
    }
}
