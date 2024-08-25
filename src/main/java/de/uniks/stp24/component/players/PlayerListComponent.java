package de.uniks.stp24.component.players;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.rest.WarsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.fillPlayerList;
import static de.uniks.stp24.util.Methods.onBasicListEvent;

@Component(view = "PlayerList.fxml")
public class PlayerListComponent extends VBox {
    @FXML
    HBox contactsIcon;
    @FXML
    ImageView playerIcon;
    @FXML
    ListView<Player> playerListView;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public UsersApiService usersApiService;
    @Inject
    public Provider<PlayerComponent> playerComponentProvider;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;
    @Inject
    public Provider<ContactsViewComponent> contactsViewComponentProvider;
    @Inject
    public WarsApiService warsApiService;
    @Inject
    public EventListener eventListener;

    @Param("members")
    List<Member> members;
    @Param("empire")
    Empire empire;
    @Param("game")
    Game game;
    @Param("empires")
    List<ReadEmpireDto> empires;
    @Param("overlapContainer")
    AnchorPane overlapContainer;
    @Param("empiresInGame")
    List<ReadEmpireDto> empiresInGame;
    @Param("sideButtons")
    VBox sideButtons;
    @Param("troopsListContainer")
    VBox troopsListContainer;

    public VBox contactsContainer;
    private final ObservableList<Player> players = FXCollections.observableArrayList();
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<War> wars = FXCollections.observableArrayList();

    @Inject
    public PlayerListComponent() {
    }

    @OnInit
    public void onInit() {
        AnchorPane.setTopAnchor(this, 0.0);
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);

        subscriber.subscribe(warsApiService.getWars(game._id(), empire._id()), this.wars::setAll);

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".wars.*.*", War.class), warEvent ->
                onBasicListEvent(warEvent, wars)
        );
    }

    @OnRender
    public void onRender() {
        // Get a list of users based on the member IDs and fill the player list.
        // Otherwise, we do not have access to the usernames.
        List<String> ids = members.stream().map(Member::user).toList();
        subscriber.subscribe(usersApiService.getUsersByIDs(ids), this::setPlayers);

        playerIcon.setImage(imageCache.get("image/default_avatar.png"));
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }

    private void setPlayers(List<User> users) {
        this.users.setAll(users);
        fillPlayerList(members, users, empiresInGame, players);

        playerListView.setItems(players);
        playerListView.setCellFactory(list -> {
            ListCell<Player> cellList = new ComponentListCell<>(app, playerComponentProvider);
            cellList.prefWidthProperty().bind(playerListView.widthProperty().subtract(20));
            cellList.setMaxWidth(Control.USE_PREF_SIZE);
            return cellList;
        });
    }

    public void clickContactsIcon() {
        if (contactsContainer.getChildren().isEmpty()) {
            contactsIcon.getStyleClass().removeFirst();
            contactsIcon.getStyleClass().add("enhancement-selected");
            contactsContainer.setMouseTransparent(false);
            setLeftSideInvisible(false);
            ContactsViewComponent contactsViewComponent = app.initAndRender(contactsViewComponentProvider.get(),
                    Map.of("empire", empire, "members", members, "players", players, "users", users, "wars", wars, "game", game, "overlapContainer", overlapContainer, "empiresInGame", empiresInGame, "sideButtons", sideButtons, "troopsListContainer", troopsListContainer), subscriber);
            this.contactsContainer.getChildren().add(contactsViewComponent);
            contactsViewComponent.setParentContainer(contactsContainer);
            contactsViewComponent.setParentIcon(contactsIcon);
            overlapContainer.setPickOnBounds(true);
        } else {
            contactsContainer.getChildren().clear();
            contactsContainer.setMouseTransparent(true);
            contactsIcon.getStyleClass().removeFirst();
            contactsIcon.getStyleClass().add("enhancement-not-selected");
            overlapContainer.setPickOnBounds(false);
            setLeftSideInvisible(true);
        }
    }

    private void setLeftSideInvisible(boolean Status) {
        if(!sideButtons.getChildren().isEmpty()) {
            sideButtons.getChildren().forEach(node -> node.setVisible(Status));
        }else{
            this.troopsListContainer.setVisible(Status);
        }
    }

    public void setParentContainer(VBox parentContainer) {
        this.contactsContainer = parentContainer;
    }

}
