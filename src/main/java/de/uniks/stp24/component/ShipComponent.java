package de.uniks.stp24.component;


import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.ShipType;
import de.uniks.stp24.service.ExplainedVariableService;
import de.uniks.stp24.service.ImageCache;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;

@Component(view = "Ship.fxml")
public class ShipComponent extends HBox implements ReusableItemComponent<ShipType> {
    @FXML
    Button decrButton;
    @FXML
    Button incrButton;
    @FXML
    ImageView shipImage;
    @FXML
    Text shipType;
    @FXML
    ImageView imgViewDecrease;
    @FXML
    ImageView imgViewIncrease;
    @FXML
    Label shipAmount;
    @FXML
    Button viewTroopButton;

    @Param("shipAmounts")
    TreeMap<String, Integer> shipAmounts;
    @Param("BuildFleetComponent")
    BuildFleetComponent buildFleetComponent;
    @Param("shipTypeClicked")
    TreeMap<String, Boolean> shipTypeClicked;
    @Param("empire")
    Empire empire;

    @Inject
    public ImageCache imageCache;
    @Inject
    public ExplainedVariableService explainedVariableService;
    @Inject
    @Resource
    public ResourceBundle bundle;

    private final SimpleIntegerProperty amount = new SimpleIntegerProperty(0);
    private ShipType ship;
    private boolean clicked;
    private String clickedShipId;

    @Inject
    public ShipComponent() {
    }

    @Override
    public void setItem(@NotNull ShipType ship) {
        this.ship = ship;
        shipAmounts.putIfAbsent(ship.id(), 0);
        shipTypeClicked.putIfAbsent(ship.id(), false);
        amount.set(shipAmounts.get(ship.id()));
        clicked = shipTypeClicked.get(ship.id());
        shipType.setText(bundle.getString(ship.id()));
        shipAmount.textProperty().bind(Bindings.convert(amount));
        initImage(ship.id());
        initEventHandlers();
        if (!clicked) {
            viewTroopButton.setStyle(" ");
        }else {
            viewTroopButton.setStyle("-fx-border-color: yellow; -fx-border-width: 2px;");
        }
    }

    private void initImage(String shipName) {
        shipImage.setImage(imageCache.get("image/ships/" + shipName + ".png"));
        imgViewDecrease.setImage(imageCache.get("image/arrow-left-orange.png"));
        imgViewIncrease.setImage(imageCache.get("image/arrow-right-orange.png"));
        viewTroopButton.setText(bundle.getString("view.unit"));

        // Do not let the player decrease a planned size to negative values
        decrButton.visibleProperty().bind(amount.greaterThan(0));
    }

    private void initEventHandlers() {
        viewTroopButton.setOnAction(event -> viewTroopDetails());
    }

    public void increaseAmount() {
        amount.set(amount.get() + 1);
        shipAmounts.put(ship.id(), amount.get());
    }

    public void decreaseAmount() {
        amount.set(amount.get() - 1);
        shipAmounts.put(ship.id(), amount.get());
    }

    private void viewTroopDetails() {
        // check if the ship is clicked when yes set the clickedShipId to the ship.id
        for (String key : shipTypeClicked.keySet()) {
            if (shipTypeClicked.get(key)) {
                clickedShipId = key;
            }
        }
        // check if the ship is clicked
        clicked = shipTypeClicked.get(ship.id());

        // get the VBox from the buildFleetComponent
        VBox unitView = buildFleetComponent.unitView;

        if (!clicked && !unitView.isVisible()) {
            clicked = true;
            shipTypeClicked.put(ship.id(), true);
            initUnitView();
            unitView.setVisible(true);
            clickedShipId = ship.id();
        } else if (clicked && unitView.isVisible() && clickedShipId.equals(ship.id())) {
            // make for every key in shipTypeClicked the value false
            shipTypeClicked.replaceAll((k, v) -> false);
            clicked = false;
            unitView.setVisible(false);
            clickedShipId = "";
        } else if (!clicked && unitView.isVisible() && !clickedShipId.equals(ship.id())) {
            clicked = true;
            initUnitView();
            unitView.setVisible(true);
            shipTypeClicked.put(clickedShipId, false);
            shipTypeClicked.put(ship.id(), true);
            clickedShipId = ship.id();
        }
        if (shipTypeClicked.get(ship.id())){
            viewTroopButton.setStyle("-fx-border-color: yellow; -fx-border-width: 2px;");
        }else{
            viewTroopButton.setStyle(" ");
        }

        buildFleetComponent.shipsList.refresh();
    }

    private void initUnitView() {
        String shipName = bundle.getString(ship.id());
        buildFleetComponent.unitImage.setImage(imageCache.get("image/ships/" + ship.id() + ".png"));
        buildFleetComponent.unitName.setText(shipName);
        buildFleetComponent.unitScrollPane.setContent(explainedVariableService.buildExplainedVariableToolTip(shipName,
                empire._id(), "ships", ship.id(), ship, List.of("speed", "health", "defense", "attack"), null, true));
    }
}
