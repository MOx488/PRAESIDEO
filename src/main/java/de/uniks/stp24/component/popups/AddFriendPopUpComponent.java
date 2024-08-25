package de.uniks.stp24.component.popups;

import de.uniks.stp24.App;
import de.uniks.stp24.model.User;
import de.uniks.stp24.rest.FriendsApiService;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.NotificationService;
import de.uniks.stp24.service.TokenStorage;
import de.uniks.stp24.ws.EventListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import java.util.ResourceBundle;

import static de.uniks.stp24.util.Methods.onBasicListEvent;

@Component(view = "AddFriendPopUp.fxml")
public class AddFriendPopUpComponent extends VBox {
    @FXML
    TextField friendNameField;
    @FXML
    Button sendRequestButton;

    @Inject
    public App app;
    @Inject
    public Subscriber subscriber;
    @Inject
    public ImageCache imageCache;
    @Inject
    public FriendsApiService friendsApiService;
    @Inject
    public UsersApiService usersApiService;
    @Inject
    public EventListener eventListener;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    @Resource
    public ResourceBundle bundle;
    @Inject
    public NotificationService notificationService;


    @Param("modalStage")
    public Stage modal;

    private final ObservableList<User> users = FXCollections.observableArrayList();

    @Inject
    public AddFriendPopUpComponent() {
    }

    @OnInit
    public void init() {
        // Load list of users
        subscriber.subscribe(usersApiService.getUsers(), this.users::addAll);

        // Dynamically update user list in case something changes while this pop up is open
        subscriber.subscribe(eventListener.listen("users.*.*", User.class), event ->
                onBasicListEvent(event, users)
        );
    }

    public void cancel() {
        modal.close();
    }

    public void sendRequest() {
        String friendName = friendNameField.getText();
        User friend = users.stream().filter(user -> user.name().equals(friendName)).findFirst().orElse(null);

        // Friend username does not exist. Show error message
        if (friend == null) {
            notificationService.displayNotification(bundle.getString("error.user.not.found"), true);
            return;
        }

        // Cannot befriend yourself. Show error message
        if (friend._id().equals(tokenStorage.getUserId())) {
            notificationService.displayNotification(bundle.getString("error.cannot.befriend.yourself"), false);
            return;
        }

        // Friend username exists and has not been added yet. Send friend request
        sendRequestButton.setDisable(true);
        subscriber.subscribe(friendsApiService.createFriendRequest(tokenStorage.getUserId(), friend._id()),
                result -> {
                    notificationService.displayNotification(bundle.getString("success.friend.request.sent"), true);
                    sendRequestButton.setDisable(false);
                    modal.close();
                },
                error -> sendRequestButton.setDisable(false)
        );
    }

    @OnDestroy
    public void destroy() {
        subscriber.dispose();
    }
}
