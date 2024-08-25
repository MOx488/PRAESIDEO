package de.uniks.stp24.component;

import de.uniks.stp24.model.Member;
import de.uniks.stp24.rest.GamesApiService;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.service.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.constructs.ReusableItemComponent;
import org.fulib.fx.controller.Subscriber;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@Component(view = "Member.fxml")
public class MemberComponent extends HBox implements ReusableItemComponent<Member> {
    @Inject
    public UsersApiService usersApiService;
    @Inject
    public GamesApiService gamesApiService;
    @Inject
    public Subscriber subscriber;
    @FXML
    Label playerName;
    @FXML
    ImageView memberStatusImage;
    @FXML
    ImageView playerAvatar;
    @Inject
    public ImageCache imageCache;

    public static String ownerId = "";

    @Inject
    public MemberComponent() {

    }

    @Override
    public void setItem(@NotNull Member member) {
        subscriber.subscribe(usersApiService.getUser(member.user()), user -> {
            playerName.setText(user.name());

            String avatarPath = user.avatar();
            if (avatarPath == null) {
                avatarPath = "image/default_avatar.png";
            }

            playerAvatar.setImage(imageCache.get(avatarPath));
        });

        if (MemberComponent.ownerId.isEmpty() || MemberComponent.ownerId.equals(member.user())) {
            memberStatusImage.setImage(imageCache.get("image/owner_crown.png"));
            MemberComponent.ownerId = member.user();
            return;
        }

        //has to be a member
        if (member.ready()) {
            memberStatusImage.setImage(imageCache.get("image/green_checkmark.png"));
        } else {
            memberStatusImage.setImage(null);
        }
    }

    @OnDestroy
    public void onDestroy() {
        subscriber.dispose();
    }
}
