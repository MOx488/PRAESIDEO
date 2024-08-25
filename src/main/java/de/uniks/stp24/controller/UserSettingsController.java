package de.uniks.stp24.controller;

import de.uniks.stp24.dto.UpdateUserDto;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.constructs.Modals;

import javax.inject.Inject;

@Title("%user.settings.title")
@Controller(view = "UserSettings.fxml")
public class UserSettingsController extends BaseController {
    @FXML
    Text nameErrorText;
    @FXML
    Button deleteAccountButton;
    @FXML
    Button backButton;
    @FXML
    Button updateAndSaveButton;
    @FXML
    PasswordField passwordField;
    @FXML
    Text errorText;
    @FXML
    TextField usernameField;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);

    @OnInit
    void setDiscordActivity() {
        discordActivityService.setActivity(bundle.getString("discord.editing.account"));
    }

    @Inject
    public UserSettingsController() {
    }

    @OnRender
    void ensurePasswordEqual() {
        final BooleanBinding usernameNotEmpty = usernameField.textProperty().isNotEmpty();
        final BooleanBinding passwordLengthValid = passwordField.textProperty().length().greaterThanOrEqualTo(8);
        final BooleanBinding waiting = this.notWaiting.not();

        errorText.textProperty().bind(
                Bindings.when(usernameNotEmpty.not())
                        .then(bundle.getString("error.missing.username"))
                        .otherwise(Bindings.when(passwordLengthValid.not())
                                .then(bundle.getString("error.password.too.short"))
                                .otherwise("")
                        )
        );

        errorText.setVisible(errorText.textProperty().isNotEmpty().get());

        updateAndSaveButton.disableProperty().bind(
                (usernameNotEmpty.and(passwordLengthValid)).not().or(waiting)
        );
    }

    public void updateAndSave() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        notWaiting.set(false);

        subscriber.subscribe(usersApiService.getUser(tokenStorage.getUserId()),
                user -> {
                    String avatarPath = user.avatar();
                    subscriber.subscribe(usersApiService.updateUser(tokenStorage.getUserId(), new UpdateUserDto(username, avatarPath, password)),
                            result -> app.show("/userSettings"),
                            error -> {
                                setErrorText(error.getMessage());
                                notWaiting.set(true);
                            }
                    );
                },
                error -> {
                    setErrorText(error.getMessage());
                    notWaiting.set(true);
                }
        );
    }

    public void backToLobby() {
        app.show("/lobby");
    }

    public void deleteAccount() {
        new Modals(app).modal(deleteAccPopUpComponent.get())
                .dialog(true)
                .show();
    }

    public void setErrorText(String errorMessage) {
        nameErrorText.setVisible(true);
        String errorKey = switch (errorMessage) {
            case "HTTP 400 " -> "error.bad.request";
            case "HTTP 409 " -> "error.username.taken";
            case "HTTP 429 " -> "error.rate.limit.reached";
            default -> "error.unknown";
        };
        nameErrorText.setText(bundle.getString(errorKey));
    }
}
