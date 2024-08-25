package de.uniks.stp24.component.friends;

import de.uniks.stp24.App;
import de.uniks.stp24.model.Friend;
import de.uniks.stp24.model.User;
import de.uniks.stp24.rest.FriendsApiService;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.Event;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.constructs.listview.ComponentListCell;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "FriendRequest.fxml")
public class FriendRequestComponent extends AnchorPane {
    @FXML
    ListView<User> requestsListView;
    @FXML
    ImageView exclamationImageView;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public EventListener eventListener;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    public FriendsApiService friendsApiService;
    @Inject
    public UsersApiService usersApiService;
    @Inject
    public ImageCache imageCache;
    @Inject
    public Provider<UserComponent> userComponentProvider;
    @Inject
    @Resource
    public ResourceBundle bundle;

    private final ObservableList<User> requests = FXCollections.observableArrayList();

    private ListChangeListener<User> requestsListener;

    @Inject
    public FriendRequestComponent() {
    }

    @OnInit
    public void init() {
        // Dynamically update request list
        subscriber.subscribe(
                eventListener.listen("users.*.friends." + tokenStorage.getUserId() + ".*", Friend.class),
                this::handleFriendEvent
        );

        // Get access to open requests
        subscriber.subscribe(friendsApiService.getFriendsByStatus(tokenStorage.getUserId(), "requested"),
                friends -> {
                    List<String> friendIds = friends.stream()
                            .map(Friend::from)
                            .filter(id -> !id.equals(tokenStorage.getUserId()))
                            .toList();
                    getUsers(friendIds);
                }
        );
    }

    @OnRender
    public void onRender() {
        // Fill request list
        requestsListView.setItems(requests);
        requestsListView.setCellFactory(list -> {
            ListCell<User> cellList = new ComponentListCell<>(
                    app, userComponentProvider, Map.of("isRequest", true)
            );
            cellList.prefWidthProperty().bind(requestsListView.widthProperty().subtract(20));
            cellList.setMaxWidth(Control.USE_PREF_SIZE);
            return cellList;
        });

        // Show exclamation mark if the user has open requests
        exclamationImageView.setImage(imageCache.get("image/exclamation.png"));
        exclamationImageView.setVisible(!requests.isEmpty());
        requests.addListener(requestsListener = change -> exclamationImageView.setVisible(!requests.isEmpty()));
    }

    private void getUsers(List<String> friendIds) {
        if (friendIds.isEmpty()) {
            return;
        }

        subscriber.subscribe(usersApiService.getUsersByIDs(friendIds), requests::setAll);
    }

    private void handleFriendEvent(Event<Friend> event) {
        switch (event.suffix()) {
            case "created" -> addNewRequest(event.data());
            case "updated" -> {
                if (event.data().status().equals("accepted")) {
                    requests.removeIf(user -> user._id().equals(event.data().from()));
                }
            }
            case "deleted" -> requests.removeIf(user -> user._id().equals(event.data().from()));
        }
    }

    private void addNewRequest(Friend friend) {
        // Only add users to the list whose request has not been accepted yet
        if (friend.status().equals("accepted")) {
            return;
        }

        // Do not add the same user twice
        if (requests.stream().anyMatch(user -> user._id().equals(friend.from()))) {
            return;
        }

        subscriber.subscribe(usersApiService.getUser(friend.from()), resultUser -> {
            // This if statement is necessary because two server calls could happen at the same time
            if (requests.stream().noneMatch(user -> user._id().equals(resultUser._id()))) {
                requests.add(resultUser);
            }
        });
    }

    @OnDestroy
    void onDestroy() {
        subscriber.dispose();
        if (requestsListener != null) {
            requests.removeListener(requestsListener);
        }
    }
}
