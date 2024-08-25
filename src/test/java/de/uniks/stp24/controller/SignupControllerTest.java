package de.uniks.stp24.controller;


import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.model.User;
import de.uniks.stp24.rest.UsersApiService;
import io.reactivex.rxjava3.core.Observable;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
class SignupControllerTest extends ControllerTest {
    @InjectMocks
    SignupController signupController;
    @Spy
    UsersApiService usersApiService;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        app.show(signupController);
    }

    @Test
    void signupWithBoxChecked() {
        doReturn(null).when(app)
                .show("/login", Map.of("username", "Saitama", "password", "blizzard"));
        doReturn(Observable.just(new User("1", "1", "1", "Saitama", "")))
                .when(usersApiService).createUser(any());

        // Start:
        // Saitama has launched PAESIDEO and wants to signup and sees the signup screen.
        assertEquals("PRAESIDEO - Signup", stage.getTitle());
        verifyThat("#errorMessageLabel", hasText("Missing username."));

        // Action:
        // Saitama enters his username "Saitama" and writes his password "blizzard" in the first field
        // then in the second and presses the signup button.
        write("Saitama\t");

        verifyThat("#errorMessageLabel", hasText("Password must be at least 8 characters long."));

        write("blizzard\t");

        verifyThat("#errorMessageLabel", hasText("Passwords do not match."));

        write("blizzard\t");

        verifyThat("#errorMessageLabel", hasText(""));

        clickOn("#signupButton");

        waitForFxEvents();

        // Result:
        // Saitama is registered.
        // He sees the login screen with his data already filled in
        verify(app, times(1))
                .show("/login", Map.of("username", "Saitama", "password", "blizzard"));
    }

    @Test
    void backLogin() {
        doReturn(null).when(app).show("/login");

        // Start:
        // Saitama has launched Praesideo. He sees the signup screen.
        // He already has an account
        // he needs to go to the login screen
        assertEquals("PRAESIDEO - Signup", stage.getTitle());

        // Action
        // Saitama clicks on "back"
        clickOn("#backButton");
        waitForFxEvents();

        // Result:
        // Saitama sees the login screen.
        verify(app, times(1)).show("/login");
    }

    @Test
    void errorUsernameTaken() {
        doReturn(Observable.error(new Throwable("HTTP 409 "))).when(usersApiService).createUser(any());

        // Start:
        // Saitama has launched Praesideo. He sees the signup screen.
        assertEquals("PRAESIDEO - Signup", stage.getTitle());
        // Action:
        // Saitama enters his username "string" and writes his password "blizzard" in the first field
        // then in the second and presses the signup button.
        write("string\t");
        write("blizzard\t");
        write("blizzard\t");
        clickOn("#signupButton");

        waitForFxEvents();
        // Result:
        // Saitama isn't registered because the username is taken and he sees a new error message.
        verifyThat("#nameError", hasText("Username is already taken."));
    }

    @Test
    void errorValidationFailed() {
        doReturn(Observable.error(new Throwable("HTTP 400 "))).when(usersApiService).createUser(any());

        // Start:
        // Saitama has launched Praesideo. He sees the signup screen.
        assertEquals("PRAESIDEO - Signup", stage.getTitle());

        // Action:
        // Saitama enters his username "Saitama" and writes his password "blizzard" in the first field
        // then in the second and presses the signup button.
        write("Saitama\t");
        write("blizzard\t");
        write("blizzard\t");
        clickOn("#signupButton");

        waitForFxEvents();
        // Result:
        // Saitama isn't registered because the validation to the server failed.
        // He sees the according error message.
        verifyThat("#nameError", hasText("Validation failed."));
    }

    @Test
    void errorRateLimit() {
        doReturn(Observable.error(new Throwable("HTTP 429 "))).when(usersApiService).createUser(any());

        // Start:
        // Saitama has launched Praesideo. He sees the signup screen.
        assertEquals("PRAESIDEO - Signup", stage.getTitle());

        // Action:
        // Saitama enters his username "Saitama" and writes his password "blizzard" in the first field
        // then in the second and presses the signup button.
        write("Saitama\t");
        write("blizzard\t");
        write("blizzard\t");
        clickOn("#signupButton");

        waitForFxEvents();
        // Result:
        // Saitama isn't registered because the rate limit was reached.
        // He sees the according error message.
        verifyThat("#nameError", hasText("Rate limit reached."));
    }
}
