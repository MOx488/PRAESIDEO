package de.uniks.stp24.component.buildings;

import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;
import java.util.ResourceBundle;

@Component(view = "BuildingPopUpStat.fxml")
public class BuildingPopUpStatComponent extends HBox {

    @FXML
    Label resourceLabel;
    @FXML
    ImageView resourceImage;

    @Param("amount")
    double amount;
    @Param("isDefault")
    boolean isDefault;
    @Param("isAttackOrDefense")
    boolean isAttackOrDefense;
    @Param("ignoreEmptyImage")
    boolean ignoreEmptyImage;
    @Param("imagePath")
    String resourcePath;

    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    ResourceBundle bundle;

    @Inject
    public BuildingPopUpStatComponent() {
    }

    @OnRender
    public void render() {
        String roundedAmount;
        if (amount % 1 == 0) {
            roundedAmount = String.format("%.0f", amount);
        } else {
            roundedAmount = String.format("%.2f", amount);
        }
        if (isDefault) {
            roundedAmount += " (" + bundle.getString("default") + ")";
        } else if (isAttackOrDefense) {
            roundedAmount += " " + bundle.getString("against") + " ";
        } else if (resourcePath.contains("time")) {
            roundedAmount += " " + bundle.getString("days") + " ";
        }
        resourceLabel.setText(roundedAmount);
        resourceImage.setImage(imageCache.get(resourcePath));

        if (ignoreEmptyImage && resourceImage.getImage().getUrl().equals("https://via.placeholder.com/150?text=Image+not+found")) {
            resourceImage.setVisible(false);
        }
    }

}