package de.uniks.stp24.component.popups;

import de.uniks.stp24.App;
import de.uniks.stp24.rest.UsersApiService;
import de.uniks.stp24.service.TokenStorage;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "DeleteAccPopUp.fxml")
public class DeleteAccPopUpComponent extends VBox {
    @FXML
    Button cancelButton;
    @FXML
    Button deleteAccButton;

    @Inject
    App app;
    @Inject
    TokenStorage tokenStorage;
    @Inject
    Subscriber subscriber;
    @Inject
    UsersApiService usersApiService;
    @Inject
    @Resource
    ResourceBundle bundle;

    @Param("modalStage")
    Stage modal;

    @Inject
    public DeleteAccPopUpComponent() {
    }

    public void deleteAcc() {
        deleteAccButton.setDisable(true);

        subscriber.subscribe(usersApiService.deleteUser(tokenStorage.getUserId()),
                user -> {
                    modal.close();
                    app.show("/login", Map.of("accDeleted", true));
                },
                error -> deleteAccButton.setDisable(false)
        );
    }

    public void cancel() {
        modal.close();
    }

    @OnDestroy
    public void destroy() {
        subscriber.dispose();
    }

}
