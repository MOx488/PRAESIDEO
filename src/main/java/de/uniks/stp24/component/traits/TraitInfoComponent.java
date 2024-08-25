package de.uniks.stp24.component.traits;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Effect;
import de.uniks.stp24.model.Trait;
import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Constants.*;

@Component(view = "TraitInfo.fxml")
public class TraitInfoComponent extends VBox {

    @FXML
    Label seperatorLabel;
    @FXML
    VBox traitInfoVBox;
    @FXML
    Label traitInfoLabel;

    @Inject
    public App app;
    @Inject
    public ImageCache imageCache;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public Provider<TraitEffectComponent> traitEffectComponentProvider;
    @Inject
    public Subscriber subscriber;

    private String traitInfoType;

    @Inject
    public TraitInfoComponent() {
    }

    public void setTraitInfoComponent(String traitInfoType, List<Effect> effects, Trait trait) {
        if (traitInfoType == null || traitInfoType.isEmpty()) {
            return;
        }

        this.traitInfoType = traitInfoType;
        traitInfoVBox.getChildren().clear();
        traitInfoVBox.getChildren().add(seperatorLabel);

        switch (traitInfoType) {
            case TRAIT_INFO_TYPE_STARTING -> setStartingInfo(effects);
            case TRAIT_INFO_TYPE_CONFLICTS -> setConflictsInfo(trait);
            case TRAIT_INFO_TYPE_PRODUCTION -> setProductionInfo(effects);
            case TRAIT_INFO_TYPE_COST -> setCostInfo(effects);
            default -> {
            }
        }
    }

    // Helper methods for starting info
    private void setStartingInfo(List<Effect> startingEffects) {
        setLabel(traitInfoLabel, "starting");
        for (Effect effect : startingEffects) {
            double bonus = effect.bonus();
            String resourceName = getResourceNameOfStarting(effect.variable());
            addTraitEffectComponent(resourceName, "", bonus);
        }
    }

    private String getResourceNameOfStarting(String variable) {
        return splitVariable(variable)[1];
    }

    // Helper methods for production or cost info
    private void setProductionInfo(List<Effect> effects) {
        setLabel(traitInfoLabel, "building.production");
        addEffectsToVBox(effects);
    }

    private void setCostInfo(List<Effect> effects) {
        setLabel(traitInfoLabel, "cost");
        addEffectsToVBox(effects);
    }

    private void addEffectsToVBox(List<Effect> effects) {
        for (Effect effect : effects) {
            double percentage = getPercentage(effect.multiplier());
            String resourceName = getResourceName(effect.variable());
            String buildingName = getBuildingName(effect.variable());
            addTraitEffectComponent(resourceName, buildingName, percentage);
        }
    }

    private double getPercentage(double multiplier) {
        BigDecimal bdMultiplier = BigDecimal.valueOf(multiplier);
        BigDecimal percentage;

        if (bdMultiplier.compareTo(BigDecimal.ONE) > 0) {
            percentage = bdMultiplier.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100));
        } else if (bdMultiplier.compareTo(BigDecimal.ONE) < 0) {
            percentage = BigDecimal.ONE.subtract(bdMultiplier).multiply(BigDecimal.valueOf(-100));
        } else {
            percentage = BigDecimal.ZERO;
        }

        return percentage.setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private void addTraitEffectComponent(String resourceName, String buildingName, double value) {
        TraitEffectComponent traitEffectComponent = getTraitEffectComponent(resourceName, buildingName, value);
        traitInfoVBox.getChildren().add(traitEffectComponent);
    }

    private TraitEffectComponent getTraitEffectComponent(String resourceName, String buildingName, double value) {
        return app.initAndRender(traitEffectComponentProvider.get(),
                Map.of("traitInfoType", traitInfoType,
                        "value", value,
                        "resource", resourceName,
                        "building", buildingName),
                subscriber);
    }

    private String getResourceName(String variable) {
        return splitVariable(variable)[3];
    }

    private String getBuildingName(String variable) {
        return splitVariable(variable)[1];
    }

    private String[] splitVariable(String variable) {
        return variable.split("\\.");
    }

    // Helper methods for conflicts
    private void setConflictsInfo(Trait trait) {
        setLabel(traitInfoLabel, "trait.conflicts");
        for (String conflict : trait.conflicts()) {
            traitInfoVBox.getChildren().add(createConflictLabel(conflict));
        }
    }

    private void setLabel(Label label, String key) {
        label.setText(bundle.getString(key));
        traitInfoVBox.getChildren().add(label);
    }

    public Label createConflictLabel(String conflict) {
        String conflictTrait = bundle.getString("traits." + conflict);
        Label label = new Label("- " + conflictTrait);
        label.getStyleClass().add("small-medium");
        return label;
    }
}
