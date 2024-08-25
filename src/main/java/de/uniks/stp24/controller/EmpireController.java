package de.uniks.stp24.controller;

import de.uniks.stp24.component.HomeSystemComponent;
import de.uniks.stp24.component.traits.TraitsComponent;
import de.uniks.stp24.dto.UpdateMemberDto;
import de.uniks.stp24.model.EmpireTemplate;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.model.SystemType;
import de.uniks.stp24.model.Trait;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import javafx.util.Subscription;
import org.fulib.fx.annotation.controller.Controller;
import org.fulib.fx.annotation.controller.Title;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Math.min;

@Controller
@Title("%empire.title")
public class EmpireController extends BaseController {
    @FXML
    Tab tabIdentity;
    @FXML
    Tab tabBasics;
    @FXML
    Tab tabTraits;
    @FXML
    Tab tabHomeSystems;
    @FXML
    HBox firstSystemRow;
    @FXML
    HBox secondSystemRow;
    @FXML
    HBox thirdSystemRow;
    @FXML
    VBox outerVbox;
    @FXML
    VBox empireBox;
    @FXML
    ImageView imgViewFlagDecrease;
    @FXML
    ImageView imgViewFlagIncrease;
    @FXML
    ImageView imgViewPortraitDecrease;
    @FXML
    ImageView imgViewPortraitIncrease;
    @FXML
    Label txtNameError;
    @FXML
    Button btnSave;
    @FXML
    Button btnBack;
    @FXML
    TabPane tabPane;
    @FXML
    TextField txtInputName;
    @FXML
    TextArea txtInputDescription;
    @FXML
    ColorPicker colorPicker;
    @FXML
    Button btnFlagDecrease;
    @FXML
    ImageView imgViewFlag;
    @FXML
    Button btnFlagIncrease;
    @FXML
    Label txtFlagID;
    @FXML
    Button btnPortraitDecrease;
    @FXML
    ImageView imgViewPortrait;
    @FXML
    Button btnPortraitIncrease;
    @FXML
    Label txtPortraitID;

    public final int MAX_FLAG_ID = 16;
    public final int MAX_PORTRAIT_ID = 18;
    public final int imageViewInitialPlacement = 86;

    private int flagId = 0;
    private int portraitId = 0;

    private final SimpleBooleanProperty notWaiting = new SimpleBooleanProperty(true);
    private final EventHandler<WindowEvent> windowCloseEventHandler = (WindowEvent WindowEvent) -> deleteGame();
    private final ToggleGroup homeSystemsGroup = new ToggleGroup();
    private Subscription subscriptions;
    TraitsComponent traitsComponent;

    private ChangeListener<Tab> tabPaneListener;

    private final Consumer<Bounds> imageViewBoundsConsumer = bounds -> {
        if (bounds.getWidth() == 100) {
            //no need to adjust
            return;
        }

        //center is imageViewInitialPlacement + width / 2
        //which is 48 + 100 / 2 = 48 + 50 = 98
        //so -> initialPlacement = center - width / 2 with center being 98 and width being the bounds width of the scaled image

        ImageView boundsOwner = imgViewFlag;
        if (bounds.equals(imgViewPortrait.getLayoutBounds())) {
            boundsOwner = imgViewPortrait;
        }

        final double centerX = imageViewInitialPlacement + boundsOwner.getFitWidth() / 2;
        final double newPlacement = centerX - bounds.getWidth() / 2;
        boundsOwner.setLayoutX(newPlacement);
    };


    @Inject
    public EmpireController() {
    }

