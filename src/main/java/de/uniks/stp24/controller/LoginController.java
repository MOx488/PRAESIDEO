package de.uniks.stp24.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;

import javax.inject.Inject;
import java.awt.*;
import java.util.Locale;
import java.util.Map;

@Controller
@Title("%login.title")
public class LoginController extends BaseController {
    @FXML
    ImageView languageEnImage;
    @FXML
    ImageView languageDeImage;
    @FXML
    VBox infoBox;
    @FXML
    ToggleButton setEnButton;
    @FXML
    ToggleButton setDeButton;
    @FXML
    Text accDeletedText;
    @FXML
    Button signupButton;
    @FXML
    Button loginButton;
    @FXML
    Label usernameLabel;
    @FXML
    Label passwordLabel;
    @FXML
    Label languageLabel;
    @FXML
    TextField usernameField;
    @FXML
    PasswordField passwordField;
    @FXML
    CheckBox rememberCheckbox;
    @FXML
    Text errorText;
    @FXML
    ImageView infoImage;
    @FXML
    ToggleGroup languageGroup;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private final SimpleStringProperty usernameProperty = new SimpleStringProperty();
    private final SimpleStringProperty passwordProperty = new SimpleStringProperty();

    private ChangeListener<Toggle> languageGroupListener;

    @Inject
    public LoginController() {
    }

    @OnInit
    void setDiscordActivity() {
        discordActivityService.setActivity(bundle.getString("discord.logging.in"));
        if (prefService.getLocale() != null) {
            Locale.setDefault(prefService.getLocale());
        }
        if (!GraphicsEnvironment.isHeadless()) {
            if (audioService.isPlayed) {
                audioService.stopSound();
                audioService.isPlayed = false;
            }
        }

    }

    @OnRender
    void onRender() {
        // Use SimpleStringProperties so that texts survive reloading
        usernameField.textProperty().bindBidirectional(usernameProperty);
        passwordField.textProperty().bindBidirectional(passwordProperty);

        // Pre-fill information
        if (username != null) usernameField.setText(username);
        if (password != null) passwordField.setText(password);
        infoImage.setImage(imageCache.get("image/exclamation.png"));

        if (accDeleted != null && accDeleted) {
            accDeletedText.setVisible(true);
            accDeletedText.setText(bundle.getString("account.deleted"));
        } else {
            accDeletedText.setVisible(false);
        }

        languageGroup.selectedToggleProperty().addListener(languageGroupListener = (observable, oldValue, newValue) -> {
            if (oldValue == null) {
                return;
            }

            if (newValue == null) {
                languageGroup.selectToggle(oldValue);
                return;
            }

            if (newValue == setDeButton) {
                setLanguage(Locale.GERMAN);
            } else {
                setLanguage(Locale.ENGLISH);
            }
        });

        languageGroup.selectToggle(prefService.getLocale() == Locale.GERMAN ? setDeButton : setEnButton);
        languageDeImage.setImage(imageCache.get("image/germany.png"));
        languageEnImage.setImage(imageCache.get("image/united-kingdom.png"));
    }

    @OnRender
    public void bindLogin() {
        final BooleanBinding usernameNotEmpty = usernameField.textProperty().isNotEmpty();
        final BooleanBinding passwordLengthValid = passwordField.textProperty().length().greaterThanOrEqualTo(8);
        final BooleanBinding usernameLessThirtyTwo = usernameField.textProperty().length().lessThanOrEqualTo(32);
        final BooleanBinding waiting = this.notWaiting.not();

        errorText.textProperty().bind(
                Bindings.when(usernameLessThirtyTwo.not())
                        .then(bundle.getString("error.name.too.long"))
                        .otherwise(Bindings.when(usernameNotEmpty.not())
                                .then(bundle.getString("error.missing.username"))
                                .otherwise(Bindings.when(passwordLengthValid.not())
                                        .then(bundle.getString("error.password.too.short"))
                                        .otherwise("")
                                )
                        )
        );

        loginButton.disableProperty().bind(
                (passwordLengthValid.and(usernameLessThirtyTwo).and(usernameNotEmpty)).not().or(waiting)
        );
    }

    public void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean rememberMe = rememberCheckbox.isSelected();

        notWaiting.set(false);

        subscriber.subscribe(loginService.login(username, password, rememberMe),
                result -> {
                    accDeleted = false;
                    notificationService.displayNotification(bundle.getString("success.login"), true);
                    app.show("/lobby");
                },
                error -> notWaiting.set(true)
        );
    }

    public void signup() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
        accDeleted = false;
        app.show("/signup", Map.of("username", username, "password", password));
    }

    public void licensesAndCredits() {
        accDeleted = false;
        app.show("/licenses-and-credits");
    }

    private void setLanguage(Locale locale) {
        if (prefService.getLocale() == locale) {
            return;
        }

        prefService.setLocale(locale);
        bundle = bundleProvider.get();
        errorService.setBundle(bundle);
        notificationService.setBundle(bundle);
        app.refresh();
    }

    @OnDestroy
    void onDestroy() {
        super.destroy();
        if (languageGroupListener != null) {
            languageGroup.selectedToggleProperty().removeListener(languageGroupListener);
        }
    }
}
