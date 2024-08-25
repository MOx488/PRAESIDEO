package de.uniks.stp24.component.troopview;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.ReadShipDto;
import de.uniks.stp24.dto.UpdateShipDto;
import de.uniks.stp24.model.Fleet;
import de.uniks.stp24.model.Ship;
import de.uniks.stp24.model.ShipType;
import de.uniks.stp24.model.troopview.TroopSizeItem;
import de.uniks.stp24.rest.FleetsApiService;
import de.uniks.stp24.rest.ShipsApiService;
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

@Component(view = "TransferUnitsTab.fxml")
public class TransferUnitsTabComponent extends AnchorPane {
    @FXML
    AnchorPane transferUnitsTabRoot;
    @FXML
    ListView<ReadShipDto> yourUnitsListView;
    @FXML
    ListView<Fleet> troopsListView;
    @FXML
    Button transferUnitButton;
    @FXML
    ListView<TroopSizeItem> focusedTroopSizeListView;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public FleetsApiService fleetsApiService;
    @Inject
    public ShipsApiService shipsApiService;
    @Inject
    public NotificationService notificationService;
    @Inject
    public Provider<UnitComponent> unitComponentProvider;
    @Inject
    public Provider<TroopComponent> troopComponentProvider;
    @Inject
    public Provider<TroopSizeComponent> troopSizeComponentProvider;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("tabPane")
    TabPane tabPane;
    @Param("troop")
    Fleet troop;
    @Param("units")
    ObservableList<ReadShipDto> unitsList;
    @Param("unitTypeInfo")
    List<ShipType> unitTypeInfo;

    private final ObservableList<Fleet> troops = FXCollections.observableArrayList();
    private final ObservableList<TroopSizeItem> focusedSizes = FXCollections.observableArrayList();
    private final ObservableList<ReadShipDto> units = FXCollections.observableArrayList();
    private Fleet selectedTroop;
    private ReadShipDto lastUnitUpdate;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);

    private ChangeListener<Fleet> troopsListViewListener;

    @Inject
    public TransferUnitsTabComponent() {
    }

    @OnInit
    void onInit() {
        units.setAll(unitsList);

        // Get all troops except the one of the TroopView
        subscriber.subscribe(fleetsApiService.getFleets(troop.game(), troop.empire()), allTroops ->
                troops.addAll(allTroops.stream().filter(t -> !t._id().equals(troop._id())).toList())
        );

        ensureDynamicInformation();
    }

    @OnRender
    void onRender() {
        ensureBasicTabResponsiveDesign(transferUnitsTabRoot, tabPane, List.of(yourUnitsListView, troopsListView, focusedTroopSizeListView));

        initListView(yourUnitsListView, units, app, unitComponentProvider,
                Map.of("listView", yourUnitsListView, "unitTypeInfo", unitTypeInfo)
        );

        initListView(troopsListView, troops, app, troopComponentProvider, Map.of("listView", yourUnitsListView));

        initListView(focusedTroopSizeListView, focusedSizes, app, troopSizeComponentProvider,
                Map.of("listView", yourUnitsListView)
        );

        prepareTroopInfo();

        // Disable "Transfer" button if we are waiting for a server response or if no unit and/or no troop is selected
        final BooleanBinding waiting = notWaiting.not();
        final BooleanBinding noUnitSelected = yourUnitsListView.getSelectionModel().selectedItemProperty().isNull();
        final BooleanBinding noTroopSelected = troopsListView.getSelectionModel().selectedItemProperty().isNull();
        transferUnitButton.disableProperty().bind(waiting.or(noUnitSelected).or(noTroopSelected));
    }

    public void transferUnit() {
        notWaiting.set(false);

        ReadShipDto selectedUnit = yourUnitsListView.getSelectionModel().getSelectedItem();
        UpdateShipDto updateUnitDto = new UpdateShipDto(selectedTroop._id(), null, null);
        subscriber.subscribe(shipsApiService.updateShip(troop.game(), troop._id(), selectedUnit._id(), updateUnitDto),
                success -> {
                    notWaiting.set(true);
                    notificationService.displayNotification(bundle.getString("success.transferred.unit"), true);
                },
                error -> notWaiting.set(true)
        );
    }

    private void ensureDynamicInformation() {
        // Dynamically update unit list and "planned" sizes
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".fleets.*.ships.*.*", Ship.class),
                this::onUnitChange
        );

        // Dynamically update troop list
        subscriber.subscribe(eventListener.listen("games." + troop.game() + ".fleets.*.*", Fleet.class),
                this::onTroopChange
        );
    }

    private void onUnitChange(Event<Ship> event) {
        final ReadShipDto unit = event.data().toDto();

        // Just update "your units"
        Methods.onUnitChange(lastUnitUpdate, troop, units, FXCollections.observableArrayList(), event);
        // Just update "focused troop"
        Methods.onUnitChange(lastUnitUpdate, selectedTroop, FXCollections.observableArrayList(), focusedSizes, event);

        lastUnitUpdate = unit;
    }

    private void onTroopChange(Event<Fleet> event) {
        final Fleet troop = event.data();
        final boolean focusedSizesChanged = selectedTroop != null && selectedTroop._id().equals(troop._id()) && !selectedTroop.size().equals(troop.size());

        // Ignore troop changes of other empires
        if (!troop.empire().equals(this.troop.empire())) {
            return;
        }

        switch (event.suffix()) {
            case "created" -> troops.add(troop);
            case "updated" -> {
                if (focusedSizesChanged) {
                    onTroopUpdate(selectedTroop, troop, focusedSizes);
                    selectedTroop = troop;
                }
            }
            case "deleted" -> troops.removeIf(t -> t._id().equals(troop._id()));
        }

        if (troop._id().equals(this.troop._id())) {
            this.troop = troop;
        }
    }

    private void prepareTroopInfo() {
        troopsListView.getSelectionModel().selectedItemProperty().addListener(troopsListViewListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            selectedTroop = newValue;

            fillFocusedSizes();
        });
    }

    private void fillFocusedSizes() {
        focusedSizes.clear();
        subscriber.subscribe(shipsApiService.getShips(selectedTroop.game(), selectedTroop._id()), units ->
                unitTypeInfo.stream().map(ShipType::id).forEach(type -> {
                    int actual = (int) units.stream().filter(u -> u.type().equals(type)).count();
                    int planned = selectedTroop.size().getOrDefault(type, 0);

                    // Do not show unnecessary information in the list
                    if (actual == 0 && planned == 0) {
                        return;
                    }

                    focusedSizes.add(new TroopSizeItem(
                            type,
                            actual,
                            planned
                    ));
                })
        );
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
        if (troopsListViewListener != null) {
            troopsListView.getSelectionModel().selectedItemProperty().removeListener(troopsListViewListener);
        }
    }
}
