package de.uniks.stp24.component.popups;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.CreateMemberDto;
import de.uniks.stp24.model.EmpireTemplate;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.rest.GameMembersApiService;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;
import retrofit2.Retrofit;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component(view = "JoinGamePopup.fxml")
public class JoinGamePopupComponent extends VBox {
    @Inject
    App app;
    @Inject
    Subscriber subscriber;
    @Inject
    GameMembersApiService gameMembersApiService;
    @Inject
    Retrofit retrofit;
    @Inject
    @Resource
    ResourceBundle bundle;

    @FXML
    ImageView cancelButton;
    @FXML
    Button confirmButton;
    @FXML
    Text textBox;
    @FXML
    TextField passwordField;
    @FXML
    Text errorText;

    @Param("game")
    public Game game;
    @Param("modalStage")
    Stage modal;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);

    @OnRender
    void Render() {
        final BooleanBinding passwordEmpty = passwordField.textProperty().isEmpty();
        final BooleanBinding waiting = this.notWaiting.not();

        textBox.setText(bundle.getString("enter.game.password") + " " + game.name());
        errorText.setText("");

        confirmButton.disableProperty().bind(passwordEmpty.or(waiting));

        errorText.textProperty().bind(
                Bindings.when(passwordEmpty)
                        .then(bundle.getString("error.missing.password"))
                        .otherwise("")
        );

        errorText.setVisible(errorText.textProperty().isNotEmpty().get());
    }


    public void cancel() {
        this.modal.close();
        this.modal.setScene(null);
    }

    public void join() {
        notWaiting.set(false);

        EmpireTemplate empireTemplate = new EmpireTemplate(
                bundle.getString("empire.default.name"),
                "",
                "FFFFFF",
                0,
                0,
                List.of(),
                List.of(),
                null,
                null,
                "regular"
        );

        CreateMemberDto createMemberDto = new CreateMemberDto(
                false,
                empireTemplate,
                passwordField.getText()
        );
        subscriber.subscribe(
                gameMembersApiService.joinGame(
                        game._id(),
                        createMemberDto
                ),
                result -> {
                    app.show("/members", Map.of("game", game, "createMemberDto", createMemberDto, "empireTemplate", empireTemplate));
                    this.modal.close();
                    this.modal.setScene(null);
                },
                error -> notWaiting.set(true)
        );
    }

    @OnDestroy
    void destroy() {
        subscriber.dispose();
    }

    @Inject
    public JoinGamePopupComponent() {
    }
}
