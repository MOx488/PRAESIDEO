package de.uniks.stp24;

import de.uniks.stp24.component.popups.DeleteAccPopUpComponent;
import de.uniks.stp24.component.popups.JoinGamePopupComponent;
import de.uniks.stp24.controller.*;
import org.fulib.fx.annotation.Route;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class Routes {
    @Route("gameLaunch")
    @Inject
    Provider<GameLaunchController> gameLaunch;

    @Route("userSettings")
    @Inject
    Provider<UserSettingsController> userSettings;

    @Route("deleteAccPopUp")
    @Inject
    Provider<DeleteAccPopUpComponent> deleteAccPopUp;

    @Route("login")
    @Inject
    Provider<LoginController> login;

    @Route("editGame")
    @Inject
    Provider<EditGameController> editGame;

    @Route("signup")
    @Inject
    Provider<SignupController> signup;

    @Route("licenses-and-credits")
    @Inject
    Provider<LicensesAndCreditsController> licensesAndCredits;

    @Route("lobby")
    @Inject
    Provider<LobbyController> lobby;

    @Route("members")
    @Inject
    Provider<MembersController> members;

    @Route("joinGamePopup")
    @Inject
    Provider<JoinGamePopupComponent> joinGamePopup;

    @Route("newGame")
    @Inject
    Provider<NewGameController> newGame;

    @Route("edit-empire")
    @Inject
    Provider<EmpireController> empire;

    @Route("ingame")
    @Inject
    Provider<IngameController> ingame;

    @Inject
    public Routes() {
    }
}
