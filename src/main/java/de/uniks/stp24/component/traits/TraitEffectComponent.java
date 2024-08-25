package de.uniks.stp24.component.traits;

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

import static de.uniks.stp24.util.Constants.*;

@Component(view = "TraitEffect.fxml")
public class TraitEffectComponent extends HBox {

    @FXML
    ImageView resourceImage;
    @FXML
    Label valueLabel;

    @Param("traitInfoType")
    String traitInfoType;
    @Param("value")
    double value;
    @Param("resource")
    String resource;
    @Param("building")
    String building;

    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public TraitEffectComponent() {
    }

    @OnRender
    public void render() {
        String resourcePath = "image/game_resources/" + resource + ".png";
        resourceImage.setImage(imageCache.get(resourcePath));

        switch (traitInfoType) {
            case TRAIT_INFO_TYPE_STARTING -> renderStartingType();
            case TRAIT_INFO_TYPE_COST, TRAIT_INFO_TYPE_PRODUCTION -> renderCostOrProductionType();
        }
    }

    private void renderStartingType() {
        int intValue = (int) value;
        String sign = intValue > 0 ? "+" : "";
        valueLabel.setText(getResourceName() + sign + intValue);
    }

    private void renderCostOrProductionType() {
        valueLabel.setText(getBuildingName() + getValuePercentageText());
    }

    private String getBuildingName() {
        return bundle.getString("building." + building) + ": ";
    }

    private String getResourceName() {
        return bundle.getString("resources." + resource) + ": ";
    }

    private String getValuePercentageText() {
        if (value == 0) {
            return "0";
        }

        String percentageText = value < 0 ? formatNegativeValue() : formatPositiveValue();
        return percentageText + " %";
    }

    private String formatNegativeValue() {
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.valueOf(value);
        }
    }

    private String formatPositiveValue() {
        if (value == (int) value) {
            return "+" + (int) value;
        } else {
            return "+" + value;
        }
    }
}
