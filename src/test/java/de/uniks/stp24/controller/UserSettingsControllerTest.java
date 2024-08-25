package de.uniks.stp24.controller;

import de.uniks.stp24.ControllerTest;
import de.uniks.stp24.component.popups.DeleteAccPopUpComponent;
import de.uniks.stp24.model.User;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.service.TokenStorage;
import io.reactivex.rxjava3.core.Observable;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.fulib.fx.constructs.Modals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@ExtendWith(MockitoExtension.class)
public class UserSettingsControllerTest extends ControllerTest {

    @InjectMocks
    UserSettingsController controller;
    @InjectMocks
    DeleteAccPopUpComponent deleteAccPopUpComponent;
    @Spy
    Provider<DeleteAccPopUpComponent> deleteGamePopUpComponentProvider = new Provider<>() {
        @Override
        public DeleteAccPopUpComponent get() {
            return deleteAccPopUpComponent;
        }
    };

    @Mock
    UsersApiService usersApiService;
    @Mock
    TokenStorage tokenStorage;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        app.show(controller);
    }

    @Test
    void updateUserSettings() {
        doReturn(null).when(app).show("/lobby");
        doReturn(null).when(app).show("/userSettings");
        doReturn(Observable.just(new User("", "", "1", "test", null)))
                .when(usersApiService).getUser(Mockito.anyString());
        doReturn("1").when(tokenStorage).getUserId();
        doReturn(Observable.just(new User("", "", "1", "Jan", null)))
                .when(usersApiService).updateUser(any(), any());

//      Start: Jan has left the Lobby screen
//      and is now in the User Setting screen.
//      He wants to edit user data.

        assertEquals("PRAESIDEO - User Settings", stage.getTitle());

//      Action: Jan enters "Jan" as the username
//      and a random password.
//      He clicks on "Update and Save" and then on "Back".

        clickOn("#usernameField").write("Jan\t");
        clickOn("#passwordField").write("password\t");
        clickOn("#updateAndSaveButton");

        waitForFxEvents();

        verify(app, times(1)).show("/userSettings");
        clickOn("#backButton");

        waitForFxEvents();

//      End: Jan has successfully edited his user data
//      and is back on the Lobby screen.

        verify(usersApiService, times(1)).updateUser(any(), any());
        verify(app, times(1)).show("/lobby");
    }

    @Test
    void usernameTaken() {
        doReturn(Observable.just(new User("", "", "1", "test", null)))
                .when(usersApiService).getUser(Mockito.anyString());
        doReturn("1").when(tokenStorage).getUserId();
        doReturn(Observable.error(new Throwable("HTTP 409 "))).when(usersApiService).updateUser(any(), any());

//      Start: Tim has left the lobby screen
//      and is now in the user setting screen.
//      He wants to edit user data.

        assertEquals("PRAESIDEO - User Settings", stage.getTitle());

//      Action: Tim enters "Tim" as the username
//      and a random password. He clicks "Update and Save".
        write("Tim\t");
        write("password\t");
        clickOn("#updateAndSaveButton");

        waitForFxEvents();

//      End: Tim cannot edit his user data,
//      because the username "Tim" is already taken.
//      The message "Username is already taken!" is displayed.

        assertEquals("PRAESIDEO - User Settings", stage.getTitle());

        Text errorText = lookup("#nameErrorText").query();

        assertEquals(errorText.getText(), "Username is already taken.");
        verifyThat(errorText, Node::isVisible);
    }

    @Test
    void openDelAccPopUp() {
//      Start: Tim has left the lobby screen
//      and is now in the User Setting screen.
//      He wants to delete his account.

        assertEquals("PRAESIDEO - User Settings", stage.getTitle());

//      Action: Tim clicks on "Delete Account".

        clickOn("#deleteAccountButton");

        waitForFxEvents();

//      End: Delete Account window is displayed.

        assertEquals(1, Modals.getModalStages().size());
        Stage modal = Modals.getModalStages().getFirst();
        assertTrue(modal.isShowing());
    }

    @Test
    void cancelDeleteAcc() {

//      Start: Tim has decided not to delete his user account
//      after clicking on "Delete Account." He sees the Delete Account window.

        assertEquals("PRAESIDEO - User Settings", stage.getTitle());
        clickOn("#deleteAccountButton");

        assertEquals(1, Modals.getModalStages().size());
        Stage modal = Modals.getModalStages().getFirst();
        assertTrue(modal.isShowing());

//      Action: Tim clicks on "Cancel."

        clickOn("#cancelButton");

//      End: Tim has not deleted his user account
//      and is on the User Settings screen.

        verify(usersApiService, times(0)).deleteUser(any());
        assertEquals("PRAESIDEO - User Settings", stage.getTitle());
    }

    @Test
    void deleteAcc() {
        doReturn(Observable.just(new User("", "", "1", "Tim", null)))
                .when(usersApiService).deleteUser(Mockito.anyString());
        doReturn("1").when(tokenStorage).getUserId();

//      Start: Tim has decided to delete his user account
//      and he clicked on "Delete Account". He sees the Delete Account window.

        assertEquals("PRAESIDEO - User Settings", stage.getTitle());
        clickOn("#deleteAccountButton");

        assertEquals(1, Modals.getModalStages().size());
        Stage modal = Modals.getModalStages().getFirst();
        assertTrue(modal.isShowing());

//      Action: Tim clicks on "Yes".

        clickOn("#deleteAccButton");

//      End: Tim has successfully deleted his user account
//      and is back on the login screen.
//      The message "your account is deleted" is displayed.

        verify(usersApiService, times(1)).deleteUser(any());

        assertEquals("PRAESIDEO - Login", stage.getTitle());

        Text accDeletedText = lookup("#accDeletedText").query();
        assertEquals("Your account has been deleted.", accDeletedText.getText());
        verifyThat(accDeletedText, Node::isVisible);
    }

}
