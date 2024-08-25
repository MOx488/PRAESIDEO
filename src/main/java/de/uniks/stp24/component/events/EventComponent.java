package de.uniks.stp24.component.events;

import de.uniks.stp24.App;
import de.uniks.stp24.dto.EventEffectDto;
import de.uniks.stp24.model.ChoiceType;
import de.uniks.stp24.model.Effect;
import de.uniks.stp24.model.Empire;
import de.uniks.stp24.model.Game;
import de.uniks.stp24.service.EnhancementService;
import de.uniks.stp24.service.EventService;
import de.uniks.stp24.service.ImageCache;
import de.uniks.stp24.service.PrefService;
import de.uniks.stp24.ws.EventListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fulib.fx.annotation.controller.Component;
import org.fulib.fx.annotation.controller.Resource;
import org.fulib.fx.annotation.event.OnDestroy;
import org.fulib.fx.annotation.event.OnInit;
import org.fulib.fx.annotation.event.OnRender;
import org.fulib.fx.annotation.param.Param;
import org.fulib.fx.controller.Subscriber;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static de.uniks.stp24.util.Constants.IMAGE_EVENT_REQUESTED_HEIGHT;
import static de.uniks.stp24.util.Constants.IMAGE_EVENT_REQUESTED_WIDTH;

@Component(view = "Event.fxml")
public class EventComponent extends VBox {

    @Inject
    public App app;


    @Inject
    public Subscriber subscriber;

    @Inject
    public ImageCache imageCache;

    @Inject
    public EventListener eventListener;
    @Inject
    @Resource
    public ResourceBundle bundle;

    @Inject
    public EventService eventService;

    @Inject
    public PrefService prefService;

    @Inject
    public Provider<EventEffectComponent> eventEffectComponentProvider;

    @Inject
    public EnhancementService enhancementService;

    @FXML
    ScrollPane eventInformationScrollPane;
    @FXML
    Label eventDurationLabel;
    @FXML
    Label eventRemainingDurationLabel;
    @FXML
    HBox eventInformationHbox;
    @FXML
    ImageView eventImage;
    @FXML
    Label eventDescriptionText;
    @FXML
    HBox eventActionButtons;

    @Param("game")
    Game game;

    @Param("empire")
    Empire empire;

    @Param("eventId")
    String eventId;

    @Param("closedComponentRunnable")
    Consumer<ChoiceType> closedComponentConsumer;

    @Param("previousChoice")
    ChoiceType previousChoice;

    @Param("isResignation")
    boolean isResignation;

    @Param("isSpecialEvent")
    boolean isSpecialEvent;

    @Param("effectIndex")
    int effectIndex;

    @Param("setEffectIndexConsumer")
    Consumer<Integer> setEffectIndexConsumer;

    @Param("overlapContainer")
    AnchorPane overlapContainer;

    final Random random;

    private int startPeriod = -1;
    private int duration;

    @Inject
    public EventComponent() {
        this.random = new Random();
    }

    @OnInit
    public void init() {

        this.startPeriod = eventService.getActiveEventStartPeriod();
        this.duration = eventService.getDuration();

        //update remaining duration label each tick
        subscriber.subscribe(eventListener.listen("games." + game._id() + ".updated", Game.class), event -> {
            final Game newGame = event.data();
            if (startPeriod <= 0 || newGame.period() == this.game.period()) {
                return;
            }

            this.game = newGame;
            final int remainingDays = startPeriod + duration - game.period();
            this.setRemainingDuration(remainingDays);

            if (remainingDays > 0) {
                return;
            }

            //dispose component
            this.destroy();
        });

    }

    @OnRender
    public void onRender() {
        overlapContainer.setPickOnBounds(true);

        String eventTextKey = "event." + eventId + ".description";
        if (previousChoice != ChoiceType.NO_CHOICE) {
            eventTextKey = "event." + eventId + "." + previousChoice.name().toLowerCase();

            if (effectIndex != -1) {
                eventTextKey += "_" + effectIndex;
            }
        }

        String imagePath = "image/events/" + eventId + ".jpg";
        if (isResignation) {
            String currentScientistType = eventId.split("\\.")[1];
            int currentScientistIndex = enhancementService.getScientistIndex(currentScientistType);
            String currentScientistName = bundle.getString(currentScientistType + "_" + currentScientistIndex);

            this.enhancementService.resignScientist(game, empire, currentScientistType);

            int newScientistIndex = enhancementService.getScientistIndex(currentScientistType);
            String newScientistName = bundle.getString(currentScientistType + "_" + newScientistIndex);
            String scientistGenericResignationText = bundle.getString("event.resignation.description");
            String scientistResignationText = currentScientistName + " " + scientistGenericResignationText.replace("______", newScientistName);

            this.eventDescriptionText.setText(scientistResignationText);

            imagePath = "image/scientists/" + currentScientistType + "_" + currentScientistIndex + ".jpg";
        } else {
            this.eventDescriptionText.setText(bundle.getString(eventTextKey));
        }

        this.eventImage.setImage(imageCache.get(imagePath, false, IMAGE_EVENT_REQUESTED_WIDTH, IMAGE_EVENT_REQUESTED_HEIGHT));

        this.initializeDurationLabels(this.previousChoice != ChoiceType.NO_CHOICE);
        this.initializeEffectComponents(this.previousChoice != ChoiceType.NO_CHOICE);
        this.initializeButtons();
        this.placeComponent();
    }

