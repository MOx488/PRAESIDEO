package de.uniks.stp24.component;

import de.uniks.stp24.App;
import de.uniks.stp24.component.troopview.TroopViewComponent;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Fleet;
import de.uniks.stp24.model.GameSystem;
import de.uniks.stp24.rest.FleetsApiService;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.listview.ComponentListCell;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.provideListClickFunctionality;

@Component(view = "TroopsList.fxml")
public class TroopsListComponent extends VBox {
    @FXML
    ListView<Fleet> fleetsListView;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public EventListener eventListener;
    @Inject
    public FleetsApiService fleetsApiService;
    @Inject
    public Provider<TroopsNameComponent> fleetsNameComponentProvider;
    @Inject
    public Provider<TroopViewComponent> troopViewComponentProvider;

    @Param("empire")
    Empire empire;
    @Param("ingameRoot")
    AnchorPane ingameRoot;
    @Param("systems")
    ObservableList<GameSystem> systems;
    @Param("sideBar")
    VBox sideBar;
    @Param("sideButtons")
    VBox sideButtons;

    private final ObservableList<Fleet> observableFleets = FXCollections.observableArrayList();

    private ListChangeListener<Fleet> fleetsListViewListener;

    @Inject
    public TroopsListComponent() {
    }

    @OnInit
    public void init() {
        subscriber.subscribe(fleetsApiService.getFleets(empire.game(), empire._id()), fleets -> {
            this.observableFleets.setAll(fleets);
            fleetsListView.setItems(observableFleets);
        });
        subscriber.subscribe(eventListener.listen("games." + empire.game() + ".fleets.*.*", Fleet.class), this::checkIfOwnFleetUpdated);
    }

    @OnRender
    public void onRender() {
        fleetsListView.getItems().addListener(fleetsListViewListener = change -> setFleets());

        setFleets();

        fleetsListView.setOnMouseClicked((MouseEvent event) -> {
            if (!event.getButton().equals(MouseButton.PRIMARY)) {
                return;
            }

            Fleet selectedTroop = fleetsListView.getSelectionModel().getSelectedItem();

            if (selectedTroop == null || event.getClickCount() != 2) {
                return;
            }

            TroopViewComponent troopView = app.initAndRender(
                    troopViewComponentProvider.get(),
                    Map.of(
                            "troop", selectedTroop, "parent", ingameRoot, "systems", systems,
                            "sideBar", sideBar, "sideButtons", sideButtons, "troopsList", this,
                            "empire", empire
                    ),
                    subscriber
            );
            this.ingameRoot.getChildren().add(troopView);
        });
    }

    private void checkIfOwnFleetUpdated(Event<Fleet> event) {
        Fleet fleet = event.data();
        // if fleet is not own fleet, return
        if (!Objects.equals(fleet.empire(), empire._id())) {
            return;
        }
        // check if event was deleted and remove fleet from observableFleets
        if (event.event().endsWith("deleted")) {
            observableFleets.removeIf(f -> f._id().equals(fleet._id()));
            return;
        }
        // check if fleet with this id is in observableFleets
        Fleet oldFleet = observableFleets.stream().filter(f -> f._id().equals(fleet._id())).findFirst().orElse(null);
        // replace old fleet with new fleet and when old fleet is not in observableFleets, add new fleet
        if (oldFleet != null) {
            observableFleets.set(observableFleets.indexOf(oldFleet), fleet);
        } else {
            observableFleets.add(fleet);
        }
    }

    private void setFleets() {
        fleetsListView.setCellFactory(list -> {
            final ListCell<Fleet> cellList = new ComponentListCell<>(app, fleetsNameComponentProvider);
            final MultipleSelectionModel<Fleet> selectionModel = fleetsListView.getSelectionModel();
            provideListClickFunctionality(selectionModel, cellList, fleetsListView);
            return cellList;
        });
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
        if (fleetsListViewListener != null) {
            fleetsListView.getItems().removeListener(fleetsListViewListener);
        }
    }
}
