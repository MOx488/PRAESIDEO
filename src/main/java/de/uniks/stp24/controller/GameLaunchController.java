package de.uniks.stp24.controller;

import de.uniks.stp24.dto.RefreshDto;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnKey;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;

import javax.inject.Inject;
import java.util.Map;

@Title("%launching.title")
@Controller(view = "GameLaunch.fxml")
public class GameLaunchController extends BaseController {
    @FXML
    Text pressKeyText;
    @FXML
    ImageView deadBirdsImageView;
    @FXML
    ImageView phoenixImageView;

    private Boolean clicked = false;
    private Boolean clientSwitch = false;

    @Param("RefreshToken")
    String refreshToken;
    @Param("GameID")
    String gameID;

    @Inject
    public GameLaunchController() {
    }

    @OnInit
    void onInit() {
        clicked = false;
        if (this.refreshToken != null && this.gameID != null) {
            clientSwitch = true;
        }
    }

    @OnRender
    public void renderImages() {
        deadBirdsImageView.setImage(imageCache.get("image/dead_birds_society_logo_round.png"));
        phoenixImageView.setImage(imageCache.get("image/phoenix_studio_logo_round.png"));
        if (clientSwitch) {
            setClientSwitch();
        }
    }

    private void setClientSwitch() {
        pressKeyText.setText("Client Switch");
        subscriber.subscribe(authApiService.refresh(new RefreshDto(this.refreshToken)),
                loginResult -> {
                    loginService.loginClientSwitch(loginResult);
                    notificationService.displayNotification(bundle.getString("success.login"), true);
                    subscriber.subscribe(gamesApiService.getGameById(this.gameID),
                            game -> app.show("/ingame", Map.of("game", game)),
                            error -> app.show("/login"));
                },
                error -> app.show("/login"));
    }

    @OnKey(type = OnKey.Type.PRESSED)
    public void onKey() {
        if (!clicked && !clientSwitch) {
            clicked = true;
            if (loginService.autoLogin()) {
                notificationService.displayNotification(bundle.getString("success.login"), true);
                app.show("/lobby");
            } else {
                app.show("/login");
            }
        }
    }
}
