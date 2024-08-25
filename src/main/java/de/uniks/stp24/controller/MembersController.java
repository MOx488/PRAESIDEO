package de.uniks.stp24.controller;

import de.uniks.stp24.component.MemberComponent;
import de.uniks.stp24.dto.UpdateMemberDto;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.Member;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.constructs.Modals;
import org.fulib.fx.constructs.listview.ComponentListCell;

import javax.inject.Inject;
import java.util.Map;

@Controller
@Title("%members.title")
public class MembersController extends BaseController {
    @FXML
    VBox outerVBox;
    @FXML
    Label gameTitle;
    @FXML
    Button btnBuildEmpire;
    @FXML
    Button btnGameAction;
    @FXML
    Button btnLeaveGame;
    @FXML
    Button btnEditGame;
    @FXML
    Label errorLabel;
    @FXML
    ListView<Member> memberListView;

    private final ObservableList<Member> members = FXCollections.observableArrayList();
    private boolean isOwner = false;
    private final EventHandler<WindowEvent> windowCloseEventHandler = (WindowEvent WindowEvent) -> leaveGame(new ActionEvent());
    private final BooleanProperty everyoneReady = new SimpleBooleanProperty(false);
    private final BooleanProperty waiting = new SimpleBooleanProperty(false);

    private ListChangeListener<Member> membersListener;

    @Inject
    public MembersController() {

    }

    //no access to javafx attributes, load data that aren't linked to the gui
    @OnInit
    void init() {
        discordActivityService.setActivity(bundle.getString("discord.in.lobby"), bundle.getString("discord.for.game") + " " + game.name());

        MemberComponent.ownerId = game.owner();
        this.isOwner = game.owner().equals(tokenStorage.getUserId());

        subscriber.subscribe(gameMembersApiService.getMembersOfGame(game._id()), this.members::setAll);

        this.setupWebSockets();

        //set current user
        subscriber.subscribe(membersService.updateMember(this.game._id(), this.tokenStorage.getUserId(), new UpdateMemberDto(this.createMemberDto.ready(), this.empireTemplate)));

        this.app.stage().getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseEventHandler);
        this.listenOnMemberChangeAsOwner();
    }

    private void bindStartGameButton() {
        if (!isOwner) {
            return;
        }

        errorLabel.textProperty().bind(
                Bindings.when(everyoneReady.not())
                        .then(bundle.getString("error.not.everyone.ready"))
                        .otherwise("")
        );

        btnGameAction.disableProperty().bind((everyoneReady.not()).or(waiting));
    }

    private void setupWebSockets() {
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".*", Game.class), event -> {
            switch (event.suffix()) {
                case "updated" -> updateGame(event.data());
                case "deleted" -> leaveGame(null);
            }
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".*", Game.class), event -> { //.(updated|deleted)
            switch (event.suffix()) {
                case "updated" -> game = event.data();
                case "deleted" -> leaveGame(null);
            }
        });

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".members.*.*", Member.class), event -> {
            switch (event.suffix()) {
                case "created" -> members.add(event.data());
                case "updated" ->
                        members.replaceAll(member -> member.user().equals(event.data().user()) ? event.data() : member);
                case "deleted" -> {
                    final String userId = event.data().user();
                    if (userId.equals(tokenStorage.getUserId())) {
                        //we got kicked
                        leaveGame(null);
                        return;
                    }

                    members.removeIf(member -> member.user().equals(userId));
                }
            }
        });
    }

    private void listenOnMemberChangeAsOwner() {
        if (!isOwner) {
            return;
        }

        this.members.addListener(membersListener = change -> {
            final ObservableList<? extends Member> memberList = change.getList();
            this.everyoneReady.set(memberList.stream().filter(member -> !member.user().equals(tokenStorage.getUserId())).allMatch(Member::ready));
        });
    }

    private void updateGame(Game updatedGame) {
        if (!game.started() && updatedGame.started()) {
            app.show("/ingame", Map.of("game", updatedGame));
        }

        game = updatedGame;
    }

    //access to attributes, modify data linked to gui
    @OnRender
    public void OnRender() {
        if (!isOwner) {
            btnGameAction.setText(
                    createMemberDto.ready() ? bundle.getString("not.ready") : bundle.getString("ready")
            );
            btnLeaveGame.setText(bundle.getString("return.to.lobby"));
            outerVBox.getChildren().remove(btnEditGame);
        }

        gameTitle.setText(game.name());

        memberListView.setItems(members);
        memberListView.setCellFactory(list -> {
            ListCell<Member> cellList = new ComponentListCell<>(app, memberComponentProvider);
            cellList.prefWidthProperty().bind(memberListView.widthProperty().subtract(20));
            cellList.setMaxWidth(Control.USE_PREF_SIZE);
            return cellList;
        });

        this.bindStartGameButton();
    }


    private void enableGameAction() {
        btnGameAction.setDisable(false);
    }

    public void changeReadyState() {
        Member localPlayer = members.stream().filter(member -> member.user().equals(tokenStorage.getUserId())).findFirst().orElse(null);
        if (localPlayer == null) {
            return;
        }

        //send server patch request to change ready state
        btnGameAction.setDisable(true);
        subscriber.subscribe(
                membersService.updateMember(game._id(), tokenStorage.getUserId(), new UpdateMemberDto(!localPlayer.ready(), localPlayer.empire())),
                member -> {
                    btnGameAction.setDisable(false);

                    //invert button text
                    String ready = bundle.getString("ready");
                    btnGameAction.setText(btnGameAction.getText().equals(ready) ? bundle.getString("not.ready") : ready);
                },
                //onError
                error -> enableGameAction()
        );
    }

    public void startGame() {
        waiting.set(true);
        subscriber.subscribe(gameService.startGame(game, createMemberDto.password()), g -> app.show("/ingame", Map.of("game", game)), error -> waiting.set(false));
    }

    public void actionButton() {
        if (isOwner) {
            // send server request to start game -> switch to game view after successful response
            startGame();
        } else {
            changeReadyState();
        }
    }

    public void leaveGame(ActionEvent event) {
        //game was deleted -> no need to call any endpoints, switch view
        if (event == null) {
            this.showLobbyScreen();
            return;
        }

        //owner wants to delete game
        if (isOwner) {
            if (event.getSource() == btnLeaveGame) {
                new Modals(app).modal(deleteGamePopUpComponent.get())
                        .dialog(true)
                        .params(Map.of("gameID", game._id()))
                        .show();
            } else {
                //host wants to delete the game by closing the window
                subscriber.subscribe(gameService.deleteGame(game._id()), success -> showLobbyScreen());
            }
            return;
        }

        btnLeaveGame.setDisable(true);
        //normal member wants to leave
        subscriber.subscribe(
                membersService.leaveGame(game._id(), tokenStorage.getUserId()),
                success -> showLobbyScreen(),
                error -> btnLeaveGame.setDisable(false));
    }

    public void editGame() {
        app.show("/editGame", Map.of("game", game, "createMemberDto", createMemberDto, "empireTemplate", empireTemplate));
    }

    public void buildEmpire() {
        app.show("/edit-empire", Map.of("game", game, "createMemberDto", createMemberDto, "empireTemplate", empireTemplate));
    }

    public void showLobbyScreen() {
        app.show("/lobby");
    }

    @Override
    public void destroy() {
        super.destroy();

        if (membersListener != null) {
            this.members.removeListener(membersListener);
        }

        this.memberListView.getItems().clear();

        this.app.stage().getScene().getWindow().removeEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseEventHandler);

        MemberComponent.ownerId = "";
    }
}
