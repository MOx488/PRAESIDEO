package de.uniks.stp24.service;

import de.uniks.stp24.App;
import de.uniks.stp24.component.buildings.BuildingPopUpStatComponent;
import de.uniks.stp24.model.Effect;
import de.uniks.stp24.model.EffectSource;
import de.uniks.stp24.model.ExplainedVariable;
import de.uniks.stp24.rest.GameLogicApiService;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.function.Consumer;

import static de.uniks.stp24.util.Methods.createLabel;

public class ExplainedVariableService {

    @Inject
    public App app;

    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public GameLogicApiService gameLogicApiService;

    @Inject
    public Subscriber subscriber;

    @Inject
    public Provider<BuildingPopUpStatComponent> buildingPopUpStatComponentProvider;

    private final List<String> defaultFieldNames = List.of("cost", "upkeep", "production");

    @Inject
    public ExplainedVariableService() {
    }


    //variableType = districts/buildings...
    //objectName = district/building name
    public List<String> getExplainedVariablesOfObject(String variableType, String objectName, Object object, List<String> fieldNames) {
        List<String> variableNames = new ArrayList<>();
        final RecordComponent[] components = object.getClass().getRecordComponents();

        if (fieldNames == null) {
            fieldNames = this.defaultFieldNames;
        }

        //create variable name for every fieldName
        for (final RecordComponent component : components) {
            if (!fieldNames.contains(component.getName())) {
                continue;
            }
            try {
                final String category = component.getName();
                final Object value = component.getAccessor().invoke(object);
                variableNames.addAll(getVariableNames(value, variableType, objectName, category));
            } catch (Exception e) {
                System.err.println("ExplainedVariable error: " + e.getMessage());
            }
        }

        return variableNames;
    }

    private List<String> getVariableNames(Object value, String variableType, String objectName, String category) {
        final List<String> variableNames = new ArrayList<>();
        if (value instanceof Map) {
            final Map<String, Integer> valueMap = (Map<String, Integer>) value;
            for (String key : valueMap.keySet()) { //minerals, fuel ... etc
                // show default values at the start of a category
                if (key.equals("default")) {
                    variableNames.addFirst(variableType + "." + objectName + "." + category + "." + key);
                } else {
                    variableNames.add(variableType + "." + objectName + "." + category + "." + key);
                }
            }
        } else {
            variableNames.add(variableType + "." + objectName + "." + category);
        }
        return variableNames;
    }

    public VBox buildExplainedVariableToolTip(String localizedObjectName, String empireId, String variableType, String objectName, Object object, List<String> fieldNames, Consumer<VBox> finishedBuilding, boolean includeSeparators) {
        VBox tooltipRoot = new VBox();
        tooltipRoot.getStyleClass().add(Objects.requireNonNull(App.class.getResource("styles.css")).toExternalForm());

        if (localizedObjectName != null) {
            tooltipRoot.getChildren().add(createLabel(localizedObjectName, "small-medium"));
        }

        final List<String> variableNames = this.getExplainedVariablesOfObject(variableType, objectName, object, fieldNames);
        if (!variableNames.isEmpty()) {
            subscriber.subscribe(gameLogicApiService.getExplainedVariables("dummy", empireId, variableNames), result -> {
                        this.constructExplainedVariablesToolTip(result, tooltipRoot, includeSeparators, objectName);

                        if (finishedBuilding != null) {
                            finishedBuilding.accept(tooltipRoot);
                        }
                    }
            );
        }

        return tooltipRoot;
    }

    public void setExplainedVariableToolTip(Control tooltipOwner, String localizedObjectName, String empireId, String variableType, String objectName, Object object, List<String> fieldNames, Consumer<List<ExplainedVariable>> explainedVariablesConsumer) {
        Tooltip tooltip = new Tooltip();
        VBox tooltipRoot = new VBox();

        tooltip.setGraphic(tooltipRoot);
        tooltip.setShowDelay(Duration.ONE);
        tooltip.getStyleClass().add(Objects.requireNonNull(App.class.getResource("styles.css")).toExternalForm());

        if (localizedObjectName != null) {
            tooltipRoot.getChildren().add(createLabel(localizedObjectName, "small-medium"));
        }

        tooltipOwner.setTooltip(tooltip);

        final List<String> variableNames = this.getExplainedVariablesOfObject(variableType, objectName, object, fieldNames);
        subscriber.subscribe(gameLogicApiService.getExplainedVariables("dummy", empireId, variableNames),
                result -> {
                    this.constructExplainedVariablesToolTip(result, tooltipRoot, true, objectName);

                    if (explainedVariablesConsumer != null) {
                        explainedVariablesConsumer.accept(result);
                    }
                }
        );
    }