    private void initializeDurationLabels(boolean isPostChoice) {
        if (this.isResignation || (this.isSpecialEvent && (!isPostChoice || this.previousChoice == ChoiceType.REJECTION))) {
            this.eventDurationLabel.setVisible(false);
            this.eventRemainingDurationLabel.setVisible(false);
            return;
        }

        this.startPeriod = eventService.getActiveEventStartPeriod();
        this.duration = eventService.getDuration();

        if (this.startPeriod > 0) {
            this.setRemainingDuration(startPeriod + duration - game.period());
        } else {
            this.setRemainingDuration(duration);
        }

        this.eventDurationLabel.setText(bundle.getString("duration") + ":");

        this.eventDurationLabel.setVisible(true);
        this.eventRemainingDurationLabel.setVisible(true);
    }

    private void setRemainingDuration(int remainingDays) {
        this.eventRemainingDurationLabel.setText(remainingDays + " " + bundle.getString("days"));
    }

    private void initializeButtons() {
        if (isSpecialEvent && previousChoice == ChoiceType.NO_CHOICE) {
            final Button noButton = new Button(bundle.getString("no"));
            noButton.setOnAction(this::specialEventChoiceDeclined);
            this.eventActionButtons.getChildren().add(noButton);

            final Button yesButton = new Button(bundle.getString("yes"));
            yesButton.setOnAction(this::specialEventChoiceAccepted);
            this.eventActionButtons.getChildren().add(yesButton);
            return;
        }

        this.createOkayButton();
    }

    private void initializeEffectComponents(boolean isPostChoice) {
        if (this.isResignation || (this.isSpecialEvent && !isPostChoice)) {
            return;
        }

        //get effects from event id
        //create effect component for each effect
        final EventEffectDto eventEffectDto = eventService.getEventEffectDto(eventId, effectIndex);
        if (eventEffectDto == null) {
            return;
        }

        final int duration = eventEffectDto.duration();
        for (Effect eventEffect : eventEffectDto.effects()) {
            final EventEffectComponent eventEffectComponent = app.initAndRender(eventEffectComponentProvider.get(), Map.of("game", game, "empire", empire, "effect", eventEffect, "duration", duration, "startPeriod", startPeriod), subscriber);
            this.eventInformationHbox.getChildren().add(eventEffectComponent);
        }
    }

    private void createOkayButton() {
        final Button okayButton = new Button("OKAY");
        okayButton.setOnAction(this::closeEventComponent);
        this.eventActionButtons.getChildren().add(okayButton);
    }

    private void placeComponent() {
        AnchorPane.setTopAnchor(this, 160d);
        AnchorPane.setLeftAnchor(this, 300d);
        AnchorPane.setRightAnchor(this, 300d);
        AnchorPane.setBottomAnchor(this, 5d);

        this.eventActionButtons.setPrefWidth(Double.MAX_VALUE);
        this.eventDescriptionText.setPrefWidth(Double.MAX_VALUE);
        this.eventInformationHbox.setPrefWidth(Double.MAX_VALUE);
        this.eventInformationScrollPane.setPrefWidth(Double.MAX_VALUE);
    }

    private void specialEventChoiceAccepted(ActionEvent actionEvent) {
        //accepted
        this.postChoice(ChoiceType.ACCEPTANCE);
    }

    private void specialEventChoiceDeclined(ActionEvent actionEvent) {
        //declined
        this.postChoice(ChoiceType.REJECTION);
    }

    private void postChoice(ChoiceType choiceType) {
        this.previousChoice = choiceType;
        this.effectIndex = -1;

        String textKey = "event." + eventId + "." + this.previousChoice.name().toLowerCase();
        if (this.previousChoice == ChoiceType.ACCEPTANCE) {
            this.effectIndex = random.nextInt(2) + 1;
            textKey += "_" + this.effectIndex; //randomly choose between 1 and 2 i.e -> event.marriage.acceptance_1, event.marriage.acceptance_2
        }

        this.eventDescriptionText.setText(bundle.getString(textKey));
        this.eventActionButtons.getChildren().clear();

        this.createOkayButton();
        this.initializeEffectComponents(true);
        this.initializeDurationLabels(true);
    }

    private void closeEventComponent(ActionEvent actionEvent) {
        overlapContainer.setPickOnBounds(false);
        this.destroy();

        if (!isResignation && this.previousChoice != ChoiceType.REJECTION) {
            this.setEffectIndexConsumer.accept(this.effectIndex);
            this.eventService.startEvent(eventId, effectIndex);
            return;
        }

        this.eventService.removeEvent();
    }

    @OnDestroy
    public void destroy() {
        this.subscriber.dispose();
        this.closedComponentConsumer.accept(previousChoice);
        this.eventService.setPreviousChoice(previousChoice.ordinal());
    }
}
