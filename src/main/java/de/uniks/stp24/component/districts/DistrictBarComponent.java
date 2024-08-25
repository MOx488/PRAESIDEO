package de.uniks.stp24.component.districts;

import de.uniks.stp24.App;
import de.uniks.stp24.model.District;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

@Component(view = "DistrictBar.fxml")
public class DistrictBarComponent extends HBox {
    @FXML
    Label productionLabel3;
    @FXML
    ImageView productionIcon3;
    @FXML
    Label productionLabel2;
    @FXML
    ImageView productionIcon2;
    @FXML
    Label productionLabel1;
    @FXML
    ImageView productionIcon1;
    @FXML
    ImageView upkeepIcon1;
    @FXML
    Label upkeepLabel1;
    @FXML
    ImageView upkeepIcon2;
    @FXML
    Label upkeepLabel2;
    @FXML
    GridPane districtGridPane;
    @FXML
    Label districtName;
    @FXML
    ImageView districtIcon;
    @FXML
    HBox districtBarRoot;

    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public App app;
    @Inject
    public ImageCache imageCache;
    @Inject
    public Provider<DistrictSquareComponent> districtSquareComponentProvider;

    @Param("district")
    String key;
    @Param("system")
    GameSystem system;
    @Param("presets")
    List<District> presets;
    @Param("parent")
    DistrictComponent parent;
    @Param("notMySystem")
    boolean notMySystem;
    @Param("subscriber")
    Subscriber subscriber;

    int districtAmount;
    int districtSlotAmount;

    DistrictSquareComponent selected = null;

    @Inject
    public DistrictBarComponent() {

    }

    @OnInit
    public void init() {
        if (system.districts().get(key) == null) {
            districtAmount = 0;
        } else {
            districtAmount = system.districts().get(key);
        }
        districtSlotAmount = system.districtSlots().get(key);
    }

    @OnRender
    public void render() {
        renderSquares();
        switch (key) {
            case "energy" -> districtIcon.setImage(imageCache.get("image/game_resources/energy.png"));
            case "mining" -> districtIcon.setImage(imageCache.get("image/game_resources/minerals.png"));
            case "agriculture" -> districtIcon.setImage(imageCache.get("image/game_resources/food.png"));
            case "research_site" -> districtIcon.setImage(imageCache.get("image/game_resources/research.png"));
            case "ancient_refinery" -> districtIcon.setImage(imageCache.get("image/game_resources/fuel.png"));
            case "city" -> districtIcon.setImage(imageCache.get("image/game_resources/credits.png"));
            case "ancient_factory" -> districtIcon.setImage(imageCache.get("image/game_resources/consumer_goods.png"));
            case "ancient_foundry" -> districtIcon.setImage(imageCache.get("image/game_resources/alloys.png"));
            case "industry" -> districtIcon.setImage(imageCache.get("image/game_resources/industry.png"));
        }

        setUpkeepAndProduction();
        districtName.setText(bundle.getString("districts.name." + this.key));
    }

    private void renderSquares() {
        districtGridPane.getChildren().clear();
        for (int i = 0; i < districtSlotAmount; i++) {
            DistrictSquareComponent square = app.initAndRender(districtSquareComponentProvider.get(), Map.of("parent", this, "owned", (i < districtAmount), "notMySystem", notMySystem), subscriber);
            if (i < districtAmount) {
                switch (key) {
                    case "energy" -> square.square.setStyle("-fx-background-color: rgb(204, 0, 0);");
                    case "mining" -> square.square.setStyle("-fx-background-color: rgb(0, 0, 153);");
                    case "agriculture" -> square.square.setStyle("-fx-background-color: rgb(0, 153, 0);");
                    case "research_site" -> square.square.setStyle("-fx-background-color: rgb(126, 166, 224);");
                    case "ancient_refinery" -> square.square.setStyle("-fx-background-color: rgb(26, 26, 26);");
                    case "city" -> square.square.setStyle("-fx-background-color: rgb(103, 171, 159);");
                    case "ancient_factory" -> square.square.setStyle("-fx-background-color: rgb(102, 102, 255);");
                    case "ancient_foundry" -> square.square.setStyle("-fx-background-color: rgb(77, 77, 77);");
                    case "industry" -> square.square.setStyle("-fx-background-color: rgb(127, 0, 255);");
                }
            }
            districtGridPane.add(square, i % 4, i / 4);
        }
    }

    private void setUpkeepAndProduction() {
        for (District preset : presets) {
            if (preset.id().equals(key)) {
                switch (preset.upkeep().size()) {
                    case 2:
                        upkeepLabel2.setText(preset.upkeep().lastEntry().getValue() * districtAmount + "");
                        upkeepIcon2.setImage(imageCache.get("image/game_resources/" + preset.upkeep().lastKey() + ".png"));
                    case 1:
                        upkeepLabel1.setText(preset.upkeep().firstEntry().getValue() * districtAmount + "");
                        upkeepIcon1.setImage(imageCache.get("image/game_resources/" + preset.upkeep().firstKey() + ".png"));
                }
                TreeMap<String, Integer> production = preset.production();
                switch (production.size()) {
                    case 3:
                        productionLabel3.setText(production.lastEntry().getValue() * districtAmount + "");
                        productionIcon3.setImage(imageCache.get("image/game_resources/" + production.lastKey() + ".png"));
                        production.remove(production.lastKey());
                    case 2:
                        productionLabel2.setText(production.lastEntry().getValue() * districtAmount + "");
                        productionIcon2.setImage(imageCache.get("image/game_resources/" + production.lastKey() + ".png"));
                    case 1:
                        productionLabel1.setText(production.firstEntry().getValue() * districtAmount + "");
                        productionIcon1.setImage(imageCache.get("image/game_resources/" + production.firstKey() + ".png"));
                }
            }
        }
    }

    public void select(DistrictSquareComponent square, boolean owned) {

        if (selected != null) {
            deselect();
        }
        parent.select(this, key, owned);
        selected = square;
    }

    public void deselect() {
        if (selected != null) {
            selected.deselect();
            selected = null;
            parent.deselect();
        }
    }
}
