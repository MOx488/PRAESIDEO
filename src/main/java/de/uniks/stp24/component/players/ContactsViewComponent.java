package de.uniks.stp24.component.players;

import de.uniks.stp24.App;
import de.uniks.stp24.component.CloseableView;
import de.uniks.stp24.dto.ReadEmpireDto;
import de.uniks.stp24.dto.UpdateEmpireDto;
import de.uniks.stp24.model.*;
import de.uniks.stp24.rest.GameEmpiresApiService;
import de.uniks.stp24.rest.GameLogicApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

import static de.uniks.stp24.util.Methods.initListView;
import static de.uniks.stp24.util.Methods.setWarStatus;

@Component(view = "ContactsView.fxml")
public class ContactsViewComponent extends AnchorPane implements CloseableView {
    @FXML
    Button sendButton;
    @FXML
    ListView<VBox> emojiList;
    @FXML
    AnchorPane contactsRoot;
    @FXML
    VBox contactsAvatarContainer;
    @FXML
    ImageView contactsAvatar;
    @FXML
    Label playerName;
    @FXML
    ImageView contactsFlagImage;
    @FXML
    ImageView contactsWarStatusImage;
    @FXML
    Label militaryPower;
    @FXML
    Label economyPower;
    @FXML
    Label technologyLevel;
    @FXML
    ListView<Player> contactsList;
    @FXML
    Button contactsViewBackButton;
    @FXML
    ImageView contactsViewBackImage;

    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public ImageCache imageCache;
    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public Provider<ContactsListComponent> contactsListComponentProvider;
    @Inject
    public GameEmpiresApiService gameEmpiresApiService;
    @Inject
    public EventListener eventListener;
    @Inject
    public GameLogicApiService gameLogicApiService;
    @Inject
    public NotificationService notificationService;

    @Param("members")
    List<Member> members;
    @Param("players")
    ObservableList<Player> players;
    @Param("empire")
    Empire empire;
    @Param("users")
    ObservableList<User> users;
    @Param("wars")
    ObservableList<War> wars;
    @Param("systems")
    ObservableList<GameSystem> systems;
    @Param("game")
    Game game;
    @Param("overlapContainer")
    AnchorPane overlapContainer;
    @Param("empiresInGame")
    List<ReadEmpireDto> empiresInGame;
    @Param("sideButtons")
    VBox sideButtons;
    @Param("troopsListContainer")
    VBox troopsListContainer;

    public VBox contactsContainer;
    public HBox contactsIcon;
    public int playerCounter = 0;
    public int playersSize = 0;
    public final List<String> emojiNames = Arrays.asList("slightly_smiling_face", "pouting_face", "face_with_tears_of_joy", "skull", "pensive_face");

    private final SimpleBooleanProperty nothingSelected = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty ownPlayer = new SimpleBooleanProperty(true);

    private ChangeListener<Player> contactsListListener;
    private ChangeListener<VBox> emojiListListener;

    @Inject
    public ContactsViewComponent() {
    }

