package de.uniks.stp24.component.popups;

import de.uniks.stp24.App;
import de.uniks.stp24.service.GameService;
import de.uniks.stp24.service.TokenStorage;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import java.util.ResourceBundle;

@Component(view = "DeleteGamePopUp.fxml")
public class DeleteGamePopUpComponent extends AnchorPane {
    @FXML
    Button cancelButton;
    @FXML
    Button deleteGameButton;
    @Inject
    public GameService gameService;
    @Inject
    public Subscriber subscriber;
    @Inject
    TokenStorage tokenStorage;
    @Inject
    public App app;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Param("modalStage")
    Stage modal;
    @Param("gameID")
    String gameID;

    @Inject
    public DeleteGamePopUpComponent() {
    }

    public void deleteGame() {
        deleteGameButton.setDisable(true);

        // delete game.
        subscriber.subscribe(gameService.deleteGame(gameID),
                result -> {
                    modal.close();
                    app.show("/lobby");
                },
                error -> deleteGameButton.setDisable(false)
        );
    }

    @OnDestroy
    void destroy() {
        subscriber.dispose();
    }

    public void cancel() {
        modal.close();
    }
}