    //no access to javafx attributes, load data that aren't linked to the gui
    @OnInit
    void init() {
        discordActivityService.setActivity(bundle.getString("discord.editing.empire"), bundle.getString("discord.in.game.lowercase") + " " + game.name());

        flagId = min(MAX_FLAG_ID - 1, empireTemplate.flag());
        portraitId = min(MAX_PORTRAIT_ID - 1, empireTemplate.portrait());

        subscriber.subscribe(eventListener.listen("games." + game._id() + ".*", Game.class), event -> {
            switch (event.suffix()) {
                case "updated" -> game = event.data();
                case "deleted" -> app.show("/lobby");
            }
        });

        this.app.stage().getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseEventHandler);
    }

    //access to attributes, modify data linked to gui
    @OnRender
    void initializeImagePickers() {
        //4 possibilities
        //1. ignore aspect ratio
        //2. set width and let height scale to remain aspect ratio
        //3. set height and let width scale to remain aspect ratio
        //4. set width and height and manually adjust image position


        //we use approach 4

        imgViewFlag.setImage(imageCache.get("image/flags/" + flagId + ".png"));
        imgViewPortrait.setImage(imageCache.get("image/portraits/" + portraitId + ".png"));

        subscriptions = Subscription.combine(
                imgViewPortrait.layoutBoundsProperty().subscribe(imageViewBoundsConsumer),
                imgViewFlag.layoutBoundsProperty().subscribe(imageViewBoundsConsumer)
        );

        txtFlagID.setText((flagId + 1) + "/" + (MAX_FLAG_ID));
        txtPortraitID.setText((portraitId + 1) + "/" + (MAX_PORTRAIT_ID));

        imgViewFlagDecrease.setImage(imageCache.get("image/arrow_left.png"));
        imgViewPortraitDecrease.setImage(imageCache.get("image/arrow_left.png"));

        imgViewFlagIncrease.setImage(imageCache.get("image/arrow_right.png"));
        imgViewPortraitIncrease.setImage(imageCache.get("image/arrow_right.png"));
    }

    @OnRender
    void render() {
        final BooleanBinding nameNotEmpty = txtInputName.textProperty().isNotEmpty();
        final BooleanBinding nameNotBlank = Bindings.createBooleanBinding(() -> !txtInputName.getText().isBlank(), txtInputName.textProperty());
        final BooleanBinding waiting = this.notWaiting.not();

        traitsComponent = app.initAndRender(traitsComponentProvider.get(), Map.of("empireTemplate", empireTemplate), subscriber);
        tabTraits.setContent(traitsComponent);

        txtNameError.textProperty().bind(
                Bindings.when(nameNotEmpty.not())
                        .then(bundle.getString("empire.missing.name"))
                        .otherwise("")
        );

        btnSave.disableProperty().bind(
                nameNotEmpty.not().or(nameNotBlank.not()).or(waiting)
        );

        //initialize values from passed empireTemplate
        colorPicker.setValue(Color.web(empireTemplate.color()));
        txtInputName.setText(empireTemplate.name());
        txtInputDescription.setPromptText(bundle.getString("empire.default.description"));
        if (!empireTemplate.description().isEmpty()) {
            txtInputDescription.setText(empireTemplate.description());
        }

        tabPane.getSelectionModel().selectedItemProperty().addListener(tabPaneListener = (observable, oldTab, newTab) -> {
            if (newTab == tabTraits || newTab == tabHomeSystems) {
                empireBox.setPrefWidth(1000);
                tabPane.setPrefWidth(767);
            } else {
                empireBox.setPrefWidth(800);
                tabPane.setPrefWidth(547);
            }
        });
    }

    @OnRender
    void initHomeSystemTab() {
        subscriber.subscribe(presetsService.getCachedPreset("getSystemTypes"), systems -> {
            List<SystemType> systemTypes = new ArrayList<>(((Map<String, SystemType>) systems).values());

            // Uninhabitable systems should not be selectable
            systemTypes = systemTypes.stream().filter(s -> !s.id().contains("uninhabitable")).toList();

            for (int i = 0; i < systemTypes.size(); i++) {
                SystemType type = systemTypes.get(i);

                HBox row = switch (i) {
                    case 0, 1 -> firstSystemRow;
                    case 2, 3 -> secondSystemRow;
                    case 4, 5, 6 -> thirdSystemRow;
                    default -> throw new IllegalStateException("Unexpected value: " + i);
                };

                HomeSystemComponent component = app.initAndRender(
                        homeSystemComponentProvider.get(), Map.of("type", type), subscriber
                );
                component.setToggleGroup(homeSystemsGroup);
                component.setSelected(empireTemplate.homeSystem().equals(type.id()));
                row.getChildren().add(component);
            }
        });
    }

    public void onSave() {
        notWaiting.set(false);
        List<Trait> ownedTraits = traitsComponent.getOwnedTraits();
        List<String> ownedTraitIds = ownedTraits.stream().map(Trait::id).toList();

        Toggle selectedToggle = homeSystemsGroup.getSelectedToggle();
        String homeSystem = selectedToggle != null ? ((HomeSystemComponent) selectedToggle).type.id() : "regular";

        //create new empireTemplate with new values and send it to the server
        EmpireTemplate newEmpireTemplate = new EmpireTemplate(
                txtInputName.getText(),
                txtInputDescription.getText(),
                toHexString(colorPicker.getValue()),
                flagId,
                portraitId,
                ownedTraitIds,
                empireTemplate.effects(),
                empireTemplate._private(),
                empireTemplate._public(),
                homeSystem
        );

        UpdateMemberDto updateMemberDto = new UpdateMemberDto(
                createMemberDto.ready(),
                newEmpireTemplate
        );

        subscriber.subscribe(
                membersService.updateMember(game._id(), tokenStorage.getUserId(), updateMemberDto),
                e -> {
                    notificationService.displayNotification(bundle.getString("success.empire.save") + " (" + e.empire().name() + ")", true);
                    empireTemplate = newEmpireTemplate;
                    notWaiting.set(true);
                    onBack();
                },
                error -> notWaiting.set(true)
        );

    }

    private String formatValToHex(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    private String toHexString(Color value) {
        return "#" + (formatValToHex(value.getRed()) + formatValToHex(value.getGreen()) + formatValToHex(value.getBlue())).toUpperCase();
    }

    public void onBack() {
        app.show("/members", Map.of("game", game, "createMemberDto", createMemberDto, "empireTemplate", empireTemplate));
    }

    public void onFlagDecrease() {
        flagId = Math.floorMod(flagId - 1, MAX_FLAG_ID);
        updateImageById("flags", imgViewFlag, txtFlagID, flagId, MAX_FLAG_ID);
    }

    public void onFlagIncrease() {
        flagId = (flagId + 1) % (MAX_FLAG_ID);
        updateImageById("flags", imgViewFlag, txtFlagID, flagId, MAX_FLAG_ID);
    }

    public void onPortraitDecrease() {
        portraitId = Math.floorMod(portraitId - 1, MAX_PORTRAIT_ID);
        updateImageById("portraits", imgViewPortrait, txtPortraitID, portraitId, MAX_PORTRAIT_ID);
    }

    public void onPortraitIncrease() {
        portraitId = (portraitId + 1) % (MAX_PORTRAIT_ID);
        updateImageById("portraits", imgViewPortrait, txtPortraitID, portraitId, MAX_PORTRAIT_ID);
    }

    private void updateImageById(String folderName, ImageView iv, Label txtLabel, int id, int maxId) {
        iv.setImage(imageCache.get("image/" + folderName + "/" + id + ".png"));
        txtLabel.setText((id + 1) + "/" + (maxId));
    }

    private void deleteGame() {
        if (!this.game.owner().equals(tokenStorage.getUserId())) {
            return;
        }

        subscriber.subscribe(gameService.deleteGame(game._id()));
    }

    @OnDestroy
    public void OnDestroy() {
        super.destroy();

        this.app.stage().getScene().getWindow().removeEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseEventHandler);
        if (tabPaneListener != null) {
            tabPane.getSelectionModel().selectedItemProperty().removeListener(tabPaneListener);
        }

        subscriptions.unsubscribe();
    }
}