    private void constructExplainedVariablesToolTip(List<ExplainedVariable> explainedVariables, VBox tooltipRoot, boolean includeSeparators, String objectName) {
        String previousCategory = null;

        for (ExplainedVariable explainedVariable : explainedVariables) {
            final String variable = explainedVariable.variable();
            final String[] variableNameParts = variable.split("\\.");
            String category = variableNameParts[variableNameParts.length - 2];
            final String resourceName = variableNameParts[variableNameParts.length - 1];

            if (category.equals(objectName)) {
                category = resourceName;
            }

            // ships (units) also have the "build_time" category, but "training" a unit makes more sense than "building"
            if (category.equals("build_time") && variable.contains("ship")) {
                category = "training_time";
            }

            if (includeSeparators) {
                this.applySeparators(tooltipRoot, previousCategory, category);
            }

            previousCategory = category;

            final Label localizedBaseLabel = createLabel(bundle.getString("variable.initial") + ":", "small-medium");
            final BuildingPopUpStatComponent baseStatComponent = getStatAmountComponent(resourceName, explainedVariable.initial(), explainedVariable.variable());
            final HBox baseHBox = new HBox(localizedBaseLabel, baseStatComponent);

            tooltipRoot.getChildren().add(baseHBox);

            if (explainedVariable.sources().isEmpty()) {
                continue;
            }

            for (EffectSource source : explainedVariable.sources()) {
                for (int i = 0; i < source.effects().size(); i++) {
                    Effect effect = source.effects().get(i);

                    //base
                    if (effect.base() > 0) {
                        final String baseRounded = String.format("%.2f", effect.base());
                        tooltipRoot.getChildren().add(createLabel(bundle.getString("source.base") + ": " + baseRounded, "small-medium"));
                    }

                    //multiplier
                    final HBox sourceHBox = new HBox();
                    final double multiplier = (effect.multiplier() - 1d) * 100d;
                    String prefix = "+";
                    if (multiplier < 0) {
                        prefix = "";
                    }

                    final String multiplierRounded = String.format("%.2f", multiplier);
                    sourceHBox.getChildren().add(createLabel(prefix + multiplierRounded + "% ", "small-medium"));
                    sourceHBox.getChildren().add(createLabel(bundle.getString(source.id()), "small-medium"));
                    tooltipRoot.getChildren().add(sourceHBox);


                    //bonus
                    if (effect.bonus() > 0) {
                        final HBox bonusHbox = new HBox();
                        tooltipRoot.getChildren().add(createLabel("", "small-medium"));

                        final String bonusRounded = String.format("%.2f", effect.bonus());
                        bonusHbox.getChildren().add(createLabel(bundle.getString("source.bonus") + ": ", "small-medium"));
                        bonusHbox.getChildren().add(createLabel("+" + bonusRounded, "small-medium"));

                        tooltipRoot.getChildren().add(bonusHbox);
                    }
                }
            }

            final Label localizedFinalLabel = createLabel(bundle.getString("variable.final"), "small-medium");

            final BuildingPopUpStatComponent finalStatComponent = getStatAmountComponent(resourceName, explainedVariable.end(), explainedVariable.variable());
            final HBox finalHBox = new HBox(localizedFinalLabel, finalStatComponent);

            tooltipRoot.getChildren().add(finalHBox);
        }
    }

    private void applySeparators(VBox tooltipRoot, String previousCategory, String category) {
        if (!tooltipRoot.getChildren().isEmpty()) {
            if (previousCategory == null || !previousCategory.equals(category)) {
                //only if we are within a new category
                tooltipRoot.getChildren().add(createLabel("------", "small-medium"));
                if (category.equals("production")) {
                    tooltipRoot.getChildren().add(createLabel(bundle.getString("building.production").toUpperCase(), "small-medium"));
                } else
                    tooltipRoot.getChildren().add(createLabel(bundle.getString(category).toUpperCase(), "small-medium"));
            } else {
                //same category -> only new line
                tooltipRoot.getChildren().add(createLabel(" ", "small-medium"));
            }
        } else {
            if (category.equals("production")) {
                tooltipRoot.getChildren().add(createLabel(bundle.getString("building.production").toUpperCase(), "small-medium"));
            } else tooltipRoot.getChildren().add(createLabel(bundle.getString(category).toUpperCase(), "small-medium"));
            tooltipRoot.getChildren().add(createLabel("------", "small-medium"));
        }
    }

    private BuildingPopUpStatComponent getStatAmountComponent(String resource, double amount, String variable) {
        boolean isAttackOrDefense = (variable.contains("attack") || variable.contains("defense")) && !variable.contains("fortress");
        boolean isDefault = variable.contains("default");
        final String imageDir = isAttackOrDefense ? "ships" : "game_resources";
        final String resourcePath = "image/" + imageDir + "/" + resource + ".png";
        return app.initAndRender(buildingPopUpStatComponentProvider.get(),
                Map.of("amount", amount, "isDefault", isDefault, "isAttackOrDefense", isAttackOrDefense,
                        "ignoreEmptyImage", true, "imagePath", resourcePath), subscriber);
    }

    public void destroy() {
        subscriber.dispose();
    }

}
