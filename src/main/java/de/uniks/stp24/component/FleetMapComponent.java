package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.component.troopview.TroopViewComponent;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Fleet;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.rest.JobsApiService;
import de.uniks.stp24.service.ImageCache;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Constants.*;
import static de.uniks.stp24.util.Methods.showNode;

@Component(view = "FleetMap.fxml")
public class FleetMapComponent extends SplitPane {
    @Inject
    public ImageCache imageCache;
    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public ResourceBundle bundle;
    @Inject
    public Provider<TroopViewComponent> troopViewComponentProvider;
    @Inject
    public JobsApiService jobsApiService;

    @FXML
    Tooltip fleetTooltip;
    @FXML
    Circle empireCircle;
    @FXML
    Label enemyName;
    @FXML
    Label fleetName;
    @FXML
    HBox enemyBox;
    @FXML
    SplitPane imageBox;
    @FXML
    ImageView fleetImage;

    @Param("fleet")
    Fleet fleet;
    @Param("alignment")
    String alignment;
    @Param("empires")
    List<ReadEmpireDto> empires;
    @Param("ingameRoot")
    AnchorPane ingameRoot;
    @Param("systems")
    ObservableList<GameSystem> systems;
    @Param("sideBar")
    VBox sideBar;
    @Param("sideButtons")
    VBox sideButtons;
    @Param("troopsList")
    VBox troopsList;
    @Param("parent")
    CastleComponent parent;
    @Param("jobInfo")
    ArrayList<String> jobInfo;
    @Param("empire")
    Empire empire;

    // 0 = unselected, 1 = selected, 2 = travelling
    int selected = 0;

    @Inject
    public FleetMapComponent() {
    }

    @OnRender
    public void onRender() {
        if (alignment.equals(COLOR_ORANGE)) {
            selected = 2;
        }

        fleetImage.setImage(imageCache.get("image/icons/fleet.png"));
        refreshDesign();

        if (fleet == null) {
            fleetName.setText(bundle.getString("map.zoom.in"));
            showNode(enemyBox, false);
            return;
        }

        fleetName.setText(fleet.name());
        if (!alignment.equals(COLOR_RED)) {
            showNode(enemyBox, false);
        } else {
            String empireColor;
            for (ReadEmpireDto empire : empires) {
                if (empire._id().equals(fleet.empire())) {
                    empireColor = empire.color();
                    empireCircle.setStyle("-fx-fill: " + empireColor + ";");
                    enemyName.setText(empire.name() + ":");
                }
            }
        }

        fleetImage.setOnMouseClicked((MouseEvent event) -> {
            if (!event.getButton().equals(MouseButton.PRIMARY) || !(this.alignment.equals(COLOR_GREEN) || this.alignment.equals(COLOR_ORANGE))) {
                return;
            }
            if (event.getClickCount() == 2) {
                handleDoubleClick();
            } else if (event.getClickCount() == 1) {
                handleSingleClick();
            }
        });
    }

    private void handleSingleClick() {
        switch (selected) {
            case 0:
                this.select();
                break;
            case 1:
                this.deselect();
                break;
            case 2:
                subscriber.subscribe(jobsApiService.deleteJob(fleet.game(), fleet.empire(), jobInfo.get(1)));
        }
    }

    private void refreshDesign() {
        if (!alignment.equals("#transparent")) {
            imageBox.setStyle("-fx-border-width: 2; -fx-border-color: " + alignment + ";");
        } else {
            imageBox.setStyle("-fx-border-width: 2; -fx-border-color: transparent;");
        }
    }

    public void deselect() {
        selected = 0;
        parent.deselect();
        refreshDesign();
    }

    private void select() {
        selected = 1;
        parent.select(this);
        imageBox.setStyle("-fx-border-width: 2; -fx-border-color:" + COLOR_YELLOW + ";");
    }

    private void handleDoubleClick() {
        TroopViewComponent troopView = app.initAndRender(
                troopViewComponentProvider.get(),
                Map.of("troop", this.fleet, "parent", ingameRoot, "systems", systems,
                        "sideBar", sideBar, "sideButtons", sideButtons, "troopsList", troopsList,
                        "empire", empire),
                subscriber
        );
        this.ingameRoot.getChildren().add(troopView);
    }

    public void setTravelling() {
        imageBox.setStyle("-fx-border-width: 2; -fx-border-color:" + COLOR_ORANGE + ";");
        selected = 2;
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }
}


