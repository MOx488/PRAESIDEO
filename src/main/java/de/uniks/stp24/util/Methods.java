package de.uniks.stp24.util;

import de.uniks.stp24.component.buildings.BuildingPopUpStatComponent;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.dto.ReadShipDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.model.troopview.TroopSizeItem;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.service.ExplainedVariableService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.ws.Event;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.fulib.fx.FulibFxApp;
import org.fulib.fx.constructs.listview.ComponentListCell;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Utility class for various methods. This class is used to avoid code duplication.
 */
public class Methods {
    public static void showNode(Node node, boolean showNode) {
        node.setManaged(showNode);
        node.setVisible(showNode);
    }

    public static Label createLabel(String text, String styleClass) {
        final Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    public static <T, Component extends Parent> void initListView(
            ListView<T> listView, ObservableList<T> items, FulibFxApp app, Provider<? extends Component> provider,
            Map<String, Object> params
    ) {
        listView.setItems(items);
        listView.setCellFactory(list -> new ComponentListCell<>(
                app, provider, params
        ));
    }

    public static <T extends Identifiable> void onBasicListEvent(Event<T> event, ObservableList<T> list) {
        final T object = event.data();
        switch (event.suffix()) {
            case "created" -> list.add(event.data());
            case "updated" -> list.replaceAll(o -> o._id().equals(object._id()) ? object : o);
            case "deleted" -> list.removeIf(o -> o._id().equals(object._id()));
        }
    }

    // ----- Players ---------------------------------------------------------------------------------------------------

    public static void fillPlayerList(List<Member> members, List<User> users, List<ReadEmpireDto> empiresInGame, List<Player> players) {
        for (Member member : members) {
            if (member.empire() == null) continue;

            int flag = member.empire().flag();
            String color = member.empire().color();
            String name = users.stream().filter(user -> user._id().equals(member.user())).findFirst().map(User::name).orElse("");
            String empireId = empiresInGame.stream().filter(readEmpireDto -> readEmpireDto.user().equals(member.user())).findFirst().map(ReadEmpireDto::_id).orElse("");
            int portrait = member.empire().portrait();
            Player player = new Player(
                    member.user(),
                    flag,
                    color,
                    name,
                    empireId,
                    portrait,
                    null,
                    null,
                    null
            );
            players.add(player);
        }
    }

    // ----- Wars ------------------------------------------------------------------------------------------------------

    public static void initWarComponent(War war, ReadEmpireDto enemy, Player player, Label warName, Label enemyName,
                                        ImageView avatarImage, VBox avatarBox, Button reasonButton,
                                        ImageCache imageCache, ResourceBundle bundle
    ) {
        warName.setText(war.name());
        enemyName.setText(enemy.name() + " / " + player.name());
        avatarImage.setImage(imageCache.get("image/portraits/" + enemy.portrait() + ".png"));
        avatarBox.setStyle("-fx-effect: dropshadow(three-pass-box, " + enemy.color() + ", 15, 0, 0, 0);");
        reasonButton.setText(bundle.getString("reason"));
    }

    // ----- Explained Variables ---------------------------------------------------------------------------------------

    public static Consumer<VBox> getDurationConsumer(ResourceBundle bundle, Control tooltipOwner) {
        return (VBox tooltipRoot) -> {
            tooltipRoot.getChildren().add(
                    createLabel(bundle.getString("jobs.action.tooltip"), "small-medium")
            );
            Tooltip tooltip = new Tooltip();
            tooltip.setGraphic(tooltipRoot);
            tooltip.setShowDelay(Duration.seconds(0.5));
            tooltipOwner.setTooltip(tooltip);
        };
    }

    // ----- Buildings & Districts -------------------------------------------------------------------------------------

    public static int getFreeCapacity(GameSystem system, int currentTasksThatIncreaseCapacity) {
        final int totalCapacity = system.capacity();
        final int buildingsSize = system.buildings().size();
        final int districtsSize = system.districts().values().stream().reduce(0, Integer::sum);
        final int totalOccupiedCapacity = buildingsSize + districtsSize;
        return totalCapacity - totalOccupiedCapacity - currentTasksThatIncreaseCapacity;
    }

    // ----- Build Fleet -----------------------------------------------------------------------------------------------

    public static void updateOurCastles(GameSystem previousWebsocketSystem, GameSystem newSystem, Empire empire,
                                        ObservableList<GameSystem> ourCastles, ObservableList<GameSystem> systems,
                                        Runnable populateChoiceBox) {

        //check if last websocket system has same updated at as current -> ignore
        if (newSystem.equals(previousWebsocketSystem)) {
            return;
        }

        if (newSystem.owner() == null || !newSystem.owner().equals(empire._id())) {
            return;
        }

        final GameSystem oldSystem = ourCastles.stream().filter(gameSystem -> gameSystem._id().equals(newSystem._id())).findFirst().orElse(null);
        if (oldSystem == null) {
            //new system is not yet in our list -> add and get name
            final GameSystem systemWithName = systems.stream().filter(gameSystem -> gameSystem._id().equals(newSystem._id())).findFirst().orElse(null);
            ourCastles.add(systemWithName != null ? newSystem.setName(systemWithName.name()) : newSystem);
            if (populateChoiceBox != null) {
                populateChoiceBox.run();
            }
            return;
        }

        //new system is already in our list -> merely update it
        ourCastles.replaceAll(gameSystem -> gameSystem._id().equals(newSystem._id()) ? newSystem.setName(gameSystem.name()) : gameSystem);
    }

    // ----- Contacts --------------------------------------------------------------------------------------------------

    public static void setWarStatus(Player player, ObservableList<War> wars, Empire empire, ImageView warStatusImage, ImageCache imageCache) {
        boolean inWar = wars.stream().anyMatch(war ->
                (war.attacker().equals(player.empireId()) && war.defender().equals(empire._id()))
                        || (war.attacker().equals(empire._id()) && war.defender().equals(player.empireId()))
        );

        warStatusImage.setImage(imageCache.get(inWar ? "image/icons/fight.png" : "image/icons/peace.png"));
    }

    // ----- Troops- and Castlelist ------------------------------------------------------------------------------------

    public static void provideListClickFunctionality(MultipleSelectionModel<?> selectionModel, ListCell<?> cellList,
                                                     ListView<?> listView) {

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        cellList.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    if (cellList.isEmpty()) {
                        return;
                    }
                    final int index = cellList.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index) && event.getClickCount() == 1) {
                        selectionModel.clearSelection(index);
                    } else {
                        selectionModel.select(index);
                    }
                    event.consume();
                }
        );

        cellList.prefWidthProperty().bind(listView.widthProperty().subtract(20));
        cellList.setMaxWidth(Control.USE_PREF_SIZE);
    }

    // ----- Tasks -----------------------------------------------------------------------------------------------------

    public static void onTaskEvent(Event<Job> event, ObservableList<Job> jobs) {
        switch (event.suffix()) {
            case "created" -> {
                if (jobs.stream().anyMatch(j -> j._id().equals(event.data()._id()))) {
                    return;
                }

                jobs.add(event.data());
            }
            case "updated" -> jobs.replaceAll(u -> u._id().equals(event.data()._id()) ? event.data() : u);
            case "deleted" -> jobs.removeIf(u -> u._id().equals(event.data()._id()));
        }
    }

    // ----- Friends ---------------------------------------------------------------------------------------------------

    public static void addNewFriendToList(Friend friend, ObservableList<User> userFriendList, UsersApiService usersApiService, Subscriber subscriber) {
        // Do not add requests to the list
        if (friend.status().equals("requested")) {
            return;
        }

        // Only add users to the list who are not already in the list
        if (userFriendList.stream().anyMatch(user -> user._id().equals(friend.to()))) {
            return;
        }

        subscriber.subscribe(usersApiService.getUser(friend.to()), resultUser -> {
            // This if statement is necessary because two server calls could happen at the same time
            if (userFriendList.stream().noneMatch(user -> user._id().equals(resultUser._id()))) {
                userFriendList.add(resultUser);
            }
        });
    }

    // ----- Troop View ------------------------------------------------------------------------------------------------

    public static void ensureBasicTabResponsiveDesign(AnchorPane root, TabPane tabPane, List<Control> containers) {
        root.prefWidthProperty().bind(tabPane.widthProperty());
        root.prefHeightProperty().bind(tabPane.heightProperty());
        for (Control container : containers) {
            container.prefWidthProperty().bind(root.widthProperty().multiply(0.25));
            container.prefHeightProperty().bind(root.heightProperty().multiply(0.6));
        }
    }

    public static void onTroopUpdate(Fleet troop, Fleet updatedTroop, ObservableList<TroopSizeItem> sizes) {
        // Ignore identical events
        if (updatedTroop.equals(troop)) {
            return;
        }

        // Add items that aren't present in the old sizes
        updatedTroop.size().forEach((type, planned) -> {
            final boolean notInSizes = sizes.stream().map(TroopSizeItem::type).noneMatch(s -> s.equals(type));
            if (notInSizes) {
                sizes.add(new TroopSizeItem(type, 0, planned));
            }
        });

        // Update the rest of the sizes
        sizes.replaceAll(s -> {
            final int planned = updatedTroop.size().getOrDefault(s.type(), 0);
            return new TroopSizeItem(s.type(), s.actual(), planned);
        });

        // Remove items that have been deleted in the updated troop size
        sizes.removeIf(s -> !updatedTroop.size().containsKey(s.type()));

        // Clean up sizes
        sizes.removeIf(s -> s.actual() == 0 && s.planned() == 0);
    }

    public static void onUnitChange(ReadShipDto lastUnitUpdate, Fleet troop, ObservableList<ReadShipDto> units,
                                    ObservableList<TroopSizeItem> sizes, Event<Ship> event) {
        final ReadShipDto unit = event.data().toDto();

        if (troop == null) {
            return;
        }

        // Ignore identical events
        if (lastUnitUpdate != null && lastUnitUpdate.equals(unit)) {
            return;
        }

        // Ignore events that are not related to the troop
        if (!unit.fleet().equals(troop._id()) && units.stream().map(ReadShipDto::_id).noneMatch(s -> s.equals(unit._id()))) {
            return;
        }

        switch (event.suffix()) {
            case "created" -> addUnit(unit, units, sizes);
            case "updated" -> {
                if (!unit.fleet().equals(troop._id())) {
                    // Unit was transferred to other troop
                    units.removeIf(s -> s._id().equals(unit._id()));
                    sizes.replaceAll(s -> s.type().equals(unit.type()) ? new TroopSizeItem(s.type(), s.actual() - 1, s.planned()) : s);
                } else if (units.stream().map(ReadShipDto::_id).noneMatch(s -> s.equals(unit._id()))) {
                    // Unit was added to the troop
                    addUnit(unit, units, sizes);
                } else {
                    units.replaceAll(s -> s._id().equals(unit._id()) ? unit : s);
                }
            }
            case "deleted" -> {
                units.removeIf(s -> s._id().equals(unit._id()));
                sizes.replaceAll(s -> s.type().equals(unit.type()) ? new TroopSizeItem(s.type(), s.actual() - 1, s.planned()) : s);
            }
        }

        // Clean up sizes
        sizes.removeIf(s -> s.actual() == 0 && s.planned() == 0);
    }

    private static void addUnit(ReadShipDto unit, ObservableList<ReadShipDto> units, ObservableList<TroopSizeItem> sizes) {
        units.add(unit);
        final boolean notInSizes = sizes.stream().map(TroopSizeItem::type).noneMatch(s -> s.equals(unit.type()));
        if (notInSizes) {
            sizes.add(new TroopSizeItem(unit.type(), 1, 0));
        } else {
            sizes.replaceAll(s -> s.type().equals(unit.type()) ? new TroopSizeItem(s.type(), s.actual() + 1, s.planned()) : s);
        }
    }

    public static void fillSizes(ObservableList<ReadShipDto> units, Fleet troop, List<ShipType> unitTypeInfo,
                                 ObservableList<TroopSizeItem> sizes) {

        // Compute actual units
        Map<String, Integer> actualUnits = new HashMap<>();
        for (ReadShipDto ship : units) {
            String type = ship.type();
            actualUnits.put(type, actualUnits.getOrDefault(type, 0) + 1);
        }

        // Fill sizes
        for (ShipType unitType : unitTypeInfo) {
            String type = unitType.id();
            int planned = troop.size().getOrDefault(type, 0);
            int actual = actualUnits.getOrDefault(type, 0);

            // Do not add unnecessary information
            if (actual == 0 && planned == 0) {
                continue;
            }

            sizes.add(new TroopSizeItem(type, actual, planned));
        }
    }

    public static ShipType getUnitInfo(List<ShipType> unitTypeInfo, String type) {
        return unitTypeInfo.stream().filter(shipType -> shipType.id().equals(type)).findFirst().orElse(null);
    }

    public static void explainUnitDetails(ScrollPane scrollPane, ExplainedVariableService explainedVariableService,
                                          String localizedObjectName, Fleet troop, List<ShipType> unitTypeInfo,
                                          String type, List<String> properties, SimpleBooleanProperty unitUnlocked) {

        Consumer<VBox> checkIfUnitUnlocked = null;
        if (unitUnlocked != null) {
            // After creating the information, the last HBox of the information features the training time of the unit.
            // If the duration is "0 Days", the unit has not been unlocked yet and the user should not be able to train it.
            checkIfUnitUnlocked = (VBox tooltipRoot) -> {
                HBox durationBox = (HBox) tooltipRoot.getChildren().getLast();
                BuildingPopUpStatComponent durationComponent = (BuildingPopUpStatComponent) durationBox.getChildren().getLast();
                Label durationLabel = (Label) durationComponent.getChildren().getFirst();
                unitUnlocked.set(!durationLabel.getText().contains("0"));
            };
        }

        scrollPane.setContent(explainedVariableService.buildExplainedVariableToolTip(
                localizedObjectName, troop.empire(), "ships", type,
                getUnitInfo(unitTypeInfo, type), properties, checkIfUnitUnlocked, true
        ));
    }

    public static void updateExplainedUnitDetails(ScrollPane scrollPane, ExplainedVariableService explainedVariableService,
                                                  Fleet troop, List<ShipType> unitTypeInfo, String type,
                                                  List<String> properties, Empire lastEmpireUpdate, Empire empire,
                                                  ResourceBundle bundle, SimpleBooleanProperty unitUnlocked) {

        // Ignore identical events and only look at effect updates
        if (lastEmpireUpdate != null && lastEmpireUpdate.effects().equals(empire.effects())) {
            return;
        }

        // Only update the explained variables if a type is selected
        if (type == null) {
            return;
        }

        final String localizedObjectName = bundle.getString(type);

        explainUnitDetails(
                scrollPane, explainedVariableService, localizedObjectName, troop, unitTypeInfo, type, properties,
                unitUnlocked
        );
    }
}
