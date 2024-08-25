package de.uniks.stp24.component.troopview;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.ReadShipDto;
import de.uniks.stp24.dto.UpdateFleetDto;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Fleet;
import de.uniks.stp24.model.Ship;
import de.uniks.stp24.model.ShipType;
import de.uniks.stp24.model.troopview.SizeUpdateItem;
import de.uniks.stp24.model.troopview.TroopSizeItem;
import de.uniks.stp24.rest.FleetsApiService;
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
import javafx.scene.control.*;
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
import java.util.TreeMap;

import static de.uniks.stp24.util.Methods.*;

@Component(view = "UpdateTroopTab.fxml")
public class UpdateTroopTabComponent extends AnchorPane {
    @FXML
    AnchorPane updateTroopTabRoot;
    @FXML
    ListView<TroopSizeItem> updateSizeListView;
    @FXML
    ListView<SizeUpdateItem> updateUnitsListView;
    @FXML
    TextField troopNameTextField;
    @FXML
    Button updateTroopButton;
    @FXML
    ScrollPane updateFocusedUnitScrollPane;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public FleetsApiService fleetsApiService;
    @Inject
    public ExplainedVariableService explainedVariableService;
    @Inject
    public NotificationService notificationService;
    @Inject
    public Provider<TroopSizeComponent> troopSizeComponentProvider;
    @Inject
    public Provider<SizeUpdateComponent> sizeUpdateComponentProvider;
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
    private final ObservableList<SizeUpdateItem> plannedUpdates = FXCollections.observableArrayList();
    private final ObservableList<ReadShipDto> units = FXCollections.observableArrayList();
    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private ReadShipDto lastUnitUpdate;
    private Empire lastEmpireUpdate;

    private ChangeListener<SizeUpdateItem> updateUnitsListViewListener;

    @Inject
    public UpdateTroopTabComponent() {
    }

    @OnInit
    void onInit() {
        ensureDynamicInformation();

        units.setAll(unitsList);
    }

    @OnRender
    void onRender() {
        ensureBasicTabResponsiveDesign(updateTroopTabRoot, tabPane, List.of(updateSizeListView, updateUnitsListView, updateFocusedUnitScrollPane));

        // Init troop size list
        fillSizes(units, troop, unitTypeInfo, sizes);
        initListView(updateSizeListView, sizes, app, troopSizeComponentProvider, Map.of("listView", updateSizeListView));

        // Fill "plan troops" list
        initListView(updateUnitsListView, plannedUpdates, app, sizeUpdateComponentProvider, Map.of("listView", updateSizeListView));
        unitTypeInfo.forEach(unitType -> plannedUpdates.add(new SizeUpdateItem(unitType.id(), troop.size().getOrDefault(unitType.id(), 0), 0)));

        final BooleanBinding waiting = notWaiting.not();
        updateTroopButton.disableProperty().bind(waiting);

        prepareUnitInfo();
    }

    public void updateTroop() {
        notWaiting.set(false);

        final TreeMap<String, Integer> newSize = new TreeMap<>();

        plannedUpdates.forEach(plannedUpdate -> {
            final String type = plannedUpdate.type();
            final int plannedChange = plannedUpdate.amount();
            newSize.put(type, troop.size().getOrDefault(type, 0) + plannedChange);
        });

        String newName = troopNameTextField.getText();

        UpdateFleetDto updateTroopDto = new UpdateFleetDto(
                newName != null && !newName.isBlank() ? newName : troop.name(),
                newSize,
                null,
                null,
                null
        );
        subscriber.subscribe(fleetsApiService.updateFleet(troop.game(), troop._id(), updateTroopDto),
                success -> {
                    notWaiting.set(true);
                    plannedUpdates.replaceAll(u -> new SizeUpdateItem(u.type(), newSize.get(u.type()), 0));
                    notificationService.displayNotification(bundle.getString("success.updated.troop"), true);
                },
                error -> notWaiting.set(true)
        );
    }

    private void prepareUnitInfo() {
        // Fill focused unit information (if a unit is selected)
        updateUnitsListView.getSelectionModel().selectedItemProperty().addListener(updateUnitsListViewListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            final String localizedUnitName = bundle.getString(newValue.type());
            explainUnitDetails(
                    updateFocusedUnitScrollPane, explainedVariableService, localizedUnitName, troop, unitTypeInfo,
                    newValue.type(), List.of("health", "speed", "attack", "defense"), null
            );
        });
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
                    final SizeUpdateItem focusedUnit = updateUnitsListView.getSelectionModel().getSelectedItem();
                    final String type = focusedUnit != null ? focusedUnit.type() : null;
                    updateExplainedUnitDetails(
                            updateFocusedUnitScrollPane, explainedVariableService, troop, unitTypeInfo, type,
                            List.of("health", "speed", "attack", "defense"), lastEmpireUpdate, empire, bundle, null
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
        final ReadShipDto unit = event.data().toDto();
        Methods.onUnitChange(lastUnitUpdate, troop, units, sizes, event);
        lastUnitUpdate = unit;
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
        if (updateUnitsListViewListener != null) {
            updateUnitsListView.getSelectionModel().selectedItemProperty().removeListener(updateUnitsListViewListener);
        }
    }
}
