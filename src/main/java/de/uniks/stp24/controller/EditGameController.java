package de.uniks.stp24.controller;

import de.uniks.stp24.dto.UpdateGameDto;
import de.uniks.stp24.model.GameSettings;
import de.uniks.stp24.util.Constants;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;

import javax.inject.Inject;
import java.util.Map;

@Controller(view = "EditGame.fxml")
@Title("%edit.game.title")
public class EditGameController extends BaseController {
    @FXML
    Label errorServerLabel;
    @FXML
    Label errorLabel;
    @FXML
    TextField gameNameField;
    @FXML
    PasswordField passwordField;
    @FXML
    ChoiceBox<Integer> mapSizeBox;
    @FXML
    Button updateButton;
    @FXML
    Button cancelButton;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);

    @Inject
    public EditGameController() {
    }

    @OnInit
    public void onInit() {
        discordActivityService.setActivity(bundle.getString("discord.editing.game"));
    }

    @OnRender
    public void fillMapSizes() {
        mapSizeBox.getItems().addAll(Constants.MAP_SIZES);
        mapSizeBox.setValue(game.settings().size());
    }

    @OnRender
    public void ensureField() {
        final BooleanBinding nameEmpty = gameNameField.textProperty().isEmpty();
        final BooleanBinding passwordEmpty = passwordField.textProperty().isEmpty();
        final BooleanBinding mapSizeBoxEmpty = Bindings.createBooleanBinding(() ->
                mapSizeBox.getValue() == null, mapSizeBox.valueProperty());
        final BooleanBinding waiting = this.notWaiting.not();
        final BooleanBinding gameNameLess32 = gameNameField.textProperty().length().lessThanOrEqualTo(32);

        // Choose error message.
        errorLabel.textProperty().bind(
                Bindings.when(nameEmpty).then(bundle.getString("error.missing.game.name"))
                        .otherwise(Bindings.when(passwordEmpty)
                                .then(bundle.getString("error.missing.password"))
                                .otherwise(Bindings.when(gameNameLess32.not())
                                        .then(bundle.getString("error.name.too.long"))
                                        .otherwise(Bindings.when(mapSizeBoxEmpty)
                                                .then(bundle.getString("error.missing.map.size"))
                                                .otherwise(""))
                                )
                        )
        );

        // Bind update button, if a field is empty.
        updateButton.disableProperty().bind(
                nameEmpty.or(passwordEmpty).or(mapSizeBoxEmpty).or(waiting).or(gameNameLess32.not())
        );
    }

    public void update() {
        notWaiting.set(false);

        GameSettings gameSettings = new GameSettings(mapSizeBox.getValue());
        UpdateGameDto editGame = new UpdateGameDto(
                gameNameField.getText(),
                Constants.MAX_MEMBER_AMOUNT,
                false,
                1,
                gameSettings,
                passwordField.getText());

        // Server request to update game
        subscriber.subscribe(gamesApiService.updateGame(game._id(), editGame),
                result -> app.show("/members", Map.of("game", result, "createMemberDto", createMemberDto, "empireTemplate", empireTemplate)),
                error -> {
                    errorServerLabel.setVisible(true);
                    String errorKey = switch (error.getMessage()) {
                        case "HTTP 400 " -> "error.bad.request";
                        case "HTTP 401 " -> "error.missing.or.invalid.bearer.token";
                        case "HTTP 403 " -> "error.change.other.game";
                        case "HTTP 404 " -> "error.not.found";
                        case "HTTP 409 " -> "error.game.already.running";
                        case "HTTP 429 " -> "error.rate.limit.reached";
                        default -> "error.unknown";
                    };
                    errorServerLabel.setText(bundle.getString(errorKey));
                    notWaiting.set(true);
                }
        );
    }

    public void cancel() {
        app.show("/members", Map.of("game", game, "createMemberDto", createMemberDto, "empireTemplate", empireTemplate));
    }
}

