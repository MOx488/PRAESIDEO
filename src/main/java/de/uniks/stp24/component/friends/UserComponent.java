package de.uniks.stp24.component.friends;

import de.uniks.stp24.dto.UpdateFriendDto;
import de.uniks.stp24.model.User;
import de.uniks.stp24.rest.FriendsApiService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.TokenStorage;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@Component(view = "User.fxml")
public class UserComponent extends HBox implements ReusableItemComponent<User> {
    @FXML
    ImageView userAvatar;
    @FXML
    Label username;
    @FXML
    ImageView declineImageView;
    @FXML
    ImageView acceptImageView;

    @Inject
    public Subscriber subscriber;
    @Inject
    public TokenStorage tokenStorage;
    @Inject
    public ImageCache imageCache;
    @Inject
    public FriendsApiService friendsApiService;

    @Param("isRequest")
    boolean isRequest;

    private User user;

    @Inject
    public UserComponent() {
    }

    @Override
    public void setItem(@NotNull User user) {
        this.user = user;

        String avatarPath = user.avatar();
        if (avatarPath == null) {
            avatarPath = "image/default_avatar.png";
        }

        if (!isRequest) {
            declineImageView.setVisible(false);
            acceptImageView.setVisible(false);
        }

        userAvatar.setImage(imageCache.get(avatarPath));
        declineImageView.setImage(imageCache.get("image/cross_red.png"));
        acceptImageView.setImage(imageCache.get("image/green_checkmark.png"));
        username.setText(user.name());
    }

    public void declineRequest() {
        declineImageView.setDisable(true);
        subscriber.subscribe(
                friendsApiService.deleteFriendOrRejectFriendRequest(user._id(), tokenStorage.getUserId()),
                result -> declineImageView.setDisable(false),
                error -> declineImageView.setDisable(false)
        );
    }

    public void acceptRequest() {
        acceptImageView.setDisable(true);
        subscriber.subscribe(
                friendsApiService.acceptFriendRequest(
                        user._id(), tokenStorage.getUserId(), new UpdateFriendDto("accepted")
                ),
                result -> acceptImageView.setDisable(false),
                error -> acceptImageView.setDisable(false)
        );
    }
}
