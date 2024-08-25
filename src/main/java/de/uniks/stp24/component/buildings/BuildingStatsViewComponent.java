package de.uniks.stp24.component.buildings;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Building;
import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "BuildingStatsView.fxml")
public class BuildingStatsViewComponent extends VBox {

    @FXML
    Label buildingNameLabel;
    @FXML
    Label upKeepLabel;
    @FXML
    Label productionLabel;
    @FXML
    HBox upKeepHBox;
    @FXML
    HBox productionHBox;
    @Inject
    public Subscriber subscriber;
    @Resource
    @Inject
    public ResourceBundle bundle;
    @Inject
    @Named("building-icons")
    public ResourceBundle buildingBundle;
    @Inject
    public Provider<BuildingStatComponent> buildingStatComponentProvider;
    @Inject
    public App app;
    @Inject
    public ImageCache imageCache;

    @Inject
    public BuildingStatsViewComponent() {
    }

    public void setBuildingStats(String buildingID, Map<String, Building> storeBuildings) {
        buildingNameLabel.setText("Information: " + bundle.getString("building." + buildingID));
        upKeepLabel.setText(bundle.getString("building.upkeep") + " ");
        productionLabel.setText(bundle.getString("building.production") + " ");

        Building building = storeBuildings.get(buildingID);
        setStats(building.upkeep(), upKeepHBox, "-");
        if (!building.production().isEmpty()) {
            setStats(building.production(), productionHBox, "+");
        } else {
            handleStatsSpecialBuildings(building);
        }
    }

    private void handleStatsSpecialBuildings(Building building) {
        if (!productionHBox.getChildren().isEmpty()) {
            productionHBox.getChildren().clear();
        }
        if (building.defense() != 0) {
            productionLabel.setText(bundle.getString("health.defense") + " ");
            productionHBox.getChildren().add(getStatAmountComponent("health", building.health(), "+"));
            productionHBox.getChildren().add(getStatAmountComponent("defense", building.defense(), "+"));
        } else {
            productionLabel.setText(bundle.getString("healing_rate") + " ");
            productionHBox.getChildren().add(getStatAmountComponent("healing_rate", building.healing_rate(), "+"));
        }
    }

    private void setStats(Map<String, Integer> statsMap, HBox statBox, String prefix) {
        if (!statBox.getChildren().isEmpty()) {
            statBox.getChildren().clear();
        }
        statsMap.forEach((resource, amount) ->
                statBox.getChildren().add(getStatAmountComponent(resource, amount, prefix)));
    }

    private BuildingStatComponent getStatAmountComponent(String resource, double amount, String prefix) {
        String resourcePath = "image/game_resources/" + resource + ".png";
        return app.initAndRender(buildingStatComponentProvider.get(), Map.of("amount", amount, "imagePath", resourcePath, "prefix", prefix), subscriber);
    }

    @OnDestroy
    public void destroy() {
        subscriber.dispose();
    }
}
