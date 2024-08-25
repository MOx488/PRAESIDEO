package de.uniks.stp24.controller;

import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.dto.LoginResult;
import de.uniks.stp24.service.AudioService;
import de.uniks.stp24.service.ErrorService;
import de.uniks.stp24.service.LoginService;
import de.uniks.stp24.service.NotificationService;
import io.reactivex.rxjava3.core.Observable;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.matcher.control.LabeledMatchers;

import javax.inject.Provider;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
public class LoginControllerTest extends ControllerTest {
    @Spy
    LoginService loginService;
    @Spy
    Provider<ResourceBundle> bundleProvider;
    @InjectMocks
    LoginController loginController;
    @Mock
    AudioService audioService;
    @Spy
    ErrorService errorService;
    @Mock
    NotificationService notificationService;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        errorService.setBundle(bundleProvider.get());
        notificationService.setBundle(bundleProvider.get());
        notificationService.errorService = errorService;

        loginController.notificationService = notificationService;

        app.show(loginController);
    }

    @Test
    void login() {
        waitForFxEvents();

        doReturn(Observable.just(new LoginResult(
                "00:00:00",
                "00:00:00",
                "1",
                "Jan",
                "jan_avatar.png",
                "a",
                "r"
        ))).when(loginService).login(any(), any(), anyBoolean());
        doReturn(null).when(app).show("/lobby");

        // Start:
        // Jan has launched Praesideo. He sees the login screen.
        // He already has an account named "Jan" with password "janistcool".
        assertEquals("PRAESIDEO - Login", stage.getTitle());

        // Action:
        // Jan enters his username "Jan" and his password "janistcool". He clicks "Login".
        write("Jan\t");
        waitForFxEvents();
        write("janistcool");
        waitForFxEvents();
        clickOn("Login");

        waitForFxEvents();

        // Result:
        // Jan is now logged in.
        // He sees the lobby screen.
        verify(loginService, times(1)).login("Jan", "janistcool", false);
        verify(app, times(1)).show("/lobby");
    }

    @Test
    void translate() {
        waitForFxEvents();

        // Start:
        // Jan has launched Praesideo. He sees the login screen.
        // His game is currently in English.
        assertEquals("PRAESIDEO - Login", stage.getTitle());
        verifyThat("#usernameLabel", LabeledMatchers.hasText("Username"));
        assertEquals("Enter Username", ((TextField) lookup("#usernameField").query()).getPromptText());
        verifyThat("#passwordLabel", LabeledMatchers.hasText("Password"));
        assertEquals("Enter Password", ((TextField) lookup("#passwordField").query()).getPromptText());
        verifyThat("#rememberCheckbox", LabeledMatchers.hasText("Remember Me"));
        verifyThat("#signupButton", LabeledMatchers.hasText("Signup"));
        verifyThat("#loginButton", LabeledMatchers.hasText("Login"));
        verifyThat("#languageLabel", LabeledMatchers.hasText("Select Language:"));

        // Action:
        // Jan clicks on the "DE" button because he wants to play in German.
        doReturn(ResourceBundle.getBundle("de/uniks/stp24/lang/lang", Locale.GERMAN))
                .when(bundleProvider).get();
        clickOn("#setDeButton");

        waitForFxEvents();

        // Result:
        // Jan's screen has been translated to German.
        verifyThat("#usernameLabel", LabeledMatchers.hasText("Benutzername"));
        assertEquals("Benutzername eingeben",
                ((TextField) lookup("#usernameField").query()).getPromptText());
        verifyThat("#passwordLabel", LabeledMatchers.hasText("Passwort"));
        assertEquals("Passwort eingeben",
                ((TextField) lookup("#passwordField").query()).getPromptText());
        verifyThat("#rememberCheckbox", LabeledMatchers.hasText("Benutzer merken"));
        verifyThat("#signupButton", LabeledMatchers.hasText("Registrieren"));
        verifyThat("#loginButton", LabeledMatchers.hasText("Einloggen"));
        verifyThat("#languageLabel", LabeledMatchers.hasText("Sprache auswählen:"));

        // Action:
        // Jan clicks on the "EN" button because he changed his mind, he wants to play in English.
        doReturn(ResourceBundle.getBundle("de/uniks/stp24/lang/lang", Locale.ENGLISH))
                .when(bundleProvider).get();
        clickOn("#setEnButton");

        waitForFxEvents();

        // Result:
        // Jan's screen has been translated to English.
        verifyThat("#usernameLabel", LabeledMatchers.hasText("Username"));
        assertEquals("Enter Username", ((TextField) lookup("#usernameField").query()).getPromptText());
        verifyThat("#passwordLabel", LabeledMatchers.hasText("Password"));
        assertEquals("Enter Password", ((TextField) lookup("#passwordField").query()).getPromptText());
        verifyThat("#rememberCheckbox", LabeledMatchers.hasText("Remember Me"));
        verifyThat("#signupButton", LabeledMatchers.hasText("Signup"));
        verifyThat("#loginButton", LabeledMatchers.hasText("Login"));
        verifyThat("#languageLabel", LabeledMatchers.hasText("Select Language:"));
    }

    @Test
    void signup() {
        waitForFxEvents();

        // Start:
        // Adnan has launched Praesideo. He sees the login screen.
        assertEquals("PRAESIDEO - Login", stage.getTitle());

        // Action:
        // Adnan clicks "Signup".
        clickOn("Signup");

        waitForFxEvents();

        if (!stage.getTitle().equals("PRAESIDEO - Signup")) {
            Platform.runLater(() -> {
                Button button = lookup("#signupButton").query();
                button.fire();
            });
        }

        waitForFxEvents();

        // Result:
        // Adnan sees the signup screen. His data has been filled into the corresponding text fields.
        assertEquals("PRAESIDEO - Signup", stage.getTitle());
    }

    @Test
    void licensesAndCredits() {
        waitForFxEvents();

        // Start:
        // Daniel has launched Praesideo. He sees the login screen.
        // He wants to look at the used licenses and read through the credits.
        assertEquals("PRAESIDEO - Login", stage.getTitle());

        // Action:
        // Daniel clicks on the gear image in the top left corner of the screen.
        clickOn("#infoImage");

        waitForFxEvents();

        // Result:
        // Daniel sees the screen with the licenses and credits.
        assertEquals("PRAESIDEO - Licenses and Credits", stage.getTitle());
    }
}