    @OnInit
    public void init() {
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".wars.*.*", War.class), warEvent -> {
            initializeContactsList();
            initializeUserInfo();
        });

        getStats();
    }

    @OnRender
    public void render() {
        initializeBackButton();
        initializeEmojiList();

        contactsList.getSelectionModel().selectedItemProperty().addListener(contactsListListener = (observable, oldValue, newValue) -> {
            initializeUserInfo();
            buttonBinding();
        });

        emojiList.getSelectionModel().selectedItemProperty().addListener(emojiListListener = (observable, oldValue, newValue) ->
                buttonBinding()
        );

        contactsViewBackButton.setOnAction(event -> closeView());
    }

    private void buttonBinding() {
        // disable the Send Button when no emoji selected and the own player selected
        nothingSelected.set(emojiList.getSelectionModel().getSelectedItem() == null);
        ownPlayer.set(Optional.ofNullable(contactsList.getSelectionModel().getSelectedItem())
                .map(player -> player.empireId().equals(empire._id()))
                .orElse(false));

        sendButton.disableProperty().bind(nothingSelected.or(ownPlayer));
    }

    private void getStats() {
        // get stats from military, economy and technology from the server in comparison to the own empire
        // load the information into the view when calculated all stats and show view
        playersSize = players.size();
        playerCounter = 0;

        for (Player player : players) {
            subscriber.subscribe(gameLogicApiService.getAggregateCompare(game._id(), empire._id(), "empire.compare.military", player.empireId()), military ->
                    subscriber.subscribe(gameLogicApiService.getAggregateCompare(game._id(), empire._id(), "empire.compare.economy", player.empireId()), economy ->
                            subscriber.subscribe(gameLogicApiService.getAggregateCompare(game._id(), empire._id(), "empire.compare.technology", player.empireId()), technology -> {
                                updatePlayer(player, military.total(), economy.total(), technology.total());
                                playerCounter++;
                                if (playersSize == playerCounter) {
                                    initializeContactsList();
                                    contactsList.getSelectionModel().selectFirst();
                                    initializeUserInfo();
                                    contactsRoot.setVisible(true);
                                    buttonBinding();
                                }
                            })
                    )
            );
        }
    }

    private void updatePlayer(Player player, double military, double economy, double technology) {
        // update the players with military, economy and technology in comparison to the own empire
        Player updatedPlayer = new Player(player._id(), player.flag(), player.color(), player.name(), player.empireId(), player.portrait(), calculateStat(military), calculateStat(economy), calculateStat(technology));
        players.set(players.indexOf(player), updatedPlayer);
    }

    private String calculateStat(double stat) {
        // calculate the stats of military, economy and technology in comparison to the own empire
        if (stat < -1.25) {
            return "pathetic";
        }
        if (stat >= -1.25 && stat < -0.25) {
            return "inferior";
        }
        if (stat >= -0.25 && stat <= 0.25) {
            return "equal";
        }
        if (stat > 0.25 && stat <= 1.25) {
            return "superior";
        }
        if (stat > 1.25) {
            return "overwhelming";
        }
        return null;
    }

    private void initializeContactsList() {
        // load List with the Players
        initListView(contactsList, players, app, contactsListComponentProvider,
                Map.of("wars", wars, "empire", empire, "game", game, "empiresInGame", empiresInGame)
        );
    }

    private void initializeUserInfo() {
        // Show information about the selected User
        Player selectedPlayer = contactsList.getSelectionModel().getSelectedItem();
        if (selectedPlayer == null) return;
        this.contactsAvatarContainer.setStyle("-fx-effect: dropshadow(three-pass-box, " + selectedPlayer.color() + ", 15, 0, 0, 0);");
        this.contactsAvatar.setImage(imageCache.get("image/portraits/" + selectedPlayer.portrait() + ".png"));
        playerName.setText(selectedPlayer.name());
        contactsFlagImage.setImage(imageCache.get("image/flags/" + selectedPlayer.flag() + ".png"));
        setText(militaryPower, selectedPlayer.military());
        setText(economyPower, selectedPlayer.economy());
        setText(technologyLevel, selectedPlayer.technology());

        setWarStatus(selectedPlayer, wars, empire, contactsWarStatusImage, imageCache);
    }

    public void setText(Label label, String type) {
        label.setText(bundle.getString(type));
        while (label.getStyleClass().size() > 2) {
            label.getStyleClass().removeLast();
        }
        label.getStyleClass().add(type);
    }

    private void initializeEmojiList() {
        // load List with Images of the Emojis
        for (String emoji : emojiNames) {
            VBox vBox = new VBox();
            vBox.setAlignment(Pos.CENTER);
            ImageView emojiImage = new ImageView();
            emojiImage.setImage(imageCache.get("image/emojis/" + emoji + ".png"));
            emojiImage.setFitHeight(40);
            emojiImage.setFitWidth(40);
            vBox.getChildren().add(emojiImage);
            vBox.getStyleClass().add("vBox-emoji");
            vBox.setPrefHeight(50);
            vBox.setPrefWidth(50);
            emojiList.getItems().add(vBox);
        }
    }

    public void sendEmoji() {
        // send the emoji by saving it in _public with <receiver empire._id, emoji number>
        subscriber.subscribe(gameEmpiresApiService.getEmpire(game._id(), empire._id()), ownEmpire -> {
            Map<String, Object> _public = Optional.ofNullable(ownEmpire._public()).orElse(new HashMap<>());
            HashMap<String, Integer> emojiSend = (HashMap<String, Integer>) _public.getOrDefault("emojiSend", new HashMap<String, Integer>());

            emojiSend.put(contactsList.getSelectionModel().getSelectedItem().empireId(), emojiList.getSelectionModel().getSelectedIndex());
            _public.put("emojiSend", emojiSend);

            subscriber.subscribe(gameEmpiresApiService.updateEmpire(game._id(), empire._id(), new UpdateEmpireDto(null, null, null, null, _public)), result ->
                    notificationService.displayNotification(bundle.getString("success.emoji.send"), true)
            );
        });
    }

    private void initializeBackButton() {
        this.contactsViewBackImage.setImage(imageCache.get("image/cross_red.png"));
    }

    @Override
    public void closeView() {
        contactsIcon.getStyleClass().removeFirst();
        contactsIcon.getStyleClass().add("enhancement-not-selected");
        contactsContainer.setMouseTransparent(true);
        contactsContainer.getChildren().clear();
        overlapContainer.setPickOnBounds(false);
        setLeftSideInvisible();
    }

    public void setParentContainer(VBox parentContainer) {
        this.contactsContainer = parentContainer;
    }

    public void setParentIcon(HBox parentIcon) {
        this.contactsIcon = parentIcon;
    }

    private void setLeftSideInvisible() {
        if (!this.sideButtons.getChildren().isEmpty()) {
            this.sideButtons.getChildren().forEach(node -> node.setVisible(true));
        }else{
            this.troopsListContainer.setVisible(true);
        }
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
        if (contactsListListener != null) {
            contactsList.getSelectionModel().selectedItemProperty().removeListener(contactsListListener);
        }
        if (emojiListListener != null) {
            emojiList.getSelectionModel().selectedItemProperty().removeListener(emojiListListener);
        }
    }
}
