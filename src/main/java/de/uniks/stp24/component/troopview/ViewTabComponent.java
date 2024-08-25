package de.uniks.stp24.component.troopview;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.ReadShipDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Fleet;
import de.uniks.stp24.model.Ship;
import de.uniks.stp24.model.ShipType;
import de.uniks.stp24.model.troopview.TroopSizeItem;
import de.uniks.stp24.rest.ShipsApiService;
import de.uniks.stp24.service.ExplainedVariableService;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.util.Methods;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.*;

@Component(view = "ViewTab.fxml")
public class ViewTabComponent extends AnchorPane {
    @FXML
    Button destroyUnitButton;
    @FXML
    AnchorPane viewTabRoot;
    @FXML
    ListView<TroopSizeItem> sizeListView;
    @FXML
    ListView<ReadShipDto> unitsListView;
    @FXML
    ScrollPane focusedUnitScrollPane;

    @Inject
    public App app;
    @Inject
    public Provider<TroopSizeComponent> troopSizeComponentProvider;
    @Inject
    public Provider<UnitComponent> unitComponentProvider;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public ShipsApiService shipsApiService;
    @Inject
    public ExplainedVariableService explainedVariableService;
    @Inject
    public NotificationService notificationService;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("tabPane")
    TabPane tabPane;
    @Param("units")
    ObservableList<ReadShipDto> unitsList;
    @Param("troop")
    Fleet troop;
    @Param("unitTypeInfo")
    List<ShipType> unitTypeInfo;

    private final ObservableList<TroopSizeItem> sizes = FXCollections.observableArrayList();
    private final ObservableList<ReadShipDto> units = FXCollections.observableArrayList();
    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private ReadShipDto lastUnitUpdate;
    private Empire lastEmpireUpdate;

    private ChangeListener<ReadShipDto> unitsListViewListener;

    @Inject
    public ViewTabComponent() {
    }

    @OnInit
    void onInit() {
        ensureDynamicInformation();

        units.setAll(unitsList);
    }

    @OnRender
    void onRender() {
        ensureBasicTabResponsiveDesign(viewTabRoot, tabPane, List.of(sizeListView, unitsListView, focusedUnitScrollPane));

        // Init troop size list
        fillSizes(units, troop, unitTypeInfo, sizes);
        initListView(sizeListView, sizes, app, troopSizeComponentProvider, Map.of("listView", sizeListView));

        // Init unit list
        initListView(unitsListView, units, app, unitComponentProvider,
                Map.of("listView", unitsListView, "unitTypeInfo", unitTypeInfo)
        );

        // Disable delete button if no unit is selected or if a server call is in progress
        final BooleanBinding noUnitSelected = unitsListView.getSelectionModel().selectedItemProperty().isNull();
        final BooleanBinding waiting = notWaiting.not();
        destroyUnitButton.disableProperty().bind(noUnitSelected.or(waiting));

        prepareUnitInfo();
    }

    private void prepareUnitInfo() {
        // Fill focused unit information (if a unit is selected)
        unitsListView.getSelectionModel().selectedItemProperty().addListener(unitsListViewListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            final String localizedUnitName = bundle.getString(newValue.type());
            explainUnitDetails(
                    focusedUnitScrollPane, explainedVariableService, localizedUnitName, troop, unitTypeInfo,
                    newValue.type(), List.of("speed", "attack", "defense"), null
            );
        });
    }

    public void destroyUnit() {
        ReadShipDto selectedUnit = unitsListView.getSelectionModel().getSelectedItem();

        notWaiting.set(false);
        subscriber.subscribe(shipsApiService.deleteShip(troop.game(), troop._id(), selectedUnit._id()),
                success -> {
                    notWaiting.set(true);
                    notificationService.displayNotification(bundle.getString("success.unit.destroyed"), true);
                },
                error -> notWaiting.set(true)
        );
    }

    private void ensureDynamicInformation() {
        // Dynamically update troop size
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".fleets." + troop._id() + ".updated", Fleet.class),
                this::onTroopChange
        );

        // Dynamically update unit list
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".fleets.*.ships.*.*", Ship.class),
                this::onUnitChange
        );

        // Update explained variables
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".empires." + troop.empire() + ".updated", Empire.class),
                event -> {
                    Empire empire = event.data();
                    final ReadShipDto focusedUnit = unitsListView.getSelectionModel().getSelectedItem();
                    final String type = focusedUnit != null ? focusedUnit.type() : null;
                    updateExplainedUnitDetails(
                            focusedUnitScrollPane, explainedVariableService, troop, unitTypeInfo, type,
                            List.of("health", "speed", "attack", "defense", "cost", "upkeep"), lastEmpireUpdate,
                            empire, bundle, null
                    );
                    lastEmpireUpdate = empire;
                }
        );
    }

    private void onTroopChange(Event<Fleet> event) {
        final Fleet updatedTroop = event.data();
        onTroopUpdate(troop, updatedTroop, sizes);
        troop = updatedTroop;
    }

    private void onUnitChange(Event<Ship> event) {
        final Ship updatedUnit = event.data();
        Methods.onUnitChange(lastUnitUpdate, troop, units, sizes, event);
        lastUnitUpdate = updatedUnit.toDto();
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
        if (unitsListViewListener != null) {
            unitsListView.getSelectionModel().selectedItemProperty().removeListener(unitsListViewListener);
        }
    }
}
