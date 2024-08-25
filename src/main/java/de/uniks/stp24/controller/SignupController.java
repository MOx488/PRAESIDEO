package de.uniks.stp24.controller;

import de.uniks.stp24.dto.CreateUserDto;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;

import javax.inject.Inject;
import java.util.Map;

@Controller(view = "Signup.fxml")
@Title("%signup.title")
public class SignupController extends BaseController {
    @FXML
    Label nameError;
    @FXML
    TextField usernameInput;
    @FXML
    PasswordField passwordInput;
    @FXML
    PasswordField confirmPasswordInput;
    @FXML
    Button backButton;
    @FXML
    Button signupButton;
    @FXML
    Label errorMessageLabel;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);

    @OnInit
    void setDiscordActivity() {
        discordActivityService.setActivity(bundle.getString("discord.signing.up"), "");
    }

    @Inject
    public SignupController() {
    }

    @OnRender
    void applyInput() {
        usernameInput.setText(username);
        passwordInput.setText(password);
    }

    @OnRender
    void ensurePasswordEqual() {
        final BooleanBinding equalPasswords = passwordInput.textProperty()
                .isEqualTo(confirmPasswordInput.textProperty());
        final BooleanBinding usernameNotEmpty = usernameInput.textProperty().isNotEmpty();
        final BooleanBinding passwordLengthValid = passwordInput.textProperty().length().greaterThanOrEqualTo(8);
        final BooleanBinding waiting = this.notWaiting.not();
        final BooleanBinding usernameLess32 = usernameInput.textProperty().length().lessThanOrEqualTo(32);

        errorMessageLabel.textProperty().bind(
                Bindings.when(equalPasswords.not())
                        .then(bundle.getString("error.passwords.unequal"))
                        .otherwise(Bindings.when(usernameLess32.not())
                                .then(bundle.getString("error.name.too.long"))
                                .otherwise(Bindings.when(usernameNotEmpty.not())
                                        .then(bundle.getString("error.missing.username"))
                                        .otherwise(Bindings.when(passwordLengthValid.not())
                                                .then(bundle.getString("error.password.too.short"))
                                                .otherwise("")
                                        )
                                )
                        )
        );

        signupButton.disableProperty().bind(
                equalPasswords.not().or(usernameNotEmpty.not()).or(passwordLengthValid.not()).or(waiting).or(usernameLess32.not())
        );
    }

    public void backToLogin() {
        app.show("/login");
    }

    public void signup() {
        notWaiting.set(false);

        String username = usernameInput.getText();
        String password = passwordInput.getText();

        subscriber.subscribe(usersApiService.createUser(new CreateUserDto(username, password)),
                result -> app.show("/login", Map.of("username", username, "password", password)),
                error -> {
                    nameError.setVisible(true);
                    switch (error.getMessage()) {
                        case "HTTP 400 " -> nameError.setText(bundle.getString("error.bad.request"));
                        case "HTTP 409 " -> nameError.setText(bundle.getString("error.username.taken"));
                        case "HTTP 429 " -> nameError.setText(bundle.getString("error.rate.limit.reached"));
                    }
                    notWaiting.set(true);
                }
        );
    }
}
