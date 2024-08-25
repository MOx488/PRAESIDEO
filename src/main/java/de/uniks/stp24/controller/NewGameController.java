package de.uniks.stp24.controller;

import de.uniks.stp24.dto.CreateGameDto;
import de.uniks.stp24.dto.CreateMemberDto;
import de.uniks.stp24.model.EmpireTemplate;
import de.uniks.stp24.model.GameSettings;
import de.uniks.stp24.util.Constants;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Controller
@Title("%new.game.title")
public class NewGameController extends BaseController {
    @FXML
    Text errorText;
    @FXML
    Text nameErrorText;
    @FXML
    ChoiceBox<Integer> mapSizeBox;
    @FXML
    TextField nameField;
    @FXML
    PasswordField passwordField;
    @FXML
    Button cancelButton;
    @FXML
    Button createButton;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);

    @OnInit
    void setDiscordActivity() {
        discordActivityService.setActivity(bundle.getString("discord.creating.game"));
    }

    @OnRender
    public void fillMapSizes() {
        mapSizeBox.getItems().addAll(Constants.MAP_SIZES);
        mapSizeBox.setValue(50);
    }

    @OnRender
    void ensurePasswordAndName() {
        StringBinding trimmedText = Bindings.createStringBinding(() ->
                        nameField.getText().replaceAll("\\s", ""),
                nameField.textProperty()
        );

        final BooleanBinding nameNotEmpty = trimmedText.isNotEmpty();
        final BooleanBinding passwordLengthValid = passwordField.textProperty().length().greaterThanOrEqualTo(1);
        final BooleanBinding gameNameLess32 = nameField.textProperty().length().lessThanOrEqualTo(32);

        errorText.textProperty().bind(
                Bindings.when(nameNotEmpty.not())
                        .then(bundle.getString("error.missing.game.name"))
                        .otherwise(Bindings.when(gameNameLess32.not())
                                .then(bundle.getString("error.name.too.long"))
                                .otherwise(Bindings.when(passwordLengthValid.not())
                                        .then(bundle.getString("error.missing.password"))
                                        .otherwise("")
                                )
                        )
        );

        errorText.setVisible(errorText.textProperty().isNotEmpty().get());

        createButton.disableProperty().bind(
                (nameNotEmpty.not()).or(passwordLengthValid.not()).or(this.notWaiting.not()).or(gameNameLess32.not())
        );
    }

    @Inject
    public NewGameController() {
    }

    public void cancel() {
        app.show("/lobby");
    }

    public void createGame() {
        notWaiting.set(true);

        GameSettings settings = new GameSettings(mapSizeBox.getValue());
        String password = passwordField.getText();
        CreateGameDto gameDto = new CreateGameDto(
                nameField.getText(),
                Constants.MAX_MEMBER_AMOUNT,
                settings,
                password
        );

        subscriber.subscribe(gamesApiService.createGame(gameDto),
                game -> {
                    EmpireTemplate empireTemplate = new EmpireTemplate(
                            bundle.getString("empire.default.name"),
                            "",
                            "#FFFFFF",
                            0,
                            0,
                            List.of(),
                            null,
                            null,
                            null,
                            "regular"
                    );
                    CreateMemberDto createMemberDto = new CreateMemberDto(false, empireTemplate, password);
                    app.show("/members", Map.of("game", game, "createMemberDto", createMemberDto, "empireTemplate", empireTemplate));
                },
                error -> {
                    setErrorText(error.getMessage());
                    notWaiting.set(false);
                }
        );
    }

    public void setErrorText(String errorMessage) {
        nameErrorText.setVisible(true);
        String errorKey = switch (errorMessage) {
            case "HTTP 400 " -> "error.bad.request";
            case "HTTP 401 " -> "error.missing.or.invalid.bearer.token";
            case "HTTP 429 " -> "error.rate.limit.reached";
            default -> "error.unknown";
        };
        nameErrorText.setText(bundle.getString(errorKey));
    }
}
