package de.uniks.stp24.component.friends;

import de.uniks.stp24.App;
import de.uniks.stp24.component.popups.AddFriendPopUpComponent;
import de.uniks.stp24.model.Friend;
import de.uniks.stp24.model.User;
import de.uniks.stp24.rest.FriendsApiService;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.constructs.Modals;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.addNewFriendToList;
import static de.uniks.stp24.util.Methods.initListView;

@Component(view = "FriendList.fxml")
public class FriendListComponent extends VBox {
    @FXML
    ListView<User> friendListView;
    @FXML
    Button deleteFriendButton;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    public ImageCache imageCache;
    @Inject
    public FriendsApiService friendsApiService;
    @Inject
    public UsersApiService usersApiService;
    @Inject
    public NotificationService notificationService;
    @Inject
    public Provider<UserComponent> userComponentProvider;
    @Inject
    public Provider<AddFriendPopUpComponent> addFriendPopUpProvider;
    @Inject
    @Resource
    public ResourceBundle bundle;

    private final ObservableList<User> userFriendList = FXCollections.observableArrayList();
    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);

    @Inject
    public FriendListComponent() {
    }

    @OnInit
    public void init() {
        // Dynamically update friend list
        subscriber.subscribe(
                eventListener.listen("users." + tokenStorage.getUserId() + ".friends.*.*", Friend.class),
                this::handleOutgoingFriendEvents
        );

        // Get access to friends
        subscriber.subscribe(friendsApiService.getFriends(tokenStorage.getUserId()), friends -> {
            List<String> friendIds = friends.stream().map(Friend::to).toList();
            getUsers(friendIds);
        });
    }

    @OnRender
    public void render() {
        initListView(friendListView, userFriendList, app, userComponentProvider, Map.of("isRequest", false));

        // Disable delete button if no friend is selected or if a server call is in progress
        final BooleanBinding noFriendSelected = friendListView.getSelectionModel().selectedItemProperty().isNull();
        final BooleanBinding waiting = notWaiting.not();
        deleteFriendButton.disableProperty().bind(noFriendSelected.or(waiting));
    }

    public void deleteFriend() {
        User selectedFriend = friendListView.getSelectionModel().getSelectedItem();

        notWaiting.set(false);
        subscriber.subscribe(
                friendsApiService.deleteFriendOrRejectFriendRequest(tokenStorage.getUserId(), selectedFriend._id()),
                result -> {
                    notificationService.displayNotification(selectedFriend.name() + " " + bundle.getString("success.friend.deleted"), true);
                    notWaiting.set(true);
                },
                error -> notWaiting.set(true)
        );
    }

    public void newFriend() {
        new Modals(app).modal(addFriendPopUpProvider.get())
                .dialog(true)
                .show();
    }

    @OnDestroy
    public void destroy() {
        subscriber.dispose();
    }


    public void getUsers(List<String> friendIds) {
        if (friendIds.isEmpty()) {
            return;
        }

        subscriber.subscribe(usersApiService.getUsersByIDs(friendIds), userFriendList::setAll);
    }

    private void handleOutgoingFriendEvents(Event<Friend> event) {
        switch (event.suffix()) {
            case "created", "updated" -> addNewFriendToList(event.data(), userFriendList, usersApiService, subscriber);
            case "deleted" -> userFriendList.removeIf(user -> user._id().equals(event.data().to()));
        }
    }
}
