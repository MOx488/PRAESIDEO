package de.uniks.stp24.controller;

import de.uniks.stp24.dto.LicensesCreditsDto;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;

import javax.inject.Inject;
import java.io.IOException;

@Controller
@Title("%licenses.and.credits.title")
public class LicensesAndCreditsController extends BaseController {
    @FXML
    ImageView deadBirdsImage;
    @FXML
    Text licensesText;
    @FXML
    Text creditsText;

    @OnInit
    void setDiscordActivity() {
        discordActivityService.setActivity(bundle.getString("discord.licenses.credits"));
    }

    @Inject
    public LicensesAndCreditsController() {
    }

    @OnRender
    public void setTexts() {
        try {
            LicensesCreditsDto licensesCreditsDto = licensesAndCreditsService.getLicensesAndCreditsDto();
            licensesText.setText(licensesAndCreditsService.buildLicensesText(licensesCreditsDto.licenses()));
            creditsText.setText(licensesAndCreditsService.buildDeveloperText(licensesCreditsDto.developers()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        deadBirdsImage.setImage(imageCache.get("image/dead_birds_society_logo_round.png"));
    }


    public void backToLogin() {
        app.show("/login");
    }
}
